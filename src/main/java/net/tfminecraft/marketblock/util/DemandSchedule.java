package net.tfminecraft.marketblock.util;

public final class DemandSchedule {
    public static final double DEFAULT_HOURS = 4.0;
    public static final long TICKS_PER_HOUR = 60L * 60L * 20L;

    private DemandSchedule() {
    }

    public static double normalizeHours(double configuredHours) {
        if (!Double.isFinite(configuredHours) || configuredHours <= 0.0) {
            return DEFAULT_HOURS;
        }
        return configuredHours;
    }

    public static long periodTicks(double configuredHours) {
        double ticks = normalizeHours(configuredHours) * TICKS_PER_HOUR;
        if (!Double.isFinite(ticks) || ticks >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, Math.round(ticks));
    }
}
