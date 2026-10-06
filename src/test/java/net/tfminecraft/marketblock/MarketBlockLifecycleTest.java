package net.tfminecraft.marketblock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import net.tfminecraft.marketblock.database.TradeDatabase;
import net.tfminecraft.marketblock.loader.CategoryLoader;
import net.tfminecraft.marketblock.loader.ConfigLoader;
import net.tfminecraft.marketblock.loader.TradeLoader;
import net.tfminecraft.marketblock.manager.TradeManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class MarketBlockLifecycleTest {
    @Test
    void enableCreatesDefaultsRegistersListenersAndStartsDemandReloadPreservesFiles() throws Exception {
        var previous = MarketBlock.plugin;
        MockBukkit.mock();
        try (var config = mockConstruction(ConfigLoader.class);
             var categories = mockConstruction(CategoryLoader.class);
             var trades = mockConstruction(TradeLoader.class);
             var managers = mockConstruction(TradeManager.class);
             var database = mockStatic(TradeDatabase.class)) {
            var plugin = MockBukkit.load(MarketBlock.class);
            assertSame(plugin, MarketBlock.plugin);
            assertNotNull(plugin.getCommand("marketblock").getExecutor());
            assertNotNull(plugin.getCommand("marketblock").getTabCompleter());
            assertTrue(Files.isDirectory(plugin.getDataFolder().toPath().resolve("data")));
            for (String name : List.of("config.yml", "categories.yml", "trades.yml")) {
                assertTrue(Files.isRegularFile(plugin.getDataFolder().toPath().resolve(name)));
            }
            verify(managers.constructed().getFirst()).start();
            var marker = plugin.getDataFolder().toPath().resolve("config.yml");
            Files.writeString(marker, "market-block: preserved\n");
            plugin.createFolders();
            plugin.createConfigs();
            assertEquals("market-block: preserved\n", Files.readString(marker));
            Player player = mock(Player.class);
            plugin.reload(player);
            verify(player).sendMessage("§a[MarketBlock] §eReloading...");
            verify(player).sendMessage("§a[MarketBlock] §eReloaded!");
            verify(config.constructed().getFirst(), times(2)).load(marker.toFile());
            verify(categories.constructed().getFirst(), times(2)).load(plugin.getDataFolder().toPath().resolve("categories.yml").toFile());
            verify(trades.constructed().getFirst(), times(2)).loadTrades();
            verify(managers.constructed().getFirst()).rescheduleDemandCycle();
            plugin.onDisable();
            database.verify(TradeDatabase::saveDemand);
            try (var paths = Files.walk(plugin.getDataFolder().toPath())) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
            plugin.createFolders();
            assertTrue(Files.isDirectory(plugin.getDataFolder().toPath().resolve("data")));
            MockBukkit.unmock();
        } finally {
            if (MockBukkit.isMocked()) MockBukkit.unmock();
            MarketBlock.plugin = previous;
        }
    }
}
