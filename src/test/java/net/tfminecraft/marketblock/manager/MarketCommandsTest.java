package net.tfminecraft.marketblock.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import net.tfminecraft.marketblock.MarketBlock;
import net.tfminecraft.marketblock.MarketTestSupport;
import net.tfminecraft.marketblock.database.TradeDatabase;
import net.tfminecraft.marketblock.loader.TradeLoader;
import net.tfminecraft.marketblock.manager.commands.ConversationManager;
import net.tfminecraft.marketblock.manager.commands.TabCompletion;
import net.tfminecraft.marketblock.trade.Category;
import net.tfminecraft.marketblock.trade.Trade;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

class MarketCommandsTest extends MarketTestSupport {
    @Test
    void permissionsUsageReloadAndPlayerOnlyBranchesKeepTheirContracts() {
        var manager = new CommandManager();
        var command = mock(Command.class);
        var console = mock(CommandSender.class);
        when(command.getName()).thenReturn("other");
        assertTrue(manager.onCommand(console, command, "", new String[0]));
        verifyNoInteractions(console);
        when(command.getName()).thenReturn("marketblock");
        when(console.hasPermission(CommandManager.ADMIN_PERMISSION)).thenReturn(true);
        assertTrue(manager.onCommand(console, command, "", new String[0]));
        verify(console).sendMessage(startsWith("§cUsage:"));
        assertTrue(manager.onCommand(console, command, "", new String[] {"add"}));
        verify(console).sendMessage("Only players can use this command.");
        var player = server.addPlayer();
        player.setOp(true);
        try (var plugins = mockStatic(JavaPlugin.class)) {
            plugins.when(() -> JavaPlugin.getPlugin(MarketBlock.class)).thenReturn(plugin);
            assertTrue(manager.onCommand(console, command, "", new String[] {"reload"}));
            assertTrue(manager.onCommand(player, command, "", new String[] {"reload"}));
            verify(plugin).reload();
            verify(plugin).reload(player);
        }
        assertFalse(manager.onCommand(player, command, "", new String[] {"unknown"}));
        assertTrue(manager.onCommand(player, command, "", new String[] {"add"}));
        assertTrue(player.nextMessage().contains("hold an item"));
        player.getInventory().setItemInMainHand(new ItemStack(Material.STONE, 4));
        assertTrue(manager.onCommand(player, command, "", new String[] {"add"}));
        var conversation = ConversationManager.getConversation(player);
        assertNotNull(conversation);
        assertEquals(4, conversation.getItem().getAmount());
        assertTrue(player.nextMessage().contains("trade ID"));
        ConversationManager.endConversation(player);
    }

    @Test
    void resetsAndDeletionKeepTheTradeIndexCategoryAndDiskInSync() {
        var manager = new CommandManager();
        var command = mock(Command.class);
        when(command.getName()).thenReturn("marketblock");
        var player = server.addPlayer();
        player.setOp(true);
        var category = new Category();
        var trade = new Trade("oak", category, 2, 20, 1, "v.oak_log", 2, 64, 0);
        TradeLoader.getTrades().put("oak", trade);
        for (String sub : List.of("reset", "delete")) {
            assertTrue(manager.onCommand(player, command, "", new String[] {sub}));
            assertTrue(player.nextMessage().contains("specify the trade ID"));
            assertTrue(manager.onCommand(player, command, "", new String[] {sub, "missing"}));
            assertTrue(player.nextMessage().contains("No trade found"));
        }
        assertTrue(manager.onCommand(player, command, "", new String[] {"reset", "oak"}));
        assertEquals(10, trade.getDemand());
        assertTrue(player.nextMessage().contains("has been reset"));
        trade.setDemand(1);
        assertTrue(manager.onCommand(player, command, "", new String[] {"resetall"}));
        assertEquals(10, trade.getDemand());
        assertEquals("§aReset 1 trades.", player.nextMessage());
        try (var database = mockStatic(TradeDatabase.class)) {
            assertTrue(manager.onCommand(player, command, "", new String[] {"delete", "oak"}));
            database.verify(() -> TradeDatabase.deleteTrade(trade));
            assertNull(TradeLoader.getTradeById("oak"));
            assertTrue(category.getTrades().isEmpty());
            assertTrue(player.nextMessage().contains("has been deleted"));
        }
    }

    @Test
    void completionRespectsCommandPermissionPrefixAndTradeArguments() {
        var completion = new TabCompletion();
        var command = mock(Command.class);
        var sender = mock(CommandSender.class);
        when(command.getName()).thenReturn("other");
        assertTrue(completion.onTabComplete(sender, command, "", new String[] {""}).isEmpty());
        when(command.getName()).thenReturn("marketblock");
        assertTrue(completion.onTabComplete(sender, command, "", new String[] {""}).isEmpty());
        when(sender.hasPermission(CommandManager.ADMIN_PERMISSION)).thenReturn(true);
        assertEquals(List.of("reload", "reset", "resetall"), completion.onTabComplete(sender, command, "", new String[] {"RE"}));
        TradeLoader.getTrades().put("oak", new Trade("oak", new Category(), 2, 20, 1, "v.oak_log", 2, 64, 0));
        assertEquals(List.of("oak"), completion.onTabComplete(sender, command, "", new String[] {"delete", ""}));
        assertEquals(List.of("oak"), completion.onTabComplete(sender, command, "", new String[] {"reset", ""}));
        assertTrue(completion.onTabComplete(sender, command, "", new String[] {"add", ""}).isEmpty());
        assertTrue(completion.onTabComplete(sender, command, "", new String[0]).isEmpty());
        assertTrue(completion.onTabComplete(sender, command, "", new String[] {"reset", "oak", "extra"}).isEmpty());
    }
}
