package net.tfminecraft.marketblock.inventory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import net.tfminecraft.marketblock.Cache;
import net.tfminecraft.marketblock.MarketTestSupport;
import net.tfminecraft.marketblock.inventory.holder.MBGUI;
import net.tfminecraft.marketblock.inventory.holder.MBHolder;
import net.tfminecraft.marketblock.loader.CategoryLoader;
import net.tfminecraft.marketblock.manager.InventoryManager;
import net.tfminecraft.marketblock.trade.Category;
import net.tfminecraft.marketblock.trade.Trade;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
class MarketViewsTest extends MarketTestSupport {
    @BeforeEach
    void itemProvider() {
        Cache.slots = new ArrayList<>(List.of(0, 1));
        when(creator.getItemFromPath(anyString())).thenAnswer(i -> new ItemStack(Material.STONE));
    }

    @Test
    void categoriesHaveIdsNamesCountsAndFillEmptySlots() {
        var player = server.addPlayer();
        var category = new Category();
        CategoryLoader.categories.put(category.getId(), category);
        new InventoryManager().categoryView(null, player);
        var inventory = player.getOpenInventory().getTopInventory();
        assertEquals(54, inventory.getSize());
        assertEquals("§7Market Categories", player.getOpenInventory().getTitle());
        var holder = (MBHolder) inventory.getHolder();
        assertEquals(MBGUI.CATEGORY, holder.getType());
        assertEquals("none", holder.getId());
        assertNull(holder.getInventory());
        assertEquals("unknown", inventory.getItem(0).getItemMeta().getPersistentDataContainer()
            .get(new NamespacedKey(plugin, "category_id"), PersistentDataType.STRING));
        assertTrue(inventory.getItem(0).getItemMeta().getLore().getFirst().contains("0"));
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItem(53).getType());
        CategoryLoader.categories.clear();
        new InventoryManager().categoryView(inventory, player);
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItem(0).getType(), "Removed categories must disappear from a reused menu");
    }

    @Test
    void tradesSortByGroupShowFreshnessAndRemoveStaleOffersOnRefresh() {
        var player = server.addPlayer();
        var category = new Category();
        var late = new Trade("late", category, 10, 20, 1, "v.stone", 2, 16, 5);
        var early = new Trade("early", category, 10, 20, 1, "c.bread", 3, 8, 1);
        var named = new ItemStack(Material.BREAD);
        var meta = named.getItemMeta();
        meta.setDisplayName("§aBread");
        named.setItemMeta(meta);
        when(creator.getItemFromPath("c.bread")).thenReturn(named);
        new InventoryManager().tradeView(null, player, category);
        var inventory = player.getOpenInventory().getTopInventory();
        var key = new NamespacedKey(plugin, "trade_id");
        assertEquals("early", inventory.getItem(0).getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING));
        assertEquals("late", inventory.getItem(1).getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING));
        assertTrue(inventory.getItem(0).getItemMeta().getDisplayName().contains("Bread"));
        assertTrue(inventory.getItem(0).getItemMeta().getLore().stream().anyMatch(line -> line.contains("Stale")));
        assertEquals(Material.BARRIER, inventory.getItem(53).getType());
        assertEquals("§cBack", inventory.getItem(53).getItemMeta().getDisplayName());
        category.removeTrade(late);
        category.removeTrade(early);
        new InventoryManager().tradeView(inventory, player, category);
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItem(0).getType(), "Removed trades must disappear from open menus");
        assertEquals(Material.BARRIER, inventory.getItem(53).getType());
    }

    @Test
    void missingAndAirIconsUseVisibleFallbackAndConfiguredSlotCapacityIsRespected() {
        var player = server.addPlayer();
        var category = new Category();
        new Trade("missing", category, 1, 2, 0, "missing", 1, 1, 0);
        new Trade("air", category, 1, 2, 0, "air", 1, 1, 1);
        new Trade("overflow", category, 1, 2, 0, "overflow", 1, 1, 2);
        when(creator.getItemFromPath("missing")).thenReturn(null);
        when(creator.getItemFromPath("air")).thenReturn(new ItemStack(Material.AIR));
        new TradeView().tradeView(null, player, category);
        var inventory = player.getOpenInventory().getTopInventory();
        assertEquals(Material.GRAY_DYE, inventory.getItem(0).getType());
        assertEquals(Material.GRAY_DYE, inventory.getItem(1).getType());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, inventory.getItem(2).getType());
    }
}
