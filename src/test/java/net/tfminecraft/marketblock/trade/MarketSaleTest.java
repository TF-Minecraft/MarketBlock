package net.tfminecraft.marketblock.trade;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Optional;
import net.tfminecraft.denareconomy.api.Denars;
import net.tfminecraft.denareconomy.api.Result;
import net.tfminecraft.marketblock.TestMoney;
import net.tfminecraft.marketblock.util.FreshnessLookup;
import net.tfminecraft.marketblock.util.InventoryUtils;
import net.tfminecraft.tlibs.TLibs;
import net.tfminecraft.tlibs.objects.api.ItemAPI;
import net.tfminecraft.tlibs.objects.api.subapi.ItemChecker;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.MockedStatic;

class MarketSaleTest {
    @TempDir Path folder;
    private PlayerMock player;
    private TestMoney money;
    private MockedStatic<TLibs> tlibs;
    private MockedStatic<FreshnessLookup> freshness;

    @BeforeEach
    void setUp() {
        player = MockBukkit.mock().addPlayer();
        money = TestMoney.open(folder);
        var checker = mock(ItemChecker.class);
        var api = mock(ItemAPI.class);
        when(api.getChecker()).thenReturn(checker);
        when(checker.checkItemWithPath(any(ItemStack.class), eq("v.carrot")))
                .thenAnswer(call -> call.<ItemStack>getArgument(0).getType() == Material.CARROT);
        tlibs = mockStatic(TLibs.class);
        tlibs.when(TLibs::getItemAPI).thenReturn(api);
        freshness = mockStatic(FreshnessLookup.class);
        freshness.when(() -> FreshnessLookup.stepId(any(ItemStack.class))).thenReturn("fresh");
    }

    @AfterEach
    void tearDown() {
        freshness.close();
        tlibs.close();
        TestMoney.close();
        MockBukkit.unmock();
    }

    @Test
    void settlingASaleAgainPaysAndTakesTheGoodsOnlyOnce() throws Exception {
        var seller = money.play(player);
        player.getInventory().setItem(0, new ItemStack(Material.CARROT, 5));
        var sale = new MarketSale(folder.resolve("sales"), seller, InventoryUtils.planTake(player, "v.carrot", 2), 250);
        assertEquals(5, player.getInventory().getItem(0).getAmount());

        assertEquals(Result.APPLIED, sale.settle());
        assertEquals(Result.APPLIED, sale.settle());

        assertEquals(250, Denars.balance(Denars.pouch(seller)));
        assertEquals(-250, Denars.balance(Denars.MINT));
        assertEquals(3, player.getInventory().getItem(0).getAmount());
        assertEquals(Optional.of(Result.APPLIED), Denars.status(sale.opId()));
        assertTrue(sale.opId().startsWith("market:"));
    }

    @Test
    void aSaleRefusedByTheLedgerTakesNothingAndCanBeRetried() throws Exception {
        var seller = money.play(player);
        player.getInventory().setItem(0, new ItemStack(Material.CARROT, 5));
        var sale = new MarketSale(folder.resolve("sales"), seller, InventoryUtils.planTake(player, "v.carrot", 2), 250);
        TestMoney.close();

        assertEquals(Result.UNAVAILABLE, sale.settle());
        assertEquals(5, player.getInventory().getItem(0).getAmount());

        TestMoney.open(folder);
        assertEquals(Result.APPLIED, sale.settle());
        assertEquals(250, Denars.balance(Denars.pouch(seller)));
        assertEquals(3, player.getInventory().getItem(0).getAmount());
    }
    @Test
    void paidRetryUsesStoredResultAfterTaxChangesAndRefusesChangedGoods() throws Exception {
        var seller = money.play(player);
        player.getInventory().setItem(0, new ItemStack(Material.CARROT, 5));
        var sale = new MarketSale(folder.resolve("sales"), seller,
                InventoryUtils.planTake(player, "v.carrot", 2), 250);
        try (var denars = mockStatic(Denars.class, CALLS_REAL_METHODS)) {
            denars.when(() -> Denars.earn(anyString(), any(), anyString(), anyLong(), eq(true)))
                    .thenAnswer(call -> {
                        assertTrue(java.nio.file.Files.readString(folder.resolve("sales/" + seller.ownerId() + ".properties"))
                                .contains("grossCents=250"));
                        call.callRealMethod();
                        throw new IllegalStateException("interrupted");
                    });
            assertThrows(IllegalStateException.class, sale::settle);
        }
        TestMoney.taxAt(20);
        player.getInventory().setItem(0, new ItemStack(Material.CARROT, 4));
        assertThrows(IllegalStateException.class, sale::settle);
        assertEquals(250, Denars.balance(Denars.pouch(seller)));
        player.getInventory().setItem(0, new ItemStack(Material.CARROT, 5));
        assertEquals(Result.APPLIED, sale.settle());
        assertEquals(250, Denars.balance(Denars.pouch(seller)));
        assertEquals(3, player.getInventory().getItem(0).getAmount());
    }


    @Test void waitingForFactionTaxDoesNotCreateAnAmbiguousSale() throws Exception {
        var seller=money.play(player);
        MockBukkit.createMockPlugin("SimpleFactions");
        player.getInventory().setItem(0,new ItemStack(Material.CARROT,5));
        var sale=new MarketSale(folder.resolve("sales"),seller,InventoryUtils.planTake(player,"v.carrot",2),250);
        assertEquals(Result.UNAVAILABLE,sale.settle());
        assertFalse(java.nio.file.Files.exists(folder.resolve("sales")));
        assertEquals(5,player.getInventory().getItem(0).getAmount());
        TestMoney.taxAt(20);
        assertEquals(Result.APPLIED,sale.settle());
        assertEquals(200,Denars.balance(Denars.pouch(seller)));
    }
}
