package net.tfminecraft.marketblock.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.cooking.item.FoodItem;
import net.tfminecraft.cooking.item.tag.TagTrack;

public class FreshnessLookup {

    public static boolean cookingLoaded() {
        return Bukkit.getPluginManager().getPlugin("Cooking") != null;
    }

    public static String stepId(ItemStack item) {
        if (item == null || !cookingLoaded()) {
            return "fresh";
        }
        FoodItem food = FoodItem.fromItem(item);
        if (food == null) {
            return "fresh";
        }
        TagTrack track = food.getTagTrack("freshness");
        if (track == null || track.getCurrentStep() == null) {
            return "fresh";
        }
        String id = track.getCurrentStep().getId();
        return id == null || id.isBlank() ? "fresh" : id.toLowerCase();
    }
}
