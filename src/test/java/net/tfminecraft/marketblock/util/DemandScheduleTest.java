package net.tfminecraft.marketblock.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DemandScheduleTest {

    @Test
    void fourHoursIsTheDefaultPeriod() {
        assertEquals(4.0 * DemandSchedule.TICKS_PER_HOUR, DemandSchedule.periodTicks(4));
        assertEquals(288_000L, DemandSchedule.periodTicks(DemandSchedule.DEFAULT_HOURS));
    }

    @Test
    void oneHourMatchesThePreviousHardcodedInterval() {
        assertEquals(72_000L, DemandSchedule.periodTicks(1));
    }

    @Test
    void fractionalHoursRoundToTheNearestTick() {
        assertEquals(36_000L, DemandSchedule.periodTicks(0.5));
        assertEquals(1L, DemandSchedule.periodTicks(1.0 / DemandSchedule.TICKS_PER_HOUR / 10));
    }

    @Test
    void missingOrInvalidHoursFallBackToFour() {
        assertEquals(DemandSchedule.DEFAULT_HOURS, DemandSchedule.normalizeHours(0));
        assertEquals(DemandSchedule.DEFAULT_HOURS, DemandSchedule.normalizeHours(-2));
        assertEquals(DemandSchedule.DEFAULT_HOURS, DemandSchedule.normalizeHours(Double.NaN));
        assertEquals(DemandSchedule.DEFAULT_HOURS, DemandSchedule.normalizeHours(Double.POSITIVE_INFINITY));
        assertEquals(288_000L, DemandSchedule.periodTicks(0));
    }
}
