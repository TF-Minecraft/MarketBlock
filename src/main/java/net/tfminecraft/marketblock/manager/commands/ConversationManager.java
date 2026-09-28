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
}

