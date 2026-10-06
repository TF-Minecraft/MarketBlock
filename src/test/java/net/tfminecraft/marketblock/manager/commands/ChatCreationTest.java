package net.tfminecraft.marketblock.manager.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tfminecraft.marketblock.MarketTestSupport;
import net.tfminecraft.marketblock.loader.CategoryLoader;
import net.tfminecraft.marketblock.loader.TradeLoader;
import net.tfminecraft.marketblock.manager.CommandManager;
import net.tfminecraft.marketblock.trade.Category;
import net.tfminecraft.marketblock.trade.Trade;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

@SuppressWarnings("deprecation")
class ChatCreationTest extends MarketTestSupport {
    private final ChatListener listener = new ChatListener();
    private PlayerMock player;
    private Category wood;

    @BeforeEach
    void setUpConversation() {
        player = server.addPlayer();
        player.setOp(true);
        YamlConfiguration config = new YamlConfiguration();
        config.set("name", "Wood");
        wood = new Category("wood", config);
        CategoryLoader.categories.put("wood", wood);
        CategoryLoader.categories.put("unknown", new Category());
        when(checker.getAsStringPath(any(ItemStack.class))).thenReturn("v.oak_log");
    }

    @AfterEach
    void endConversation() {
        if (player != null) ConversationManager.endConversation(player);
    }

    @Test
    void chatWithoutAConversationIsNotConsumed() {
        assertFalse(chat("hello everyone").isCancelled());
        assertTrue(messages().isEmpty());
        verifyNoInteractions(checker);
    }

