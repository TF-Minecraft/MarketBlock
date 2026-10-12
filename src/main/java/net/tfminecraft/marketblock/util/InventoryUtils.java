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
        return Double.isFinite(requiredAmount) && requiredAmount > 0
                && requiredAmount <= Integer.MAX_VALUE
                && requiredAmount == Math.rint(requiredAmount)
                && getTotalAmount(p, path) >= requiredAmount;
    }

    /**
     * Plans which matching stacks a sale would take, without changing the inventory. Call
     * {@link SaleTake#apply()} to take them.
     */
    public static SaleTake planTake(Player p, String path, double amountToRemove) {
        SaleTake take = new SaleTake();
        if (!Double.isFinite(amountToRemove) || amountToRemove <= 0
                || amountToRemove > Integer.MAX_VALUE
                || amountToRemove != Math.rint(amountToRemove)) {
            return take;
        }
        double remaining = amountToRemove;

        ItemStack[] contents = p.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) continue;

            if (!TLibs.getItemAPI().getChecker().checkItemWithPath(item, path)) continue;

            int stackAmount = item.getAmount();
            int taken = stackAmount <= remaining ? stackAmount : (int) remaining;
            take.add(FreshnessLookup.stepId(item), taken);
            take.plan(p.getInventory(), slot, taken);
            remaining -= taken;

            if (remaining <= 0) break;
        }
        return take;
    }
}
