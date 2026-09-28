package net.tfminecraft.marketblock.manager.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TradeInputTest {

    @Test
    void rejectsValuesThatAreNotFiniteNumbers() {
        for (String text : new String[] { "NaN", "Infinity", "-Infinity", "1e400", "", "twenty", null }) {
            assertNull(TradeInput.demandLimit(text), text);
            assertNull(TradeInput.priceChange(text), text);
            assertNull(TradeInput.restingPrice(text), text);
        }
    }

    @Test
    void demandLimitMustBeAtLeastOne() {
        assertNull(TradeInput.demandLimit("0.5"));
        assertEquals(1.0, TradeInput.demandLimit("1"));
        assertEquals(20.0, TradeInput.demandLimit(" 20 "));
    }

    @Test
    void priceChangeMayBeZeroButNotNegative() {
        assertNull(TradeInput.priceChange("-1"));
        assertEquals(0.0, TradeInput.priceChange("0"));
        assertEquals(8.0, TradeInput.priceChange("8"));
    }

    @Test
    void restingPriceMustBeAboveZero() {
        assertNull(TradeInput.restingPrice("0"));
        assertNull(TradeInput.restingPrice("-4"));
        assertEquals(0.5, TradeInput.restingPrice("0.5"));
    }

    @Test
    void cancelIgnoresCaseAndSpaces() {
        assertTrue(TradeInput.isCancel(" Cancel "));
        assertFalse(TradeInput.isCancel("cancelled"));
        assertFalse(TradeInput.isCancel(null));
    }
}
