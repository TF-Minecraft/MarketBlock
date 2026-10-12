package net.tfminecraft.marketblock.manager;

import java.util.Optional;
import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.tfminecraft.marketblock.Cache;
import net.tfminecraft.marketblock.MarketBlock;
import net.tfminecraft.denareconomy.api.Denars;
import net.tfminecraft.denareconomy.api.Result;
import net.tfminecraft.marketblock.events.MarketSaleEvent;
import net.tfminecraft.marketblock.inventory.holder.MBGUI;
import net.tfminecraft.marketblock.inventory.holder.MBHolder;
import net.tfminecraft.marketblock.loader.CategoryLoader;
import net.tfminecraft.marketblock.loader.TradeLoader;
import net.tfminecraft.marketblock.trade.Category;
import net.tfminecraft.marketblock.trade.MarketSale;
import net.tfminecraft.marketblock.trade.Trade;
import net.tfminecraft.marketblock.util.DemandSchedule;
import net.tfminecraft.marketblock.util.InventoryUtils;
import net.tfminecraft.marketblock.util.PriceCalculator;
import net.tfminecraft.marketblock.util.SaleTake;
import net.tfminecraft.tlibs.character.CharacterRef;

public class TradeManager implements Listener {
    InventoryManager inv = new InventoryManager();
    private BukkitTask demandTask;
    private final Map<InventoryClickEvent, Boolean> handledSales = new WeakHashMap<>();

    public void update() {
        for(Player p : Bukkit.getOnlinePlayers()) {
            if(p.getOpenInventory().getTopInventory() == null) continue;
            Inventory i = p.getOpenInventory().getTopInventory();
            if(!(i.getHolder() instanceof MBHolder)) continue;
            MBHolder h = (MBHolder) i.getHolder();
            if(h.getType().equals(MBGUI.TRADE)) {
                inv.tradeView(i, p, CategoryLoader.getByString(h.getId()));
            }
        }
    }

    private String getItemId(MBGUI type, ItemStack i) {
        ItemMeta meta = i.getItemMeta();
        if (meta == null) return null;
        NamespacedKey key = new NamespacedKey(MarketBlock.plugin, switch (type) {
            case CATEGORY -> "category_id";
            case TRADE -> "trade_id";
        });
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public void start() {
        scheduleDemand(0L);
    }

    public void rescheduleDemandCycle() {
        scheduleDemand(DemandSchedule.periodTicks(Cache.demandRecoveryHours));
    }

    private void scheduleDemand(long delayTicks) {
        if (demandTask != null) {
            demandTask.cancel();
            demandTask = null;
        }
        long period = DemandSchedule.periodTicks(Cache.demandRecoveryHours);
        demandTask = new BukkitRunnable() {
            @Override
            public void run() {
                recoverDemand();
            }
        }.runTaskTimer(MarketBlock.plugin, delayTicks, period);
    }

    public void recoverDemand() {
        for (Trade t : TradeLoader.getTrades().values()) {
            t.demand();
        }
        update();
    }
    
    @EventHandler
    public void openMarket(PlayerInteractEvent e) {
        if(!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if(!Cache.blockIsMarketBlock(e.getClickedBlock())) return;
        e.setCancelled(true);
        inv.categoryView(null, e.getPlayer());
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof MBHolder)) return;
        e.setCancelled(true);
        if(e.getClickedInventory() != e.getView().getTopInventory()) return;
        Player p = (Player) e.getWhoClicked();
        MBHolder h = (MBHolder) e.getView().getTopInventory().getHolder();
        ItemStack i = e.getCurrentItem();
        if(i == null) return;
        switch (h.getType()) {
            case CATEGORY:
                categoryClick(p, h, i, e);
                break;
            case TRADE:
                tradeClick(p, h, i, e);
                break;
            default:
                break;
        }
    }

    public void categoryClick(Player p, MBHolder h, ItemStack i, InventoryClickEvent e) {
        String id = getItemId(h.getType(), i);
        if(id == null) return;
        Category cat = CategoryLoader.getByString(id);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
        inv.tradeView(null, p, cat);
    }

