package net.tfminecraft.marketblock.manager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.marketblock.MarketBlock;
import net.tfminecraft.marketblock.database.TradeDatabase;
import net.tfminecraft.marketblock.loader.TradeLoader;
import net.tfminecraft.marketblock.manager.commands.ConversationManager;
import net.tfminecraft.marketblock.manager.commands.MarketblockConversation;
import net.tfminecraft.marketblock.trade.Trade;

public class CommandManager implements CommandExecutor {

    public String cmd1 = "marketblock";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase(cmd1)) return true;

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /marketblock add OR /marketblock delete <id>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if(sender instanceof Player) {
                JavaPlugin.getPlugin(MarketBlock.class).reload((Player) sender);
            } else {
                JavaPlugin.getPlugin(MarketBlock.class).reload();
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args[0].equalsIgnoreCase("add")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir()) {
                player.sendMessage("§cYou must hold an item in your hand to add a trade.");
                return true;
            }

            MarketblockConversation convo = new MarketblockConversation(player, item);
            ConversationManager.startConversation(player, convo);
            player.sendMessage("§aPlease enter the trade ID in chat:");
            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                player.sendMessage("§cPlease specify the trade ID to delete.");
                return true;
            }

            String id = args[1];
            Trade trade = TradeLoader.getTradeById(id);
            if (trade == null) {
                player.sendMessage("§cNo trade found with ID: " + id);
                return true;
            }
            trade.getCategory().removeTrade(trade);
            TradeDatabase.deleteTrade(trade);
            TradeLoader.getTrades().remove(id);

            player.sendMessage("§aTrade with ID '" + id + "' has been deleted.");
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            if (args.length < 2) {
                player.sendMessage("§cPlease specify the trade ID to delete.");
                return true;
            }

            String id = args[1];
            Trade trade = TradeLoader.getTradeById(id);
            if (trade == null) {
                player.sendMessage("§cNo trade found with ID: " + id);
                return true;
            }
            trade.resetDemand();

            player.sendMessage("§aTrade with ID '" + id + "' has been reset.");
            return true;
        }

        if (args[0].equalsIgnoreCase("resetall")) {
            int amount = 0;
            for(Trade trade : TradeLoader.getTrades().values()) {
                trade.resetDemand();
                amount++;
            }
            player.sendMessage("§aReset "+amount+" trades.");
            return true;
        }

        return false;
    }
}
