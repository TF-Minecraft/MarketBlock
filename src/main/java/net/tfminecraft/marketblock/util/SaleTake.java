package net.tfminecraft.marketblock.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.inventory.ItemStack;

/** The goods a sale takes: counts per freshness step and the planned amount from each stack. */
public class SaleTake {
    private record Part(ItemStack stack, int amount) {
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

    void plan(ItemStack stack, int amount) {
        parts.add(new Part(stack, amount));
    }

    /** Takes the planned amounts from their stacks. */
    public void apply() {
        for (Part part : parts) {
            part.stack().setAmount(part.stack().getAmount() - part.amount());
        }
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
