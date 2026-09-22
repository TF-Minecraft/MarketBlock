package net.tfminecraft.marketblock.util;

import net.tfminecraft.tlibs.objects.api.subapi.StringFormatter;

public class DemandFormatter {

    private static final int TOTAL_BARS = 20;

    public static String getDemandBar(double demand, double maxDemand) {
        double ratio = maxDemand <= 0 ? 0 : demand / maxDemand;
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        int filledBars = (int) Math.round(ratio * TOTAL_BARS);

        int[] rgb = interpolateColor(ratio);
        String filledHex = toHex(rgb[0], rgb[1], rgb[2]);
        String emptyHex = toHex(rgb[0] / 3, rgb[1] / 3, rgb[2] / 3);

        String filled = filledBars > 0
                ? StringFormatter.formatHex(filledHex + "|".repeat(filledBars))
                : "";
        int emptyBars = TOTAL_BARS - filledBars;
        String empty = emptyBars > 0
                ? StringFormatter.formatHex(emptyHex + "|".repeat(emptyBars))
                : "";

        return StringFormatter.formatHex("#aaaaaa[") + filled + empty + StringFormatter.formatHex("#aaaaaa]");
    }

    private static int[] interpolateColor(double progress) {
        int r1 = 0xaa, g1 = 0x00, b1 = 0x00;
        int r2 = 0x00, g2 = 0xff, b2 = 0x00;

        int r = (int) Math.round(r1 + (r2 - r1) * progress);
        int g = (int) Math.round(g1 + (g2 - g1) * progress);
        int b = (int) Math.round(b1 + (b2 - b1) * progress);
        return new int[] { r, g, b };
    }

    private static String toHex(int r, int g, int b) {
        return String.format("#%02x%02x%02x", clamp(r), clamp(g), clamp(b));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