    public void tradeClick(Player p, MBHolder h, ItemStack i, InventoryClickEvent e) {
        if (i.getType().equals(Material.BARRIER)) {
            inv.categoryView(null, p);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
            return;
        }

        if (e != null && handledSales.put(e, true) != null) return;
        String id = getItemId(h.getType(), i);
        if (id == null) return;

        Trade trade = TradeLoader.getTradeById(id);
        if (trade == null) {
            p.sendMessage("§cThe market no longer buys this.");
            return;
        }

        String tradePath = trade.getItemString();
        double requiredAmount = trade.getAmount();

        if (!InventoryUtils.hasEnough(p, tradePath, requiredAmount)) {
            p.sendMessage("§cYou don't have enough items for this trade.");
            return;
        }
        Optional<CharacterRef> seller = Denars.active(p);
        if (seller.isEmpty()) {
            p.sendMessage("§cChoose a character before you sell.");
            return;
        }
        SaleTake take = InventoryUtils.planTake(p, tradePath, requiredAmount);
        if (take.total() != requiredAmount) {
            p.sendMessage("§cThe market could not take the items for this trade.");
            return;
        }
        double base = PriceCalculator.calculatePrice(trade);
        double price = applyFreshness(base, take);
        long cents = Math.round(price * 100);
        long tax = Denars.taxFor(seller.get(), cents);
        try {
            if (new MarketSale(MarketBlock.plugin.getDataFolder().toPath().resolve("pending-sales"),
                    seller.get(), take, cents).settle() != Result.APPLIED) {
                p.sendMessage("§cThe market could not pay you, so it kept nothing.");
                return;
            }
        } catch (Exception failure) {
            MarketBlock.plugin.getLogger().warning("Sale needs review for " + p.getUniqueId() + ": " + failure);
            p.sendMessage("§cThis sale needs staff review before you sell again.");
            return;
        }
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        sendSaleBreakdown(p, take, (int) requiredAmount, base, price);
        if (tax > 0) {
            p.sendMessage("§7  Tax: " + formatMoney(tax / 100.0) + "d");
        }
        trade.sell();
        Bukkit.getPluginManager().callEvent(new MarketSaleEvent(p, trade, price, requiredAmount));
        update();
    }

    private static double applyFreshness(double base, SaleTake take) {
        int taken = take.total();
        double weight = 0;
        for (var entry : take.getCounts().entrySet()) {
            weight += entry.getValue() * Cache.freshnessMultiplier(entry.getKey());
        }
        double price = base * (weight / taken);
        price = Math.round(price * 100.0) / 100.0;
        return Math.max(0.01, price);
    }

    private static void sendSaleBreakdown(Player p, SaleTake take, int amount, double base, double price) {
        p.sendMessage("§aSold " + amount + " for " + formatMoney(price) + "d");
        if (take.allFresh()) {
            return;
        }
        p.sendMessage("§7  Base: " + formatMoney(base) + "d");
        for (String step : Cache.freshnessPrice.keySet()) {
            Integer n = take.getCounts().get(step);
            if (n == null || n <= 0) {
                continue;
            }
            p.sendMessage("§7  " + capitalize(step) + " x" + n + ": " + Cache.freshnessPercent(step) + "%");
        }
        for (var entry : take.getCounts().entrySet()) {
            if (Cache.freshnessPrice.containsKey(entry.getKey()) || entry.getValue() <= 0) {
                continue;
            }
            p.sendMessage("§7  " + capitalize(entry.getKey()) + " x" + entry.getValue() + ": "
                    + Cache.freshnessPercent(entry.getKey()) + "%");
        }
    }

    private static String formatMoney(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static String capitalize(String step) {
        if (step == null || step.isBlank()) {
            return "Fresh";
        }
        return Character.toUpperCase(step.charAt(0)) + step.substring(1);
    }
}
