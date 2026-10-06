package net.tfminecraft.marketblock.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.tfminecraft.marketblock.trade.Category;
import net.tfminecraft.marketblock.trade.Trade;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

class PriceAndDemandTest {
    @Test
    void pricesScaleWithDemandAndRoundToTheNearestCent() {
        Trade trade = trade(10, 2.345);
        assertEquals(2.35, PriceCalculator.calculatePrice(trade));
        trade.setDemand(5);
        assertEquals(1.17, PriceCalculator.calculatePrice(trade));
        trade.setDemand(20);
        assertEquals(4.69, PriceCalculator.calculatePrice(trade));
    }

    @Test
    void pricesNeverFallBelowOneCentEvenForZeroOrNegativeConfiguredPrices() {
        assertEquals(0.01, PriceCalculator.calculatePrice(trade(1, 0.001)));
        assertEquals(0.01, PriceCalculator.calculatePrice(trade(1, 0)));
        assertEquals(0.01, PriceCalculator.calculatePrice(trade(1, -10)));
        assertEquals(0.02, PriceCalculator.calculatePrice(trade(10, 0.015)));
    }

    @Test
    void emptyDemandUsesTwentyDarkRedBarsInsideGrayBrackets() {
        String bar = DemandFormatter.getDemandBar(0, 20);
        assertEquals("§x§a§a§a§a§a§a[§x§3§8§0§0§0§0" + "|".repeat(20)
                + "§x§a§a§a§a§a§a]", bar);
        assertEquals("[" + "|".repeat(20) + "]", ChatColor.stripColor(bar));
    }

    @Test
    void halfDemandSeparatesTenFilledAndTenDimmedBars() {
        assertEquals("§x§a§a§a§a§a§a[§x§5§5§8§0§0§0" + "|".repeat(10)
                + "§x§1§c§2§a§0§0" + "|".repeat(10) + "§x§a§a§a§a§a§a]",
                DemandFormatter.getDemandBar(10, 20));
    }

    @Test
    void fullDemandUsesTwentyGreenBarsAndNoEmptySegment() {
        assertEquals("§x§a§a§a§a§a§a[§x§0§0§f§f§0§0" + "|".repeat(20)
                + "§x§a§a§a§a§a§a]", DemandFormatter.getDemandBar(20, 20));
    }

    @Test
    void demandBarsClampOutOfRangeValuesAndTreatNonPositiveLimitsAsEmpty() {
        String empty = DemandFormatter.getDemandBar(0, 20);
        String full = DemandFormatter.getDemandBar(20, 20);
        assertEquals(empty, DemandFormatter.getDemandBar(-5, 20));
        assertEquals(full, DemandFormatter.getDemandBar(30, 20));
        assertEquals(empty, DemandFormatter.getDemandBar(10, 0));
        assertEquals(empty, DemandFormatter.getDemandBar(10, -2));
    }

    @Test
    void fractionalDemandRoundsTheFilledBarCountInsteadOfTruncating() {
        String bar = DemandFormatter.getDemandBar(1, 8);
        String[] visibleSegments = bar.split("§x(?:§[0-9a-f]){6}");
        assertArrayEquals(new String[] {"", "[", "|||", "|".repeat(17), "]"}, visibleSegments);
    }

    @Test
    void emptySaleHasNoItemsAndIsFresh() {
        SaleTake sale = new SaleTake();
        assertTrue(sale.getCounts().isEmpty());
        assertEquals(0, sale.total());
        assertTrue(sale.allFresh());
        sale.add("rotten", 0);
        sale.add("rotten", -2);
        assertTrue(sale.getCounts().isEmpty());
        assertTrue(sale.allFresh());
    }

    @Test
    void missingAndBlankFreshnessLabelsMergeWithFreshQuantities() {
        SaleTake sale = new SaleTake();
        sale.add(null, 2);
        sale.add("", 3);
        sale.add(" \t ", 4);
        sale.add("FRESH", 5);
        assertEquals(Map.of("fresh", 14), sale.getCounts());
        assertEquals(14, sale.total());
        assertTrue(sale.allFresh());
    }

    @Test
    void mixedFreshnessKeepsFirstSeenOrderAndAggregatesRepeatedSteps() {
        SaleTake sale = new SaleTake();
        sale.add("fresh", 8);
        sale.add("Stale", 3);
        sale.add("rotten", 2);
        sale.add("STALE", 4);
        assertEquals(List.of("fresh", "stale", "rotten"), new ArrayList<>(sale.getCounts().keySet()));
        assertEquals(Map.of("fresh", 8, "stale", 7, "rotten", 2), sale.getCounts());
        assertEquals(17, sale.total());
        assertFalse(sale.allFresh());
    }

    @Test
    void freshnessKeysDoNotDependOnTheServerLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            SaleTake sale = new SaleTake();
            sale.add("RIPE", 2);
            sale.add("ripe", 3);
            assertEquals(Map.of("ripe", 5), sale.getCounts());
            assertEquals(5, sale.total());
            assertFalse(sale.allFresh());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void zeroQuantityStepsDoNotMakeASaleNonFresh() {
        SaleTake sale = new SaleTake();
        sale.add("fresh", 4);
        sale.getCounts().put("stale", 0);
        assertTrue(sale.allFresh());
        assertEquals(4, sale.total());
    }

    @Test
    void recoveryPeriodsSaturateAtLongMaxForFiniteOrOverflowingProducts() {
        assertEquals(Long.MAX_VALUE, DemandSchedule.periodTicks(Double.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, DemandSchedule.periodTicks(Long.MAX_VALUE / 36_000.0));
        assertEquals(72_000_000_000L, DemandSchedule.periodTicks(1_000_000));
        assertEquals(DemandSchedule.DEFAULT_HOURS, DemandSchedule.normalizeHours(Double.NEGATIVE_INFINITY));
    }

    private static Trade trade(double demand, double restingPrice) {
        return new Trade("oak", new Category(), demand, 20, 1, "v.oak_log", restingPrice, 64, 0);
    }
}
