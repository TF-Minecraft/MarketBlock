package net.tfminecraft.marketblock.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.tfminecraft.tlibs.objects.api.subapi.StringFormatter;
import net.tfminecraft.marketblock.Cache;
import net.tfminecraft.marketblock.MarketBlock;
import net.tfminecraft.marketblock.inventory.holder.MBGUI;
import net.tfminecraft.marketblock.inventory.holder.MBHolder;
import net.tfminecraft.marketblock.trade.Category;
import net.tfminecraft.marketblock.trade.Trade;
import net.tfminecraft.marketblock.util.DemandFormatter;
import net.tfminecraft.marketblock.util.PriceCalculator;

public class TradeView {
    public void tradeView(Inventory inv, Player p, Category cat) {
        boolean open = false;
        if(inv == null) {
            inv = MarketBlock.plugin.getServer().createInventory(new MBHolder(MBGUI.TRADE, cat.getId()), 54, cat.getTradesTitle());
            open = true;
        }
        List<Trade> trades = new ArrayList<>(cat.getTrades());
        trades.sort(Comparator.comparingInt(Trade::getGroup));
		for(int i = 0; i < Cache.slots.size() && i < trades.size(); i++) {
            int slot = Cache.slots.get(i);
            Trade trade = trades.get(i);
            inv.setItem(slot, createTradeItem(trade, cat));
        }
		int slotn = 0;
		while(slotn < inv.getSize()) {
			if(inv.getItem(slotn) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName("§8 ");
				fill.setItemMeta(fm);
				inv.setItem(slotn, fill);
			}
			slotn++;
		}
        inv.setItem(53, getBackButton());
		if(open) p.openInventory(inv);
	}

    private ItemStack createTradeItem(Trade trade, Category cat) {
        ItemStack i = trade.getIconItem();
        if (i == null || i.getItemMeta() == null) {
            i = new ItemStack(Material.GRAY_DYE, 1);
        }
        ItemMeta m = i.getItemMeta();
        if (m == null) {
            return i;
        }
        m.setDisplayName(StringFormatter.formatHex(cat.getColorHex() + getPlainName(i) + "#d6d3a7x#b6e66e" + trade.getAmount()));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#af97bdDemand§e: "+DemandFormatter.getDemandBar(trade.getDemand(), trade.getDemandLimit())));
        lore.add(StringFormatter.formatHex("#d97b66Price§e: §6"+PriceCalculator.calculatePrice(trade)+"d"));
        String match = trade.getItemString();
        if (match != null && match.regionMatches(true, 0, "c.", 0, 2)) {
            lore.add(StringFormatter.formatHex("#9e9e9eFresh " + Cache.freshnessPercent("fresh")
                    + "% · Stale " + Cache.freshnessPercent("stale")
                    + "% · Rotten " + Cache.freshnessPercent("rotten") + "%"));
        }
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#6b9c68[#fafa16Click to Trade#6b9c68]"));
        m.setLore(lore);
        NamespacedKey key = new NamespacedKey(MarketBlock.plugin, "trade_id");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, trade.getId());
        i.setItemMeta(m);
        return i;
    }

    private ItemStack getBackButton() {
        ItemStack i = new ItemStack(Material.BARRIER, 1);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName("§cBack");
        i.setItemMeta(m);
        return i;
    }

    private String getPlainName(ItemStack item) {
        ItemMeta m = item.getItemMeta();
        if (m.hasDisplayName()) {
            return ChatColor.stripColor(m.getDisplayName());
        }
        return StringFormatter.getVanillaName(item.getType());
    }
}
