package net.tfminecraft.marketblock.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.tlibs.TLibs;

public class InventoryUtils {

    public static double getTotalAmount(Player p, String path) {
        double total = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;

            if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, path)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public static boolean hasEnough(Player p, String path, double requiredAmount) {
        return getTotalAmount(p, path) >= requiredAmount;
    }

    public static SaleTake removeItems(Player p, String path, double amountToRemove) {
        SaleTake take = new SaleTake();
        double remaining = amountToRemove;

        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;

            if (!TLibs.getItemAPI().getChecker().checkItemWithPath(item, path)) continue;

            int stackAmount = item.getAmount();
            int taken = stackAmount <= remaining ? stackAmount : (int) remaining;
            take.add(FreshnessLookup.stepId(item), taken);

            if (stackAmount <= remaining) {
                remaining -= stackAmount;
                item.setAmount(0);
            } else {
                item.setAmount(stackAmount - taken);
                remaining = 0;
            }

            if (remaining <= 0) break;
        }
        return take;
    }
}
