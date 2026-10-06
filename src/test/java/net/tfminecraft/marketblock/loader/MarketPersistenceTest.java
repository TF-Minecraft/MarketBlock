package net.tfminecraft.marketblock.loader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.util.Map;
import net.tfminecraft.marketblock.MarketTestSupport;
import net.tfminecraft.marketblock.database.TradeDatabase;
import net.tfminecraft.marketblock.trade.Category;
import net.tfminecraft.marketblock.trade.Trade;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MarketPersistenceTest extends MarketTestSupport {
    @Test
    void categoriesLoadInOrderAndUnknownIdsUseTheFallback() throws Exception {
        var file = directory.resolve("categories.yml");
        Files.writeString(file, "wood:\n  name: '#aabbccWood'\n  item: v.oak_log\n");
        var loader = new CategoryLoader();
        loader.load(file.toFile());
        assertEquals(2, CategoryLoader.get().size());
        assertEquals("wood", CategoryLoader.getAsList().getFirst().getId());
        assertSame(CategoryLoader.getDefaultCategory(), CategoryLoader.getByString("missing"));
        assertEquals("#aabbcc", CategoryLoader.getByString("wood").getColorHex());
        Files.writeString(file, "unknown:\n  name: Custom fallback\n");
        loader.load(file.toFile());
        assertEquals(1, CategoryLoader.get().size());
        assertEquals("Custom fallback", CategoryLoader.getDefaultCategory().getName());
        loader.load(directory.resolve("missing.yml").toFile());
        assertEquals(1, CategoryLoader.get().size());
        assertEquals("unknown", CategoryLoader.getDefaultCategory().getId());
    }

    @Test
    void demandSurvivesDiskRoundTripAndLiveValuesTakePrecedenceOnReload() throws Exception {
        Category category = new Category();
        CategoryLoader.categories.put("unknown", category);
        assertTrue(TradeDatabase.loadDemand().isEmpty());
        Trade live = trade("live", category, 4);
        TradeLoader.add(live);
        Trade saved = trade("saved", category, 7);
        TradeLoader.add(saved);
        TradeDatabase.saveDemand();
        assertEquals(Map.of("live", 4.0, "saved", 7.0), TradeDatabase.loadDemand());
        live.setDemand(9);
        TradeLoader.getTrades().remove("saved");
        var definitions = directory.resolve("trades.yml");
        Files.writeString(definitions, Files.readString(definitions) + "new:\n  item: v.stone\n  demand-limit: 12\nscalar: ignored\n");
        new TradeLoader().loadTrades();
        assertEquals(9, TradeLoader.getTradeById("live").getDemand());
        assertEquals(7, TradeLoader.getTradeById("saved").getDemand());
        assertEquals(6, TradeLoader.getTradeById("new").getDemand());
        assertNull(TradeLoader.getTradeById("scalar"));
        TradeDatabase.deleteTrade(TradeLoader.getTradeById("saved"));
        assertFalse(TradeDatabase.loadDemand().containsKey("saved"));
        assertFalse(YamlConfiguration.loadConfiguration(definitions.toFile()).contains("saved"));
        TradeDatabase.deleteTrade(TradeLoader.getTradeById("new"));
        Files.delete(directory.resolve("data/demand.yml"));
        TradeDatabase.deleteTrade(live);
        Files.delete(definitions);
        new TradeLoader().loadTrades();
        assertTrue(TradeLoader.getTrades().isEmpty());
        verify(logger).warning(startsWith("trades.yml does not exist:"));
    }

    @Test
    void definitionPreservesOptionalIconAndAllTradeTerms() throws Exception {
        Category category = new Category();
        CategoryLoader.categories.put("unknown", category);
        var config = new YamlConfiguration();
        var section = config.createSection("sale");
        section.set("icon", "v.apple");
        section.set("item", "v.stone");
        section.set("amount", 16);
        section.set("demand-limit", 40);
        section.set("resting-price", 2.5);
        section.set("price-change", 0.5);
        section.set("group", 3);
        Trade trade = Trade.fromYaml("sale", section, 8);
        TradeDatabase.addTradeDefinition(trade);
        var saved = YamlConfiguration.loadConfiguration(directory.resolve("trades.yml").toFile());
        assertEquals("v.apple", saved.getString("sale.icon"));
        assertEquals("v.stone", saved.getString("sale.item"));
        assertEquals(16, saved.getDouble("sale.amount"));
        assertEquals(40, saved.getDouble("sale.demand-limit"));
        assertEquals(2.5, saved.getDouble("sale.resting-price"));
        assertEquals(0.5, saved.getDouble("sale.price-change"));
        assertEquals(3, saved.getInt("sale.group"));
        assertEquals("unknown", saved.getString("sale.category"));
    }

    @Test
    void failedWritesAreReportedWithoutDestroyingOtherFiles() throws Exception {
        Category category = new Category();
        Trade trade = trade("oak", category, 3);
        Files.createDirectory(directory.resolve("trades.yml"));
        Files.createDirectories(directory.resolve("data/demand.yml"));
        TradeDatabase.addTradeDefinition(trade);
        TradeDatabase.saveDemand();
        TradeDatabase.deleteTrade(trade);
        verify(logger, times(2)).warning(startsWith("Could not update trades.yml:"));
        verify(logger).warning(startsWith("Could not save demand.yml:"));
        verify(logger).warning(startsWith("Could not update demand.yml:"));
        assertTrue(Files.isDirectory(directory.resolve("trades.yml")));
        assertTrue(Files.isDirectory(directory.resolve("data/demand.yml")));
    }

    private Trade trade(String id, Category category, double demand) {
        return new Trade(id, category, demand, 20, 1, "v.oak_log", 2, 64, 0);
    }
}
