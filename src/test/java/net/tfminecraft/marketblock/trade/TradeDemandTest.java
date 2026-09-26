package net.tfminecraft.marketblock.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TradeDemandTest {

    @Test
    void eachRecoveryTickRaisesDemandByOneUpToJustUnderSeven() {
        Trade trade = trade(1_000);
        trade.setDemand(10);

        for (int i = 0; i < 20; i++) {
            double before = trade.getDemand();
            trade.demand();
            double increase = trade.getDemand() - before;
            assertTrue(increase >= 1.0, "increase was " + increase);
            assertTrue(increase < 7.0, "increase was " + increase);
        }
    }

    @Test
    void recoveryStopsAtTheDemandLimit() {
        Trade trade = trade(20);
        trade.setDemand(19.5);

        trade.demand();

        assertEquals(20.0, trade.getDemand());
    }

    @Test
    void sellingCannotDropDemandBelowOne() {
        Trade trade = trade(20);
        trade.setDemand(1);

        trade.sell();

        assertEquals(1.0, trade.getDemand());
    }

    private static Trade trade(double limit) {
        return new Trade("oak", new Category(), limit / 2, limit, 1, "v.oak_log", 2, 64, 0);
    }
}
