package net.tfminecraft.marketblock;

import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.util.logging.Logger;
import net.tfminecraft.marketblock.loader.CategoryLoader;
import net.tfminecraft.marketblock.loader.TradeLoader;
import net.tfminecraft.tlibs.TLibs;
import net.tfminecraft.tlibs.objects.api.ItemAPI;
import net.tfminecraft.tlibs.objects.api.subapi.ItemChecker;
import net.tfminecraft.tlibs.objects.api.subapi.ItemCreator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedStatic;

/** Real Bukkit inventory/YAML behavior, with only the external TLibs boundary mocked. */
public abstract class MarketTestSupport {
    @TempDir protected Path directory;
    protected ServerMock server;
    protected MarketBlock plugin;
    protected Logger logger;
    protected ItemCreator creator;
    protected ItemChecker checker;
    private MarketBlock previousPlugin;
    protected MockedStatic<TLibs> tlibs;

    @BeforeEach
    void createServer() {
        server = MockBukkit.mock();
        previousPlugin = MarketBlock.plugin;
        plugin = mock(MarketBlock.class);
        when(plugin.getName()).thenReturn("MarketBlock");
        when(plugin.namespace()).thenReturn("marketblock");
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getServer()).thenReturn(server);
        when(plugin.isEnabled()).thenReturn(true);
        logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        MarketBlock.plugin = plugin;
        var api = mock(ItemAPI.class);
        creator = mock(ItemCreator.class);
        checker = mock(ItemChecker.class);
        when(api.getCreator()).thenReturn(creator);
        when(api.getChecker()).thenReturn(checker);
        tlibs = mockStatic(TLibs.class);
        tlibs.when(TLibs::getItemAPI).thenReturn(api);
        CategoryLoader.categories.clear();
        TradeLoader.getTrades().clear();
        Cache.slots.clear();
        Cache.freshnessPrice.clear();
        Cache.demandRecoveryHours = 4.0;
    }

    @AfterEach
    void closeServer() {
        tlibs.close();
        MockBukkit.unmock();
        MarketBlock.plugin = previousPlugin;
        CategoryLoader.categories.clear();
        TradeLoader.getTrades().clear();
        Cache.slots.clear();
        Cache.freshnessPrice.clear();
        Cache.demandRecoveryHours = 4.0;
    }
}
