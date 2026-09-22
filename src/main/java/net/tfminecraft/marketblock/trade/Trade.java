package net.tfminecraft.marketblock.trade;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.tlibs.TLibs;
import net.tfminecraft.marketblock.loader.CategoryLoader;
import net.tfminecraft.marketblock.manager.commands.MarketblockConversation;

public class Trade {
    private String id;
    private Category category;
    private double demand;
    private double demandLimit;
    private double priceChange;
    private String item;
    private String icon;
    private double itemRestingPrice;
    private double amount;
    private int group;

    public Trade(String id, Category category, double demand, double demandLimit, double priceChange, String item, double itemRestingPrice, double amount, int group) {
        this.id = id;
        this.category = category;
        category.addTrade(this);
        this.demandLimit = Math.max(1, demandLimit);
        this.demand = clampDemand(demand);
        this.priceChange = priceChange;
        this.item = item;
        this.itemRestingPrice = itemRestingPrice;
        this.amount = amount;
        this.group = group;
    }

    public Trade(MarketblockConversation convo) {
        this.id = convo.getId();
        this.category = convo.getCategory();
        category.addTrade(this);
        this.demandLimit = Math.max(1, convo.getDemandLimit());
        this.demand = this.demandLimit / 2;
        this.item = TLibs.getItemAPI().getChecker().getAsStringPath(convo.getItem());
        this.itemRestingPrice = convo.getRestingPrice();
        this.priceChange = convo.getPriceChange();
        this.amount = convo.getItem().getAmount();
        this.group = convo.getGroup();
    }

    public void resetDemand() {
        demand = demandLimit / 2;
    }

    public void setDemand(double demand) {
        this.demand = clampDemand(demand);
    }

    public void demand() {
        demand += Math.max(1, Math.random() * 7);
        if (demand > demandLimit) demand = demandLimit;
    }

    public void sell() {
        demand -= Math.min(1, Math.random() * priceChange);
        if (demand < 1) demand = 1;
    }

    private double clampDemand(double value) {
        if (value < 1) return 1;
        if (value > demandLimit) return demandLimit;
        return value;
    }

    public String getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public double getDemand() {
        return demand;
    }

    public double getDemandLimit() {
        return demandLimit;
    }

    public double getPriceChange() {
        return priceChange;
    }

    public ItemStack getItem() {
        return TLibs.getItemAPI().getCreator().getItemFromPath(item);
    }

    public ItemStack getIconItem() {
        String path = icon != null && !icon.isBlank() ? icon : item;
        return TLibs.getItemAPI().getCreator().getItemFromPath(path);
    }

    public String getItemString() {
        return item;
    }

    public String getIconString() {
        return icon;
    }

    public double getItemRestingPrice() {
        return itemRestingPrice;
    }

    public double getAmount() {
        return amount;
    }

    public int getGroup() {
        return group;
    }

    public static Trade fromYaml(String id, ConfigurationSection section, double demand) {
        Category category = CategoryLoader.getByString(section.getString("category", "unknown"));
        double demandLimit = section.getDouble("demand-limit", 20);
        double priceChange = section.getDouble("price-change", 1);
        String item = section.getString("item", "v.gray_dye");
        double itemRestingPrice = section.getDouble("resting-price", 1);
        double amount = section.getDouble("amount", 64);
        int group = section.getInt("group", 0);
        Trade trade = new Trade(id, category, demand, demandLimit, priceChange, item, itemRestingPrice, amount, group);
        String icon = section.getString("icon");
        if (icon != null && !icon.isBlank()) {
            trade.icon = icon;
        }
        return trade;
    }
}
