package net.tfminecraft.marketblock.manager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import net.tfminecraft.marketblock.inventory.CategoryView;
import net.tfminecraft.marketblock.inventory.TradeView;
import net.tfminecraft.marketblock.trade.Category;

public class InventoryManager {
    private CategoryView categoryView = new CategoryView();
    private TradeView tradeView = new TradeView();

    public void categoryView(Inventory inv, Player p) {
        categoryView.categoryView(inv, p);
	}
    public void tradeView(Inventory inv, Player p, Category cat) {
        tradeView.tradeView(inv, p, cat);
	} 
}  
