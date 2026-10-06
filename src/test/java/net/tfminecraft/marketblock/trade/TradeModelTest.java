package net.tfminecraft.marketblock.trade;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.LinkedHashMap;
import java.util.Set;

import net.tfminecraft.marketblock.loader.CategoryLoader;
import net.tfminecraft.marketblock.manager.commands.MarketblockConversation;
import net.tfminecraft.tlibs.TLibs;
import net.tfminecraft.tlibs.objects.api.ItemAPI;
import net.tfminecraft.tlibs.objects.api.subapi.ItemChecker;
import net.tfminecraft.tlibs.objects.api.subapi.ItemCreator;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.MockedStatic;

class TradeModelTest {
    private LinkedHashMap<String, Category> previousCategories;
    private MockedStatic<TLibs> tlibs;
    private ItemCreator creator;
    private ItemChecker checker;
    private Category unknown;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        previousCategories = CategoryLoader.categories;
        CategoryLoader.categories = new LinkedHashMap<>();
        unknown = new Category();
        CategoryLoader.categories.put("unknown", unknown);

        ItemAPI api = mock(ItemAPI.class);
        creator = mock(ItemCreator.class);
        checker = mock(ItemChecker.class);
        when(api.getCreator()).thenReturn(creator);
        when(api.getChecker()).thenReturn(checker);
        tlibs = mockStatic(TLibs.class);
        tlibs.when(TLibs::getItemAPI).thenReturn(api);
    }

    @AfterEach
    void tearDown() {
        if (tlibs != null) tlibs.close();
        CategoryLoader.categories = previousCategories;
        MockBukkit.unmock();
    }

    @Test
    void categoryReadsItsNameColorAndIconAndFormatsTheTradeMenuTitle() throws Exception {
        Category category = new Category("ores", yaml("""
            name: '#123456Ores'
            item: v.iron_ingot
            """));
        ItemStack icon = new ItemStack(Material.IRON_INGOT);
        when(creator.getItemFromPath("v.iron_ingot")).thenReturn(icon);

        assertEquals("ores", category.getId());
        assertEquals("#123456", category.getColorHex());
        assertEquals("§x§1§2§3§4§5§6Ores", category.getName());
        assertEquals("§x§1§2§3§4§5§6Ores Trades", category.getTradesTitle());
        assertEquals("v.iron_ingot", category.getItemString());
        assertSame(icon, category.getItem());
        assertTrue(category.getTrades().isEmpty());
    }

    @Test
    void categoryDefaultsAndPlainNamesRemainUsable() throws Exception {
        Category configuredDefault = new Category("empty", yaml("{}"));
        assertEquals("Unknown Category", ChatColor.stripColor(configuredDefault.getName()));
        assertEquals("#ffffff", configuredDefault.getColorHex());
        assertEquals("v.gray_dye", configuredDefault.getItemString());
        assertEquals("unknown", unknown.getId());
        assertEquals("§fUnknown Category", unknown.getName());
        assertEquals("Unknown Category Trades", ChatColor.stripColor(unknown.getTradesTitle()));
        assertEquals("#ffffff", unknown.getColorHex());
        assertEquals("v.gray_dye", unknown.getItemString());

        assertEquals("#ffffff", new Category("short", yaml("name: Ore")).getColorHex());
        assertEquals("#ffffff", new Category("plain", yaml("name: Plain category")).getColorHex());
        assertEquals("#abcdef", new Category("spaced", yaml("name: '  #abcdefOres'")).getColorHex());
    }

    @Test
    void tradeAutomaticallyJoinsItsCategoryWithoutDuplicateMembership() {
        Trade trade = trade(unknown, 8, 20, 1);
        assertEquals(Set.of(trade), unknown.getTrades());
        unknown.addTrade(trade);
        assertEquals(Set.of(trade), unknown.getTrades());
        unknown.removeTrade(trade);
        assertTrue(unknown.getTrades().isEmpty());
        unknown.removeTrade(trade);
        assertTrue(unknown.getTrades().isEmpty());
    }

    @Test
    void yamlRoundTripPreservesEveryConfiguredTradeFieldAndResolvesItsCategory() throws Exception {
        Category category = new Category("ores", yaml("name: Ores"));
        CategoryLoader.categories.put("ores", category);
        YamlConfiguration original = yaml("""
            category: ores
            demand-limit: 30
            price-change: 2.5
            item: v.iron_ingot
            icon: v.iron_block
            resting-price: 1.25
            amount: 16
            group: 4
            """);
        Trade trade = Trade.fromYaml("iron", yaml(original.saveToString()), 7.5);
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        ItemStack icon = new ItemStack(Material.IRON_BLOCK);
        when(creator.getItemFromPath("v.iron_ingot")).thenReturn(item);
        when(creator.getItemFromPath("v.iron_block")).thenReturn(icon);

        assertEquals("iron", trade.getId());
        assertSame(category, trade.getCategory());
        assertEquals(Set.of(trade), category.getTrades());
        assertEquals(7.5, trade.getDemand());
        assertEquals(30, trade.getDemandLimit());
        assertEquals(2.5, trade.getPriceChange());
        assertEquals("v.iron_ingot", trade.getItemString());
        assertEquals("v.iron_block", trade.getIconString());
        assertEquals(1.25, trade.getItemRestingPrice());
        assertEquals(16, trade.getAmount());
        assertEquals(4, trade.getGroup());
        assertSame(item, trade.getItem());
        assertSame(icon, trade.getIconItem());
    }

    @Test
    void omittedTradeSettingsUseDefaultsAndTheTradeItemAsItsIcon() throws Exception {
        Trade trade = Trade.fromYaml("fallback", yaml("{}"), 100);
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        when(creator.getItemFromPath("v.gray_dye")).thenReturn(item);

        assertSame(unknown, trade.getCategory());
        assertEquals(20, trade.getDemandLimit());
        assertEquals(20, trade.getDemand());
        assertEquals(1, trade.getPriceChange());
        assertEquals("v.gray_dye", trade.getItemString());
        assertNull(trade.getIconString());
        assertSame(item, trade.getIconItem());
        assertEquals(1, trade.getItemRestingPrice());
        assertEquals(64, trade.getAmount());
        assertEquals(0, trade.getGroup());
    }

    @Test
    void blankIconsAndUnknownCategoriesFallBackWithoutLosingTheTradeItem() throws Exception {
        ItemStack item = new ItemStack(Material.OAK_LOG);
        when(creator.getItemFromPath("v.oak_log")).thenReturn(item);
        for (String icon : new String[] {"", "   "}) {
            YamlConfiguration config = yaml("category: nonexistent\nitem: v.oak_log\n");
            config.set("icon", icon);
            Trade trade = Trade.fromYaml("oak", config, 10);
            assertSame(unknown, trade.getCategory());
            assertNull(trade.getIconString());
            assertSame(item, trade.getIconItem());
        }
    }

    @Test
    void demandIsClampedAtConstructionAndWhenUpdatedAndResetsToItsMidpoint() {
        Trade trade = trade(unknown, -1, 20, 0);
        assertEquals(1, trade.getDemand());
        trade.setDemand(30);
        assertEquals(20, trade.getDemand());
        trade.setDemand(7.5);
        assertEquals(7.5, trade.getDemand());
        trade.sell();
        assertEquals(7.5, trade.getDemand(), "zero price change must not reduce demand");
        trade.setDemand(-10);
        assertEquals(1, trade.getDemand());
        trade.resetDemand();
        assertEquals(10, trade.getDemand());
        trade.setDemand(20);
        trade.demand();
        assertEquals(20, trade.getDemand());

        Trade minimum = trade(unknown, 10, -5, 0);
        assertEquals(1, minimum.getDemandLimit());
        assertEquals(1, minimum.getDemand());
    }

    @Test
    void resettingAOneUnitDemandLimitDoesNotDropBelowTheMinimum() {
        Trade trade = trade(unknown, 1, 1, 0);
        trade.resetDemand();
        assertEquals(1, trade.getDemand());
    }

    @Test
    void conversationBuildsATradeFromTheHeldStackAndSelectedSettings() {
        ItemStack item = new ItemStack(Material.OAK_LOG, 16);
        when(checker.getAsStringPath(item)).thenReturn("v.oak_log");
        MarketblockConversation conversation = conversation(item, 40);

        Trade trade = new Trade(conversation);

        assertEquals("oak", trade.getId());
        assertSame(unknown, trade.getCategory());
        assertTrue(unknown.getTrades().contains(trade));
        assertEquals(40, trade.getDemandLimit());
        assertEquals(20, trade.getDemand());
        assertEquals("v.oak_log", trade.getItemString());
        assertEquals(16, trade.getAmount());
        assertEquals(1.5, trade.getPriceChange());
        assertEquals(2.25, trade.getItemRestingPrice());
        assertEquals(3, trade.getGroup());
    }

    @Test
    void conversationWithTheSmallestAllowedLimitStartsAtTheDemandMinimum() {
        ItemStack item = new ItemStack(Material.OAK_LOG);
        when(checker.getAsStringPath(item)).thenReturn("v.oak_log");
        Trade trade = new Trade(conversation(item, 1));
        assertEquals(1, trade.getDemand());
        assertEquals(1, trade.getDemandLimit());
    }

    private MarketblockConversation conversation(ItemStack item, double limit) {
        MarketblockConversation conversation = new MarketblockConversation(null, item);
        conversation.setId("oak");
        conversation.setCategory(unknown);
        conversation.setDemandLimit(limit);
        conversation.setPriceChange(1.5);
        conversation.setRestingPrice(2.25);
        conversation.setGroup(3);
        return conversation;
    }

    private static Trade trade(Category category, double demand, double limit, double priceChange) {
        return new Trade("oak", category, demand, limit, priceChange, "v.oak_log", 2, 64, 0);
    }

    private static YamlConfiguration yaml(String source) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(source);
        return config;
    }
}
