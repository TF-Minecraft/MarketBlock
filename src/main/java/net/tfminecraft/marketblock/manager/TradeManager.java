package net.tfminecraft.marketblock.manager;

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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.marketblock.Cache;
import net.tfminecraft.marketblock.MarketBlock;
import net.tfminecraft.denareconomy.DenarEconomy;
import net.tfminecraft.marketblock.events.MarketSaleEvent;
import net.tfminecraft.marketblock.inventory.holder.MBGUI;
import net.tfminecraft.marketblock.inventory.holder.MBHolder;
import net.tfminecraft.marketblock.loader.CategoryLoader;
import net.tfminecraft.marketblock.loader.TradeLoader;
import net.tfminecraft.marketblock.trade.Category;
import net.tfminecraft.marketblock.trade.Trade;
import net.tfminecraft.marketblock.util.InventoryUtils;
import net.tfminecraft.marketblock.util.PriceCalculator;
import net.tfminecraft.marketblock.util.SaleTake;

public class TradeManager implements Listener {
    InventoryManager inv = new InventoryManager();

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
        NamespacedKey key;
        switch (type) {
            case CATEGORY:
                key = new NamespacedKey(MarketBlock.plugin, "category_id");
                break;
            case TRADE:
                key = new NamespacedKey(MarketBlock.plugin, "trade_id");
                break;
            default:
                key = new NamespacedKey(MarketBlock.plugin, "none");
                break;
        }
        return i.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public void start() {
        demandCycle();
    }

    public void demandCycle() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for(Trade t : TradeLoader.getTrades().values()) {
                    t.demand();
                }
                update();
            }
        }.runTaskTimer(MarketBlock.plugin, 0, 60*60*20L);
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

        String id = getItemId(h.getType(), i);
        if (id == null) return;

        Trade trade = TradeLoader.getTradeById(id);
        if (trade == null) {
            p.sendMessage("§cCould not find this trade.");
            return;
        }

        String tradePath = trade.getItemString();
        double requiredAmount = trade.getAmount();

        if (!InventoryUtils.hasEnough(p, tradePath, requiredAmount)) {
            p.sendMessage("§cYou don't have enough items for this trade.");
            return;
        }
        SaleTake take = InventoryUtils.removeItems(p, tradePath, requiredAmount);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        double base = PriceCalculator.calculatePrice(trade);
        double price = applyFreshness(base, take);
        DenarEconomy.getMoneyManager().addMoney(p, price, false, true);
        sendSaleBreakdown(p, take, requiredAmount, base, price);
        trade.sell();
        Bukkit.getPluginManager().callEvent(new MarketSaleEvent(p, trade, price, requiredAmount));
        update();
    }

    private static double applyFreshness(double base, SaleTake take) {
        int taken = take.total();
        if (taken <= 0) {
            return Math.max(0.01, base);
        }
        double weight = 0;
        for (var entry : take.getCounts().entrySet()) {
            weight += entry.getValue() * Cache.freshnessMultiplier(entry.getKey());
        }
        double price = base * (weight / taken);
        price = Math.round(price * 100.0) / 100.0;
        return Math.max(0.01, price);
    }

    private static void sendSaleBreakdown(Player p, SaleTake take, double amount, double base, double price) {
        p.sendMessage("§aSold " + formatAmount(amount) + " for " + formatMoney(price) + "d");
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

    private static String formatAmount(double amount) {
        if (amount == Math.rint(amount)) {
            return String.valueOf((long) amount);
        }
        return String.valueOf(amount);
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
