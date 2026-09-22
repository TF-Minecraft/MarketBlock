package net.tfminecraft.marketblock.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import net.tfminecraft.marketblock.trade.Trade;

/**
 * Fired after the payout in TradeManager.tradeClick, once a player has sold
 * items to the market block. Informational only; not cancellable.
 */
public class MarketSaleEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final Trade trade;
    private final double price;
    private final double amount;

    public MarketSaleEvent(Player player, Trade trade, double price, double amount) {
        this.player = player;
        this.trade = trade;
        this.price = price;
        this.amount = amount;
    }

    public Player getPlayer() {
        return player;
    }

    public Trade getTrade() {
        return trade;
    }

    public double getPrice() {
        return price;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
