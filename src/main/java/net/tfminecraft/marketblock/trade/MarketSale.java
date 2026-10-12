package net.tfminecraft.marketblock.trade;

import java.util.UUID;

import net.tfminecraft.denareconomy.api.Denars;
import net.tfminecraft.denareconomy.api.Result;
import net.tfminecraft.marketblock.util.SaleTake;
import net.tfminecraft.tlibs.character.CharacterRef;

/**
 * One sale to the market. Its operation ID is fixed when the sale is planned, so settling it again
 * returns the ledger's first result instead of paying twice, and the goods are taken once, only
 * after the payment is applied.
 */
public final class MarketSale {
    private final String opId;
    private final CharacterRef seller;
    private final SaleTake take;
    private final long cents;
    private boolean taken;

    public MarketSale(CharacterRef seller, SaleTake take, long cents) {
        this.opId = "market:" + UUID.randomUUID();
        this.seller = seller;
        this.take = take;
        this.cents = cents;
    }

    public String opId() {
        return opId;
    }

    /** Pays the seller's pouch from the mint, taxed, then takes the goods if the payment applied. */
    public Result settle() {
        Result result = Denars.earn(opId, seller, Denars.pouch(seller), cents, true);
        if (result == Result.APPLIED && !taken) {
            take.apply();
            taken = true;
        }
        return result;
    }
}
