package net.tfminecraft.marketblock.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class SaleTake {
    private final Map<String, Integer> counts = new LinkedHashMap<>();

    public void add(String stepId, int amount) {
        if (amount <= 0) {
            return;
        }
        String key = stepId == null || stepId.isBlank() ? "fresh" : stepId.toLowerCase(Locale.ROOT);
        counts.merge(key, amount, Integer::sum);
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
