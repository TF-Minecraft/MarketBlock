package net.tfminecraft.marketblock.manager.commands;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import net.tfminecraft.tlibs.TLibs;
import net.tfminecraft.marketblock.loader.CategoryLoader;
import net.tfminecraft.marketblock.loader.TradeLoader;
import net.tfminecraft.marketblock.trade.Category;
import net.tfminecraft.marketblock.trade.Trade;

public class ChatListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        MarketblockConversation convo = ConversationManager.getConversation(player);

        if (convo == null) return;

        event.setCancelled(true);
        String message = event.getMessage();

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
                try {
                    double demandLimit = Double.parseDouble(message);
                    convo.setDemandLimit(demandLimit);
                    player.sendMessage("§aDemand limit set to: " + demandLimit);
                    convo.nextStep();
                    player.sendMessage("§aEnter the group:");
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number. Please enter a valid demand limit.");
                }
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
                try {
                    double priceChange = Double.parseDouble(message);
                    convo.setPriceChange(priceChange);
                    player.sendMessage("§aPrice change set to: " + priceChange);
                    convo.nextStep();
                    player.sendMessage("§aEnter the resting price:");
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number. Please enter a valid price change.");
                }
            }
            case 4 -> {
                try {
                    double restingPrice = Double.parseDouble(message);
                    convo.setRestingPrice(restingPrice);
                    player.sendMessage("§aResting price set to: " + restingPrice);
                    convo.nextStep();
                    player.sendMessage("§aEnter the category:");
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number. Please enter a valid resting price.");
                }
            }
            case 5 -> {
                Category cat = CategoryLoader.getByString(message);
                if (cat == null || cat.getId().equalsIgnoreCase("unknown")) {
                    player.sendMessage("§cWarning, no category found, default selected");
                }
                convo.setCategory(cat);
                player.sendMessage("§aCategory set to: " + cat.getId());
                // Done!
                ConversationManager.endConversation(player);
                if (!saveTrade(player, convo)) return;
                player.sendMessage("§aTrade successfully created!");
            }
        }
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
