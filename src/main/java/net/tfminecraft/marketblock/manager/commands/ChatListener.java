package net.tfminecraft.marketblock.manager.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.tfminecraft.tlibs.TLibs;
import net.tfminecraft.marketblock.MarketBlock;
import net.tfminecraft.marketblock.loader.CategoryLoader;
import net.tfminecraft.marketblock.loader.TradeLoader;
import net.tfminecraft.marketblock.manager.CommandManager;
import net.tfminecraft.marketblock.trade.Category;
import net.tfminecraft.marketblock.trade.Trade;

public class ChatListener implements Listener {

    // Retain Bukkit chat-event ordering and String message semantics for existing integrations.
    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        MarketblockConversation convo = ConversationManager.getConversation(player);

        if (convo == null) return;

        answerIfCurrent(event, player, convo);
    }

    /**
     * One answer at a time per conversation. A late message for a conversation that has already
     * been cancelled, finished or replaced is not an answer, so it goes to chat as normal.
     */
    @SuppressWarnings("deprecation")
    void answerIfCurrent(AsyncPlayerChatEvent event, Player player, MarketblockConversation convo) {
        String message = event.getMessage();
        synchronized (convo) {
            if (ConversationManager.getConversation(player) != convo) return;
            // Permission may have been removed since /marketblock add; let the message reach chat.
            if (!player.hasPermission(CommandManager.ADMIN_PERMISSION)) {
                ConversationManager.finishConversation(player, convo);
                return;
            }
            event.setCancelled(true);
            if (TradeInput.isCancel(message)) {
                if (ConversationManager.finishConversation(player, convo)) {
                    player.sendMessage("§eTrade creation cancelled.");
                }
                return;
            }
            answer(player, convo, message);
        }
    }

    private void answer(Player player, MarketblockConversation convo, String message) {
        switch (convo.getStep()) {
            case 0 -> {
                if (TradeLoader.getTradeById(message) != null) {
                    player.sendMessage("§cThat id is already taken");
                    return;
                }
                convo.setId(message);
                player.sendMessage("§aID set to: " + message);
                convo.nextStep();
                player.sendMessage("§aEnter the demand limit:");
            }
            case 1 -> {
                Double demandLimit = TradeInput.demandLimit(message);
                if (demandLimit == null) {
                    player.sendMessage("§cInvalid number. Enter a demand limit of at least 1.");
                    return;
                }
                convo.setDemandLimit(demandLimit);
                player.sendMessage("§aDemand limit set to: " + demandLimit);
                convo.nextStep();
                player.sendMessage("§aEnter the group:");
            }
            case 2 -> {
                try {
                    int group = Integer.parseInt(message);
                    convo.setGroup(group);
                    player.sendMessage("§aGroup set to: " + group);
                    convo.nextStep();
                    player.sendMessage("§aEnter the price change:");
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number. Please enter a valid group.");
                }
            }
            case 3 -> {
                Double priceChange = TradeInput.priceChange(message);
                if (priceChange == null) {
                    player.sendMessage("§cInvalid number. Enter a price change of 0 or more.");
                    return;
                }
                convo.setPriceChange(priceChange);
                player.sendMessage("§aPrice change set to: " + priceChange);
                convo.nextStep();
                player.sendMessage("§aEnter the resting price:");
            }
            case 4 -> {
                Double restingPrice = TradeInput.restingPrice(message);
                if (restingPrice == null) {
                    player.sendMessage("§cInvalid number. Enter a resting price above 0.");
                    return;
                }
                convo.setRestingPrice(restingPrice);
                player.sendMessage("§aResting price set to: " + restingPrice);
                convo.nextStep();
                player.sendMessage("§aEnter the category:");
            }
            case 5 -> {
                // Done! Claim the conversation first, so a quit that got there first stops the save.
                if (!ConversationManager.finishConversation(player, convo)) return;
                Category cat = CategoryLoader.getByString(message);
                if (cat == null || cat.getId().equalsIgnoreCase("unknown")) {
                    player.sendMessage("§cWarning, no category found, default selected");
                }
                convo.setCategory(cat);
                player.sendMessage("§aCategory set to: " + cat.getId());
                // Chat arrives on an async thread; trades and trades.yml belong to the main thread.
                Bukkit.getScheduler().runTask(MarketBlock.plugin, () -> {
                    if (!saveTrade(player, convo)) return;
                    player.sendMessage("§aTrade successfully created!");
                });
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ConversationManager.endConversation(event.getPlayer());
    }

    private boolean saveTrade(Player p, MarketblockConversation convo) {
        String path = TLibs.getItemAPI().getChecker().getAsStringPath(convo.getItem());
        if(path == null) {
            p.sendMessage("§cCould not parse the item you are holding");
            return false;
        }
        if(TradeLoader.getTradeById(convo.getId()) != null) {
            p.sendMessage("§cA trade already exists with that id");
            return false;
        }
        TradeLoader.add(new Trade(convo));
        return true;
    }
}
