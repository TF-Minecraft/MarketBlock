package net.tfminecraft.marketblock.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import net.tfminecraft.denareconomy.api.Denars;
import net.tfminecraft.denareconomy.api.Result;
import net.tfminecraft.marketblock.Cache;
import net.tfminecraft.marketblock.MarketTestSupport;
import net.tfminecraft.marketblock.TestMoney;
import net.tfminecraft.marketblock.events.MarketSaleEvent;
import net.tfminecraft.marketblock.inventory.holder.MBGUI;
import net.tfminecraft.marketblock.inventory.holder.MBHolder;
import net.tfminecraft.marketblock.loader.CategoryLoader;
import net.tfminecraft.marketblock.loader.TradeLoader;
import net.tfminecraft.marketblock.trade.Category;
import net.tfminecraft.marketblock.trade.Trade;
import net.tfminecraft.marketblock.util.FreshnessLookup;
import net.tfminecraft.marketblock.util.InventoryUtils;
import net.tfminecraft.marketblock.util.SaleTake;
import net.tfminecraft.tlibs.TLibs;
import net.tfminecraft.tlibs.character.CharacterRef;
import net.tfminecraft.tlibs.objects.api.BlockAPI;
import net.tfminecraft.tlibs.objects.api.subapi.BlockChecker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class TradeManagerTest extends MarketTestSupport {
    private TradeManager manager;
    private PlayerMock player;
    private Category category;
    private TestMoney money;
    private CharacterRef seller;

    @BeforeEach
    void prepareMarket() {
        player = server.addPlayer();
        money = TestMoney.open(directory);
        seller = money.play(player);
        manager = new TradeManager();
        category = new Category();
        CategoryLoader.categories.put(category.getId(), category);
        Cache.slots = new ArrayList<>(List.of(0, 1));
        Cache.freshnessPrice.put("fresh", 1.0);
        Cache.freshnessPrice.put("stale", 0.5);
        Cache.freshnessPrice.put("rotten", 0.1);
        when(creator.getItemFromPath(anyString())).thenReturn(new ItemStack(Material.CARROT));
        when(checker.checkItemWithPath(any(), eq("v.carrot"))).thenAnswer(i -> i.<ItemStack>getArgument(0).getType() == Material.CARROT);
    }

    @AfterEach
    void closeLedger() {
        TestMoney.close();
    }

    @Test
    void successfulSaleConsumesGoodsPaysTheSellingCharacterOnceAndPublishesMatchingEvent() {
        var trade = trade("sale", 4);
        String taxes = TestMoney.taxAt(10);
        player.getInventory().setItem(0, new ItemStack(Material.CARROT, 2));
        player.getInventory().setItem(1, new ItemStack(Material.CARROT, 4));
        var holder = openTrade();
        try (var freshness = mockStatic(FreshnessLookup.class)) {
            freshness.when(() -> FreshnessLookup.stepId(any())).thenReturn("stale", "fresh");
            manager.tradeClick(player, holder, tagged("trade_id", "sale"), null);
        }
        assertEquals(540, Denars.balance(Denars.pouch(seller)));
        assertEquals(60, Denars.balance(taxes));
        assertEquals(0, player.getInventory().getItem(0).getAmount());
        assertEquals(2, player.getInventory().getItem(1).getAmount());
        assertEquals("§aSold 4 for 6d", player.nextMessage());
        assertEquals("§7  Base: 8d", player.nextMessage());
        assertEquals("§7  Fresh x2: 100%", player.nextMessage());
        assertEquals("§7  Stale x2: 50%", player.nextMessage());
        assertEquals("§7  Tax: 0.6d", player.nextMessage());
        assertNull(player.nextMessage());
        var events = server.getPluginManager().getFiredEvents().filter(MarketSaleEvent.class::isInstance).map(MarketSaleEvent.class::cast).toList();
        assertEquals(1, events.size());
        var event = events.getFirst();
        assertSame(player, event.getPlayer());
        assertSame(trade, event.getTrade());
        assertEquals(6, event.getPrice());
        assertEquals(4, event.getAmount());
        assertSame(MarketSaleEvent.getHandlerList(), event.getHandlers());
    }

    @Test
    void replayingTheSameClickDoesNotSellAnotherBatch() {
        trade("sale", 2);
        var holder = openTrade();
        player.getInventory().setItem(0, new ItemStack(Material.CARROT, 5));
        var event = clickEvent(tagged("trade_id", "sale"), player.getOpenInventory().getTopInventory());
        manager.click(event);
        manager.click(event);
        assertEquals(3, player.getInventory().getItem(0).getAmount());
        assertEquals(800, Denars.balance(Denars.pouch(seller)));
    }

    @Test
    void interruptedPostingBlocksNewCallbacksAndSurvivesManagerRecreation() throws Exception {
        trade("sale", 2);
        var holder = openTrade();
        player.getInventory().setItem(0, new ItemStack(Material.CARROT, 5));
        try (var denars = mockStatic(Denars.class, CALLS_REAL_METHODS)) {
            denars.when(() -> Denars.earn(anyString(), eq(seller), anyString(), anyLong(), eq(true)))
                    .thenAnswer(call -> {
                        call.callRealMethod();
                        throw new IllegalStateException("interrupted after posting");
                    });
            manager.tradeClick(player, holder, tagged("trade_id", "sale"), null);
        }
        assertEquals("§cThis sale needs staff review before you sell again.", player.nextMessage());
        var intent = directory.resolve("pending-sales/" + player.getUniqueId() + ".properties");
        String saved = java.nio.file.Files.readString(intent);
        assertTrue(saved.contains("character=" + seller.characterId()));
        manager = new TradeManager();
        manager.tradeClick(player, holder, tagged("trade_id", "sale"), null);
        assertEquals("§cThis sale needs staff review before you sell again.", player.nextMessage());
        assertEquals(saved, java.nio.file.Files.readString(intent));
        assertEquals(800, Denars.balance(Denars.pouch(seller)));
        assertEquals(5, player.getInventory().getItem(0).getAmount());
        server.getPluginManager().assertEventNotFired(MarketSaleEvent.class);
    }

    @Test
    void freshSalesAndUnknownFreshnessHaveAccurateMessagesAndRounding() {
        var trade = trade("sale", 2);
        var holder = openTrade();
        try (var freshness = mockStatic(FreshnessLookup.class)) {
            freshness.when(() -> FreshnessLookup.stepId(any())).thenReturn("fresh", "custom");
            for (String step : List.of("fresh", "custom")) {
                trade.setDemand(1.234);
                player.getInventory().setItem(0, new ItemStack(Material.CARROT, 2));
                manager.tradeClick(player, holder, tagged("trade_id", "sale"), null);
                assertEquals("§aSold 2 for 0.99d", player.nextMessage());
                if (step.equals("custom")) {
                    assertEquals("§7  Base: 0.99d", player.nextMessage());
                    assertEquals("§7  Custom x2: 100%", player.nextMessage());
                }
                assertNull(player.nextMessage());
            }
        }
        assertEquals(198, Denars.balance(Denars.pouch(seller)));
    }

    @Test
    void aSellerWithNoCurrentCharacterIsRefusedAndKeepsTheGoods() {
        trade("sale", 2);
        money.stop(player);
        player.getInventory().setItem(0, new ItemStack(Material.CARROT, 3));
        manager.tradeClick(player, openTrade(), tagged("trade_id", "sale"), null);
        assertEquals("§cChoose a character before you sell.", player.nextMessage());
        assertNull(player.nextMessage());
        assertEquals(3, player.getInventory().getItem(0).getAmount());
        assertEquals(0, Denars.balance(Denars.pouch(seller)));
        server.getPluginManager().assertEventNotFired(MarketSaleEvent.class);
    }

    @Test
    void aRefusedOrUnavailablePaymentTakesNothing() {
        trade("sale", 2);
        var holder = openTrade();
        String pouch = Denars.pouch(seller);
        assertEquals(Result.APPLIED, Denars.transfer("fund", "staff:test", Denars.MINT, pouch, Long.MAX_VALUE - 1));
        player.getInventory().setItem(0, new ItemStack(Material.CARROT, 3));
        manager.tradeClick(player, holder, tagged("trade_id", "sale"), null);
        assertEquals("§cThe market could not pay you, so it kept nothing.", player.nextMessage());
        assertEquals(Long.MAX_VALUE - 1, Denars.balance(pouch));
        TestMoney.close();
        manager.tradeClick(player, holder, tagged("trade_id", "sale"), null);
        assertEquals("§cThe market could not pay you, so it kept nothing.", player.nextMessage());
        assertNull(player.nextMessage());
        assertEquals(3, player.getInventory().getItem(0).getAmount());
        server.getPluginManager().assertEventNotFired(MarketSaleEvent.class);
    }

    @Test
    void anEmptyRemovalCannotPayEvenIfTheInventoryCheckPreviouslySucceeded() {
        trade("sale", 2);
        var holder = openTrade();
        try (var inventory = mockStatic(InventoryUtils.class)) {
            inventory.when(() -> InventoryUtils.hasEnough(player, "v.carrot", 2)).thenReturn(true);
            inventory.when(() -> InventoryUtils.planTake(player, "v.carrot", 2)).thenReturn(new SaleTake());
            manager.tradeClick(player, holder, tagged("trade_id", "sale"), null);
            assertEquals("§cThe market could not take the items for this trade.", player.nextMessage());
            assertEquals(0, Denars.balance(Denars.pouch(seller)));
            server.getPluginManager().assertEventNotFired(MarketSaleEvent.class);
        }
    }

    @Test
    void externallySuppliedFreshnessCountsIgnoreZeroEntriesAndDisplayUnnamedStagesSafely() {
        trade("sale", 2);
        var holder = openTrade();
        var take = new SaleTake();
        // SaleTake exposes a mutable map; tolerate a caller that supplied legacy unnamed stages.
        take.getCounts().put("fresh", 0);
        take.getCounts().put("unknown", 0);
        take.getCounts().put(null, 2);
        try (var inventory = mockStatic(InventoryUtils.class)) {
            inventory.when(() -> InventoryUtils.hasEnough(player, "v.carrot", 2)).thenReturn(true);
            inventory.when(() -> InventoryUtils.planTake(player, "v.carrot", 2)).thenReturn(take);
            manager.tradeClick(player, holder, tagged("trade_id", "sale"), null);
            assertEquals(800, Denars.balance(Denars.pouch(seller)));
            assertEquals("§aSold 2 for 8d", player.nextMessage());
            assertEquals("§7  Base: 8d", player.nextMessage());
            assertEquals("§7  Fresh x2: 100%", player.nextMessage());
            assertNull(player.nextMessage());
        }
    }

    @Test
    void missingGoodsInvalidAmountsDeletedOffersAndDecorationsDoNotPay() {
        trade("sale", 2);
        var holder = openTrade();
        manager.tradeClick(player, holder, new ItemStack(Material.STONE), null);
        manager.tradeClick(player, holder, tagged("trade_id", "missing"), null);
        assertEquals("§cThe market no longer buys this.", player.nextMessage());
        manager.tradeClick(player, holder, tagged("trade_id", "sale"), null);
        assertEquals("§cYou don't have enough items for this trade.", player.nextMessage());
        for (double amount : new double[] {0, -1, 0.5, Double.NaN, Double.POSITIVE_INFINITY}) {
            trade("sale", amount);
            player.getInventory().setItem(0, new ItemStack(Material.CARROT, 3));
            manager.tradeClick(player, holder, tagged("trade_id", "sale"), null);
            assertEquals(3, player.getInventory().getItem(0).getAmount());
            assertEquals("§cYou don't have enough items for this trade.", player.nextMessage());
        }
        assertEquals(0, Denars.balance(Denars.pouch(seller)));
    }

    @Test
    void menuClicksAreCancelledAndNavigateOnlyWithinTheTopInventory() {
        manager.inv = mock(InventoryManager.class);
        var holder = openTrade();
        var event = clickEvent(new ItemStack(Material.BARRIER), player.getOpenInventory().getTopInventory());
        manager.click(event);
        verify(event).setCancelled(true);
        verify(manager.inv).categoryView(null, player);
        reset(manager.inv);
        event = clickEvent(new ItemStack(Material.BARRIER), player.getInventory());
        manager.click(event);
        verify(event).setCancelled(true);
        verifyNoInteractions(manager.inv);
        manager.click(clickEvent(null, player.getOpenInventory().getTopInventory()));
        manager.click(clickEvent(new ItemStack(Material.AIR), player.getOpenInventory().getTopInventory()));
        verifyNoInteractions(manager.inv);
        player.openInventory(server.createInventory(new MBHolder(MBGUI.CATEGORY, "none"), 54));
        manager.click(clickEvent(tagged("category_id", "unknown"), player.getOpenInventory().getTopInventory()));
        verify(manager.inv).tradeView(null, player, category);
        reset(manager.inv);
        manager.categoryClick(player, new MBHolder(MBGUI.CATEGORY, "none"), new ItemStack(Material.STONE), null);
        verifyNoInteractions(manager.inv);
        player.openInventory(server.createInventory(null, 9));
        event = clickEvent(new ItemStack(Material.STONE), player.getInventory());
        manager.click(event);
        verify(event, never()).setCancelled(true);
    }

    @Test
    void opensOnlyMatchingRightClickedMarketBlocks() {
        manager.inv = mock(InventoryManager.class);
        var blocks = mock(BlockAPI.class);
        var blockChecker = mock(BlockChecker.class);
        when(blocks.getChecker()).thenReturn(blockChecker);
        tlibs.when(TLibs::getBlockAPI).thenReturn(blocks);
        Cache.marketBlock = "v.stone";
        var block = mock(Block.class);
        var event = mock(PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getClickedBlock()).thenReturn(block);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
        manager.openMarket(event);
        verifyNoInteractions(blockChecker, manager.inv);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        manager.openMarket(event);
        verifyNoInteractions(manager.inv);
        when(blockChecker.checkBlock(block, "v.stone")).thenReturn(true);
        manager.openMarket(event);
        verify(event).setCancelled(true);
        verify(manager.inv).categoryView(null, player);
    }

    @Test
    void recoveryRunsOnScheduleAndReschedulingCancelsThePreviousTimer() {
        var trade = trade("sale", 2);
        trade.setDemand(1);
        openTrade();
        var scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
        var first = mock(org.bukkit.scheduler.BukkitTask.class);
        var replacement = mock(org.bukkit.scheduler.BukkitTask.class);
        var callbacks = new ArrayList<Runnable>();
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenAnswer(call -> {
            callbacks.add(call.getArgument(1));
            return callbacks.size() == 1 ? first : replacement;
        });
        try (var bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            manager.start();
            verify(scheduler).runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(288000L));
            callbacks.getFirst().run();
            assertTrue(trade.getDemand() >= 2);
            Cache.demandRecoveryHours = 2.0 / 72000;
            manager.rescheduleDemandCycle();
            verify(first).cancel();
            verify(scheduler).runTaskTimer(eq(plugin), any(Runnable.class), eq(2L), eq(2L));
            double before = trade.getDemand();
            callbacks.getLast().run();
            assertTrue(trade.getDemand() > before);
        }
        player.closeInventory();
        manager.update();
        player.openInventory(server.createInventory(new MBHolder(MBGUI.CATEGORY, "none"), 54));
        manager.update();
    }

    private Trade trade(String id, double amount) {
        var trade = new Trade(id, category, 10, 20, 0, "v.carrot", 8, amount, 0);
        TradeLoader.getTrades().put(id, trade);
        return trade;
    }

    private MBHolder openTrade() {
        var holder = new MBHolder(MBGUI.TRADE, "unknown");
        player.openInventory(server.createInventory(holder, 54));
        return holder;
    }

    private ItemStack tagged(String key, String value) {
        var item = new ItemStack(Material.CARROT);
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    private InventoryClickEvent clickEvent(ItemStack item, Inventory clicked) {
        var event = mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(player.getOpenInventory());
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getCurrentItem()).thenReturn(item);
        when(event.getClickedInventory()).thenReturn(clicked);
        return event;
    }
}
