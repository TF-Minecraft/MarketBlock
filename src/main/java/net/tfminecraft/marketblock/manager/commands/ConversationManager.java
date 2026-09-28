package net.tfminecraft.marketblock.manager.commands;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationManager {

    // Read from the async chat thread and written from the main thread.
    private static final Map<UUID, MarketblockConversation> conversations = new ConcurrentHashMap<>();

    public static void startConversation(Player player, MarketblockConversation convo) {
        conversations.put(player.getUniqueId(), convo);
    }

    public static MarketblockConversation getConversation(Player player) {
        return conversations.get(player.getUniqueId());
    }

    public static void endConversation(Player player) {
        conversations.remove(player.getUniqueId());
    }

    /**
     * Ends this exact conversation and reports whether this call did so. Only one caller can win,
     * so a trade is saved only if its final answer arrived before the player quit or cancelled.
     */
    public static boolean finishConversation(Player player, MarketblockConversation convo) {
        return conversations.remove(player.getUniqueId(), convo);
    }
}

