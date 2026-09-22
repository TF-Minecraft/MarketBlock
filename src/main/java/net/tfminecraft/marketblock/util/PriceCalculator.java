package net.tfminecraft.marketblock.util;

import net.tfminecraft.marketblock.trade.Trade;

public class PriceCalculator {

    public static double calculatePrice(Trade t) {
        double midDemand = t.getDemandLimit() / 2.0;
        double price = t.getItemRestingPrice() * (t.getDemand() / midDemand);
        price = Math.max(price, 0.0);
        price = Math.round(price * 100.0) / 100.0;
        return Math.max(0.01, price);
    }
}
