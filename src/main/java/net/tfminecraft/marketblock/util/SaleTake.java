package net.tfminecraft.marketblock.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** The goods a sale takes: counts per freshness step and the planned amount from each stack. */
public class SaleTake {
    private record Part(PlayerInventory inventory, int slot, ItemStack stack, int amount) {
    }

    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private final List<Part> parts = new ArrayList<>();

    public void add(String stepId, int amount) {
        if (amount <= 0) {
            return;
        }
        String key = stepId == null || stepId.isBlank() ? "fresh" : stepId.toLowerCase(Locale.ROOT);
        counts.merge(key, amount, Integer::sum);
    }

    void plan(PlayerInventory inventory, int slot, int amount) {
        parts.add(new Part(inventory, slot, inventory.getItem(slot).clone(), amount));
    }

    /** Takes the planned amounts from their stacks. */
    public void apply() {
        for (Part part : parts) {
            if (!part.stack().equals(part.inventory().getItem(part.slot()))) {
                throw new IllegalStateException("Paid market goods changed before removal");
            }
        }
        for (Part part : parts) {
            part.inventory().getItem(part.slot()).setAmount(part.stack().getAmount() - part.amount());
        }
    }

    public String description() {
        return parts.stream().map(part -> "slot " + part.slot() + ": " + part.amount() + " x " + part.stack()).toList().toString();
    }

    public Map<String, Integer> getCounts() {
        return counts;
    }

    public int total() {
        int sum = 0;
        for (int n : counts.values()) {
            sum += n;
        }
        return sum;
    }

    public boolean allFresh() {
        if (counts.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            if (!"fresh".equals(entry.getKey())) {
                return false;
            }
        }
        return true;
    }
}
