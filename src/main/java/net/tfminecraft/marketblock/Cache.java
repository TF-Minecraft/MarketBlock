package net.tfminecraft.marketblock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.block.Block;

import net.tfminecraft.tlibs.TLibs;

public class Cache {
    public static List<Integer> slots = new ArrayList<>();
    public static String marketBlock;
    public static final Map<String, Double> freshnessPrice = new LinkedHashMap<>();

    public static boolean blockIsMarketBlock(Block b) {
        return TLibs.getBlockAPI().getChecker().checkBlock(b, marketBlock);
    }

    public static double freshnessMultiplier(String stepId) {
        if (stepId == null || stepId.isBlank()) {
            return 1.0;
        }
        Double value = freshnessPrice.get(stepId.toLowerCase());
        return value == null ? 1.0 : value;
    }

    public static int freshnessPercent(String stepId) {
        return (int) Math.round(freshnessMultiplier(stepId) * 100.0);
    }
}
