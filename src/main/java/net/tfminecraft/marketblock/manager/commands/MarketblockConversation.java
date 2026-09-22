package net.tfminecraft.marketblock.manager.commands;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.marketblock.trade.Category;

public class MarketblockConversation {
    private final Player player;
    private final ItemStack item;

    private Category category;
    private String id;
    private double demandLimit;
    private int group;

    private int step = 0;
    private double priceChange;
    private double restingPrice;



    public MarketblockConversation(Player player, ItemStack item) {
        this.player = player;
        this.item = item;
    }

    public Category getCategory() { return category; }

    public Player getPlayer() { return player; }
    public ItemStack getItem() { return item; }

    public int getStep() { return step; }
    public void nextStep() { step++; }

    public void setId(String id) { this.id = id; }
    public void setDemandLimit(double demandLimit) { this.demandLimit = demandLimit; }
    public void setGroup(int group) { this.group = group; }
    public void setCategory(Category c) { this.category = c; }

    public String getId() { return id; }
    public double getDemandLimit() { return demandLimit; }
    public int getGroup() { return group; }

    public void setPriceChange(double priceChange) {
        this.priceChange = priceChange;
    }

    public double getPriceChange() {
        return priceChange;
    }

    public void setRestingPrice(double restingPrice) {
        this.restingPrice = restingPrice;
    }

    public double getRestingPrice() {
        return restingPrice;
    }
}