    @Test
    void cancellationConsumesTheAnswerAndDoesNotCreateATrade() {
        start(new ItemStack(Material.OAK_LOG, 12));
        assertTrue(chat("  CaNcEl  ").isCancelled());
        assertNull(ConversationManager.getConversation(player));
        assertEquals(List.of("§eTrade creation cancelled."), messages());
        server.getScheduler().performOneTick();
        assertTrue(TradeLoader.getTrades().isEmpty());
        assertFalse(Files.exists(directory.resolve("trades.yml")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", ".", "oak.logs", "two words", "oak\tlogs"})
    void idsThatCannotBeAddressedAsOneYamlKeyAreRejected(String id) {
        MarketblockConversation conversation = start(new ItemStack(Material.OAK_LOG, 12));

        assertTrue(chat(id).isCancelled());

        assertEquals(0, conversation.getStep());
        assertNull(conversation.getId());
        assertSame(conversation, ConversationManager.getConversation(player));
        assertTrue(messages().getLast().contains("ID"));
        assertFalse(Files.exists(directory.resolve("trades.yml")));
    }

    @Test
    void surroundingWhitespaceIsRemovedBeforeDuplicateIdChecks() {
        Trade existing = existing("oak", 3);
        TradeLoader.getTrades().put("oak", existing);
        MarketblockConversation conversation = start(new ItemStack(Material.OAK_LOG, 12));

        chat("  oak  ");

        assertEquals(0, conversation.getStep());
        assertNull(conversation.getId());
        assertSame(existing, TradeLoader.getTradeById("oak"));
        assertEquals(List.of("§cThat id is already taken"), messages());
    }

    @Test
    void invalidNumbersKeepTheCurrentQuestionUntilAUsableAnswerArrives() {
        MarketblockConversation conversation = start(new ItemStack(Material.OAK_LOG, 12));
        chat("oak");
        for (String invalid : List.of("NaN", "Infinity", "0", "-1", "not a number")) {
            chat(invalid);
            assertEquals(1, conversation.getStep());
        }
        chat("40");
        assertEquals(2, conversation.getStep());
        assertEquals(40, conversation.getDemandLimit());
        for (String invalid : List.of("1.5", "2147483648", "group")) {
            chat(invalid);
            assertEquals(2, conversation.getStep());
        }
        chat("4");
        assertEquals(3, conversation.getStep());
        assertEquals(4, conversation.getGroup());
        for (String invalid : List.of("-1", "NaN", "Infinity", "price")) {
            chat(invalid);
            assertEquals(3, conversation.getStep());
        }
        chat("0");
        assertEquals(4, conversation.getStep());
        assertEquals(0, conversation.getPriceChange());
        for (String invalid : List.of("0", "-1", "NaN", "Infinity", "price")) {
            chat(invalid);
            assertEquals(4, conversation.getStep());
        }
        chat("2.75");
        assertEquals(5, conversation.getStep());
        assertEquals(2.75, conversation.getRestingPrice());
        assertTrue(messages().contains("§cInvalid number. Please enter a valid group."));
        assertFalse(Files.exists(directory.resolve("trades.yml")));
    }

    @Test
    void completeConversationSavesOnTheMainThreadAndSurvivesYamlReload() {
        MarketblockConversation conversation = answerThroughPrice("  oak-log:bundle  ", 12);

        assertTrue(chat("wood").isCancelled());
        assertNull(ConversationManager.getConversation(player));
        assertNull(TradeLoader.getTradeById("oak-log:bundle"));
        assertFalse(Files.exists(directory.resolve("trades.yml")));
        server.getScheduler().performOneTick();

        Trade trade = TradeLoader.getTradeById("oak-log:bundle");
        assertNotNull(trade);
        assertSame(wood, trade.getCategory());
        assertEquals(Set.of(trade), wood.getTrades());
        assertEquals(20, trade.getDemand());
        assertEquals(12, trade.getAmount());
        assertEquals(4, trade.getGroup());
        assertEquals(1.5, trade.getPriceChange());
        assertEquals(2.75, trade.getItemRestingPrice());
        assertEquals("oak-log:bundle", conversation.getId());
        YamlConfiguration saved = saved();
        assertEquals(Set.of("oak-log:bundle"), saved.getKeys(false));
        assertEquals("v.oak_log", saved.getString("oak-log:bundle.item"));
        assertEquals("wood", saved.getString("oak-log:bundle.category"));
        assertEquals(12, saved.getDouble("oak-log:bundle.amount"));
        assertEquals(40, saved.getDouble("oak-log:bundle.demand-limit"));
        assertEquals(4, saved.getInt("oak-log:bundle.group"));
        assertEquals(1.5, saved.getDouble("oak-log:bundle.price-change"));
        assertEquals(2.75, saved.getDouble("oak-log:bundle.resting-price"));
        assertTrue(messages().contains("§aTrade successfully created!"));

        TradeLoader.getTrades().clear();
        new TradeLoader().loadTrades();
        assertEquals("v.oak_log", TradeLoader.getTradeById("oak-log:bundle").getItemString());
        assertEquals(12, TradeLoader.getTradeById("oak-log:bundle").getAmount());
        assertNull(TradeLoader.getTradeById("  oak-log:bundle  "));
    }

    @Test
    void unknownCategoryUsesTheConfiguredFallbackAndWarnsThePlayer() {
        answerThroughPrice("oak", 12);
        chat("missing");
        server.getScheduler().performOneTick();

        assertSame(CategoryLoader.getDefaultCategory(), TradeLoader.getTradeById("oak").getCategory());
        assertEquals("unknown", saved().getString("oak.category"));
        assertTrue(messages().contains("§cWarning, no category found, default selected"));
    }

    @Test
    void missingFallbackCategoryIsRecreatedInsteadOfCrashingTheFinalAnswer() {
        answerThroughPrice("oak", 12);
        CategoryLoader.categories.clear();

        assertDoesNotThrow(() -> chat("missing"));
        assertDoesNotThrow(() -> server.getScheduler().performOneTick());

        Trade trade = TradeLoader.getTradeById("oak");
        assertNotNull(trade);
        assertEquals("unknown", trade.getCategory().getId());
        assertSame(trade.getCategory(), CategoryLoader.getDefaultCategory());
        assertEquals(Set.of(trade), trade.getCategory().getTrades());
    }

    @Test
    void categoryReloadBeforeTheQueuedSaveUsesTheCurrentCategoryObject() {
        answerThroughPrice("oak", 12);
        chat("wood");
        Category replacement = new Category("wood", new YamlConfiguration());
        CategoryLoader.categories.put("wood", replacement);

        server.getScheduler().performOneTick();

        Trade trade = TradeLoader.getTradeById("oak");
        assertSame(replacement, trade.getCategory());
        assertEquals(Set.of(trade), replacement.getTrades());
        assertTrue(wood.getTrades().isEmpty());
    }

    @Test
    void competingCreationBeforeTheQueuedSaveCannotOverwriteAnExistingTrade() throws Exception {
        answerThroughPrice("oak", 12);
        chat("wood");
        Trade winner = existing("oak", 3);
        TradeLoader.add(winner);
        String before = Files.readString(directory.resolve("trades.yml"));

        server.getScheduler().performOneTick();

        assertSame(winner, TradeLoader.getTradeById("oak"));
        assertEquals(before, Files.readString(directory.resolve("trades.yml")));
        assertTrue(messages().contains("§cA trade already exists with that id"));
    }

    @Test
    void itemThatStopsParsingBeforeTheQueuedSaveDoesNotCreateATrade() {
        answerThroughPrice("oak", 12);
        chat("wood");
        when(checker.getAsStringPath(any(ItemStack.class))).thenReturn(null);

        server.getScheduler().performOneTick();

        assertTrue(TradeLoader.getTrades().isEmpty());
        assertTrue(wood.getTrades().isEmpty());
        assertFalse(Files.exists(directory.resolve("trades.yml")));
        assertTrue(messages().contains("§cCould not parse the item you are holding"));
    }

    @Test
    void permissionRevokedAfterTheFinalAnswerPreventsTheQueuedSave() {
        answerThroughPrice("oak", 12);
        chat("wood");
        player.setOp(false);
        assertFalse(player.hasPermission(CommandManager.ADMIN_PERMISSION));

        server.getScheduler().performOneTick();

        assertTrue(TradeLoader.getTrades().isEmpty());
        assertTrue(wood.getTrades().isEmpty());
        assertFalse(Files.exists(directory.resolve("trades.yml")));
    }

    @Test
    void mutatingTheOriginalHeldStackDoesNotChangeTheSavedTradeQuantity() {
        ItemStack held = new ItemStack(Material.OAK_LOG, 12);
        start(held);
        held.setAmount(1);
        chat("oak");
        chat("40");
        chat("4");
        chat("1.5");
        chat("2.75");
        chat("wood");

        server.getScheduler().performOneTick();

        assertEquals(12, TradeLoader.getTradeById("oak").getAmount());
        assertEquals(12, saved().getDouble("oak.amount"));
        assertEquals(1, held.getAmount());
    }

    private MarketblockConversation answerThroughPrice(String id, int amount) {
        MarketblockConversation conversation = start(new ItemStack(Material.OAK_LOG, amount));
        for (String answer : List.of(id, "40", "4", "1.5", "2.75")) {
            assertTrue(chat(answer).isCancelled());
        }
        assertEquals(5, conversation.getStep());
        return conversation;
    }

    private MarketblockConversation start(ItemStack item) {
        MarketblockConversation conversation = new MarketblockConversation(player, item);
        ConversationManager.startConversation(player, conversation);
        return conversation;
    }

    private Trade existing(String id, int amount) {
        return new Trade(id, wood, 10, 20, 1, "v.oak_log", 1, amount, 0);
    }

    private AsyncPlayerChatEvent chat(String message) {
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, message, new HashSet<>());
        listener.onPlayerChat(event);
        return event;
    }

    private List<String> messages() {
        List<String> messages = new ArrayList<>();
        String message;
        while ((message = player.nextMessage()) != null) messages.add(message);
        return messages;
    }

    private YamlConfiguration saved() {
        return YamlConfiguration.loadConfiguration(directory.resolve("trades.yml").toFile());
    }
}
