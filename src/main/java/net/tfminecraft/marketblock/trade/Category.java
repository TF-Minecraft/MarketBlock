package net.tfminecraft.marketblock.trade;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.tlibs.TLibs;
import net.tfminecraft.tlibs.objects.api.subapi.StringFormatter;

public class Category {
    private final String id;
    private final String name;
    private final String colorHex;
    private final String item;
    private Set<Trade> trades = new HashSet<>();

    public Category(String key, ConfigurationSection config) {
        this.id = key;
        String rawName = config.getString("name", "#ffffffUnknown Category");
        this.colorHex = leadingHex(rawName);
        this.name = StringFormatter.formatHex(rawName);
        this.item = config.getString("item", "v.gray_dye");
    }

    public Category() {
        this.id = "unknown";
        this.colorHex = "#ffffff";
        this.name = "§fUnknown Category";
        this.item = "v.gray_dye";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getTradesTitle() {
        return StringFormatter.formatHex(colorHex + ChatColor.stripColor(name) + " Trades");
    }

    private static String leadingHex(String raw) {
        if (raw == null) {
            return "#ffffff";
        }
        String trimmed = raw.trim();
        if (trimmed.length() >= 7 && trimmed.charAt(0) == '#') {
            return trimmed.substring(0, 7);
        }
        return "#ffffff";
    }

    public ItemStack getItem() {
        return TLibs.getItemAPI().getCreator().getItemFromPath(item);
    }

    public String getItemString() {
        return item;
    }

    public Set<Trade> getTrades() {
        return trades;
    }

    public void addTrade(Trade t) {
        if(trades.contains(t)) return;
        trades.add(t);
    }

    public void removeTrade(Trade t) {
        if(!trades.contains(t)) return;
        trades.remove(t);
    }
}
