package net.tfminecraft.marketblock.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.MockedStatic;

import net.tfminecraft.tlibs.TLibs;
import net.tfminecraft.tlibs.objects.api.ItemAPI;
import net.tfminecraft.tlibs.objects.api.subapi.ItemChecker;

class InventoryUtilsTest {
    private static final String PATH = "food.vegetables";

    private PlayerMock player;
    private ItemChecker checker;
    private MockedStatic<TLibs> tlibs;
    private MockedStatic<FreshnessLookup> freshness;

    @BeforeEach
    void setUp() {
        player = MockBukkit.mock().addPlayer();
        checker = mock(ItemChecker.class);
        ItemAPI api = mock(ItemAPI.class);
        when(api.getChecker()).thenReturn(checker);
        when(checker.checkItemWithPath(any(ItemStack.class), eq(PATH))).thenAnswer(invocation -> {
            Material type = invocation.<ItemStack>getArgument(0).getType();
            return type == Material.CARROT || type == Material.POTATO;
        });
        tlibs = mockStatic(TLibs.class);
        tlibs.when(TLibs::getItemAPI).thenReturn(api);
        freshness = mockStatic(FreshnessLookup.class);
        freshness.when(() -> FreshnessLookup.stepId(any(ItemStack.class))).thenAnswer(invocation ->
                invocation.<ItemStack>getArgument(0).getType() == Material.CARROT ? "fresh" : "stale");
    }

    @AfterEach
    void tearDown() {
        if (freshness != null) freshness.close();
        if (tlibs != null) tlibs.close();
        MockBukkit.unmock();
    }

    @Test
    void emptyInventoryHasNoMatchingItems() {
        assertNotNull(new InventoryUtils());
        assertEquals(0, InventoryUtils.getTotalAmount(player, PATH));
        assertFalse(InventoryUtils.hasEnough(player, PATH, 1));
        assertEquals(0, InventoryUtils.removeItems(player, PATH, 1).total());
        verifyNoInteractions(checker);
        freshness.verifyNoInteractions();
    }

    @Test
    void explicitAirStacksDoNotReachItemOrFreshnessLookups() {
        Player owner = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(owner.getInventory()).thenReturn(inventory);
        when(inventory.getContents()).thenReturn(new ItemStack[] {null, new ItemStack(Material.AIR)});

        assertEquals(0, InventoryUtils.getTotalAmount(owner, PATH));
        assertEquals(0, InventoryUtils.removeItems(owner, PATH, 1).total());
        verifyNoInteractions(checker);
        freshness.verifyNoInteractions();
    }

    @Test
    void totalsOnlyMatchingStacksAndChecksTheExactRequiredAmount() {
        put(1, Material.AIR, 1);
        put(2, Material.CARROT, 3);
        put(3, Material.STONE, 64);
        put(4, Material.POTATO, 5);

        assertEquals(8, InventoryUtils.getTotalAmount(player, PATH));
        assertEquals(0, InventoryUtils.getTotalAmount(player, "food.other"));
        assertTrue(InventoryUtils.hasEnough(player, PATH, 1));
        assertTrue(InventoryUtils.hasEnough(player, PATH, 8));
        assertFalse(InventoryUtils.hasEnough(player, PATH, 9));
        assertEquals(64, player.getInventory().getItem(3).getAmount());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -0.0, -1, 0.5, 1.5, Double.NaN,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
    void invalidAmountsCannotAuthorizeATrade(double amount) {
        put(0, Material.CARROT, 10);

        assertFalse(InventoryUtils.hasEnough(player, PATH, amount));
        assertEquals(10, player.getInventory().getItem(0).getAmount());
        verifyNoInteractions(checker);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -0.0, -1, 0.5, 1.5, Double.NaN,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
    void invalidRemovalAmountsLeaveEveryInventoryStackUntouched(double amount) {
        put(0, Material.CARROT, 3);
        put(1, Material.STONE, 7);
        put(2, Material.POTATO, 5);
        ItemStack[] before = snapshot();

        SaleTake taken = InventoryUtils.removeItems(player, PATH, amount);

        assertEquals(Map.of(), taken.getCounts());
        assertArrayEquals(before, player.getInventory().getContents());
        verifyNoInteractions(checker);
        freshness.verifyNoInteractions();
    }

    @Test
    void removesPartOfAStackAndStopsBeforeLaterMatches() {
        put(0, Material.CARROT, 10);
        put(1, Material.POTATO, 4);

        SaleTake taken = InventoryUtils.removeItems(player, PATH, 3);

        assertEquals(Map.of("fresh", 3), taken.getCounts());
        assertEquals(7, player.getInventory().getItem(0).getAmount());
        assertEquals(4, player.getInventory().getItem(1).getAmount());
        assertEquals(11, InventoryUtils.getTotalAmount(player, PATH));
    }

    @Test
    void exactStackRemovalDoesNotTouchTheNextMatchingStack() {
        put(0, Material.CARROT, 3);
        put(1, Material.POTATO, 4);

        SaleTake taken = InventoryUtils.removeItems(player, PATH, 3);

        assertEquals(Map.of("fresh", 3), taken.getCounts());
        assertEquals(0, player.getInventory().getItem(0).getAmount());
        assertEquals(4, player.getInventory().getItem(1).getAmount());
    }

    @Test
    void mixedStackRemovalSkipsEmptyAndUnrelatedItemsAndTracksFreshness() {
        put(1, Material.AIR, 1);
        put(2, Material.STONE, 9);
        put(3, Material.CARROT, 2);
        put(4, Material.POTATO, 5);
        put(5, Material.CARROT, 7);

        SaleTake taken = InventoryUtils.removeItems(player, PATH, 4);

        assertEquals(Map.of("fresh", 2, "stale", 2), taken.getCounts());
        assertEquals(4, taken.total());
        assertEquals(9, player.getInventory().getItem(2).getAmount());
        assertEquals(0, player.getInventory().getItem(3).getAmount());
        assertEquals(3, player.getInventory().getItem(4).getAmount());
        assertEquals(7, player.getInventory().getItem(5).getAmount());
        assertEquals(10, InventoryUtils.getTotalAmount(player, PATH));
    }

    @Test
    void requestingMoreThanAvailableReturnsOnlyTheItemsActuallyRemoved() {
        put(0, Material.CARROT, 2);
        put(2, Material.STONE, 9);
        put(3, Material.POTATO, 3);

        assertFalse(InventoryUtils.hasEnough(player, PATH, 8));
        SaleTake taken = InventoryUtils.removeItems(player, PATH, 8);

        assertEquals(Map.of("fresh", 2, "stale", 3), taken.getCounts());
        assertEquals(5, taken.total());
        assertEquals(0, InventoryUtils.getTotalAmount(player, PATH));
        assertEquals(9, player.getInventory().getItem(2).getAmount());
    }

    @Test
    void removalWithNoMatchingPathLeavesInventoryIntact() {
        put(0, Material.CARROT, 2);
        put(1, Material.POTATO, 3);
        ItemStack[] before = snapshot();

        assertEquals(0, InventoryUtils.removeItems(player, "food.other", 1).total());
        assertArrayEquals(before, player.getInventory().getContents());
        freshness.verifyNoInteractions();
    }

    private void put(int slot, Material material, int amount) {
        player.getInventory().setItem(slot, new ItemStack(material, amount));
    }

    private ItemStack[] snapshot() {
        return Arrays.stream(player.getInventory().getContents())
                .map(item -> item == null ? null : item.clone())
                .toArray(ItemStack[]::new);
    }
}
