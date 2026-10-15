package net.tfminecraft.marketblock.manager.commands;

import net.tfminecraft.marketblock.loader.TradeLoader;
import net.tfminecraft.marketblock.manager.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TabCompletion implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!command.getName().equalsIgnoreCase("marketblock")) return completions;
        if (!sender.hasPermission(CommandManager.ADMIN_PERMISSION)) return completions;

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String sub : new String[] { "add", "delete", "reload", "reset", "resetall" }) {
                if (!(sender instanceof Player) && !sub.equals("reload")) continue;
                if (sub.startsWith(prefix)) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (sender instanceof Player && args.length == 2
                && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("reset"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (String id : TradeLoader.getTrades().keySet()) {
                if (id.toLowerCase(Locale.ROOT).startsWith(prefix)) completions.add(id);
            }
        }

        return completions;
    }
}

