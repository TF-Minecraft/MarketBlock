package net.tfminecraft.marketblock.manager.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerQuitEvent.QuitReason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.tfminecraft.marketblock.manager.FakePlayer;

@SuppressWarnings("deprecation")
class ChatListenerTest {
    private final ChatListener listener = new ChatListener();
    private final FakePlayer tester = new FakePlayer(true);

    @AfterEach
    void endConversation() {
        ConversationManager.endConversation(tester.player);
    }

    @Test
    void typingCancelEndsTheConversation() {
        start(0);

        AsyncPlayerChatEvent event = chat("cancel");

        assertTrue(event.isCancelled());
        assertNull(ConversationManager.getConversation(tester.player));
        assertEquals("§eTrade creation cancelled.", tester.lastMessage());
    }

    @Test
    void numbersThatAreNotFiniteAreRejected() {
        MarketblockConversation convo = start(1);

        for (String text : new String[] { "NaN", "Infinity", "0" }) {
            chat(text);
            assertEquals(1, convo.getStep(), text);
        }
        chat("20");

        assertEquals(2, convo.getStep());
        assertEquals(20.0, convo.getDemandLimit());
    }

    @Test
    void anInfiniteRestingPriceIsRejected() {
        MarketblockConversation convo = start(4);

        chat("Infinity");

        assertEquals(4, convo.getStep());
        assertEquals("§cInvalid number. Enter a resting price above 0.", tester.lastMessage());
    }

    @Test
    void losingPermissionEndsTheConversationAndLeavesChatAlone() {
        start(0);
        tester.admin = false;

        AsyncPlayerChatEvent event = chat("hello");

        assertFalse(event.isCancelled());
        assertNull(ConversationManager.getConversation(tester.player));
    }

    @Test
    void quittingEndsTheConversation() {
        start(0);

        listener.onQuit(new PlayerQuitEvent(tester.player, Component.text("left"), QuitReason.DISCONNECTED));

        assertNull(ConversationManager.getConversation(tester.player));
    }

    private MarketblockConversation start(int step) {
        MarketblockConversation convo = new MarketblockConversation(tester.player, null);
        for (int i = 0; i < step; i++) convo.nextStep();
        ConversationManager.startConversation(tester.player, convo);
        assertSame(convo, ConversationManager.getConversation(tester.player));
        return convo;
    }

    private AsyncPlayerChatEvent chat(String message) {
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, tester.player, message, new HashSet<>());
        listener.onPlayerChat(event);
        return event;
    }
}
