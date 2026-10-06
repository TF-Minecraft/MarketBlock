package net.tfminecraft.marketblock;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import net.tfminecraft.marketblock.loader.ConfigLoader;
import net.tfminecraft.marketblock.database.TradeDatabase;
import net.tfminecraft.marketblock.manager.commands.ConversationManager;
import net.tfminecraft.marketblock.manager.commands.MarketblockConversation;
import net.tfminecraft.marketblock.trade.Category;
import net.tfminecraft.marketblock.util.DemandFormatter;
import net.tfminecraft.marketblock.util.PriceCalculator;
import org.bukkit.configuration.ConfigurationSection;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;

class CacheTest extends MarketTestSupport {
    @Test
    void publicUtilityConstructionDoesNotDiscardLiveStateAndConversationRetainsItsPlayer() {
        Cache.freshnessPrice.put("ripe", 0.75);
        assertNotNull(new Cache());
        assertNotNull(new TradeDatabase());
        assertNotNull(new DemandFormatter());
        assertNotNull(new PriceCalculator());
        assertNotNull(new ConversationManager());
        assertEquals(0.75, Cache.freshnessMultiplier("ripe"));
        var player = server.addPlayer();
        assertSame(player, new MarketblockConversation(player, null).getPlayer());
    }

    @Test
    void aProgrammaticCategoryWithANullNameKeepsTheDefaultColour() {
        var config = mock(ConfigurationSection.class);
        var category = new Category("custom", config);
        assertEquals("#ffffff", category.getColorHex());
    }

    @Test
    void freshnessIsCaseInsensitiveRegardlessOfServerLocaleAndUnknownStagesKeepFullValue() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Cache.freshnessPrice.put("ripe", 0.755);
            assertEquals(0.755, Cache.freshnessMultiplier("RIPE"));
            assertEquals(76, Cache.freshnessPercent("RIPE"));
            assertEquals(1, Cache.freshnessMultiplier(null));
            assertEquals(1, Cache.freshnessMultiplier(" "));
            assertEquals(1, Cache.freshnessMultiplier("other"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void missingConfigurationUsesSafeDefaultsAndClearsPreviousSettings() {
        Cache.freshnessPrice.put("obsolete", 10.0);
        new ConfigLoader().load(directory.resolve("missing.yml").toFile());
        assertNull(Cache.marketBlock);
        assertTrue(Cache.slots.isEmpty());
        assertEquals(4, Cache.demandRecoveryHours);
        assertEquals(1, Cache.freshnessMultiplier("fresh"));
        assertEquals(0.5, Cache.freshnessMultiplier("stale"));
        assertEquals(0.1, Cache.freshnessMultiplier("rotten"));
        assertFalse(Cache.freshnessPrice.containsKey("obsolete"));
    }
}
