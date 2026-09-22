package net.tfminecraft.marketblock.manager.commands;

import net.tfminecraft.marketblock.loader.TradeLoader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class TabCompletion implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!command.getName().equalsIgnoreCase("marketblock")) return completions;

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : new String[] { "add", "delete", "reload", "reset", "resetall" }) {
                if (sub.startsWith(prefix)) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            completions.addAll(TradeLoader.getTrades().keySet());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            completions.addAll(TradeLoader.getTrades().keySet());
        }

        return completions;
    }
}

