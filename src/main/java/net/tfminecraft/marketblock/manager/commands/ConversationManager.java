package net.tfminecraft.marketblock.manager.commands;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class ConversationManager {

    private static final HashMap<UUID, MarketblockConversation> conversations = new HashMap<>();

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

