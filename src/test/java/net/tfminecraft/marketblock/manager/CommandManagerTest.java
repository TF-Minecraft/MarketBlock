package net.tfminecraft.marketblock.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.tfminecraft.marketblock.manager.commands.ConversationManager;
import net.tfminecraft.marketblock.manager.commands.TabCompletion;

class CommandManagerTest {
    private final CommandManager commands = new CommandManager();
    private final Command command = new Command("marketblock") {
        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return false;
        }
    };
    private FakePlayer tester;

    @AfterEach
    void endConversation() {
        if (tester != null) ConversationManager.endConversation(tester.player);
    }

    @Test
    void aPlayerWithoutPermissionCannotStartAddingATrade() {
        tester = new FakePlayer(false);

        assertTrue(commands.onCommand(tester.player, command, "marketblock", new String[] { "add" }));

        assertNull(ConversationManager.getConversation(tester.player));
        assertEquals("§cYou do not have permission to use this command.", tester.lastMessage());
    }

    @Test
    void everySubcommandIsRefusedWithoutPermission() {
        // reload would reach JavaPlugin.getPlugin and throw here if the check did not stop it first.
        for (String sub : List.of("add", "delete", "reset", "resetall", "reload")) {
            tester = new FakePlayer(false);

            commands.onCommand(tester.player, command, "marketblock", new String[] { sub, "stone" });

            assertEquals(List.of("§cYou do not have permission to use this command."), tester.messages, sub);
        }
    }

    @Test
    void anAdminGetsPastThePermissionCheck() {
        tester = new FakePlayer(true);

        commands.onCommand(tester.player, command, "marketblock", new String[] { "add" });

        assertEquals("§cYou must hold an item in your hand to add a trade.", tester.lastMessage());
    }

    @Test
    void tabCompletionIsEmptyWithoutPermission() {
        TabCompletion completion = new TabCompletion();

        assertTrue(completion.onTabComplete(new FakePlayer(false).player, command, "marketblock", new String[] { "" }).isEmpty());
        assertEquals(List.of("add", "delete", "reload", "reset", "resetall"),
                completion.onTabComplete(new FakePlayer(true).player, command, "marketblock", new String[] { "" }));
    }

    @Test
    void pluginYmlRequiresTheAdminPermission() throws Exception {
        YamlConfiguration yaml;
        try (Reader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("plugin.yml"), StandardCharsets.UTF_8)) {
            yaml = YamlConfiguration.loadConfiguration(reader);
        }

        assertEquals(CommandManager.ADMIN_PERMISSION, yaml.getString("commands.marketblock.permission"));
        assertNotNull(yaml.getConfigurationSection("permissions." + CommandManager.ADMIN_PERMISSION));
        assertEquals("op", yaml.getString("permissions." + CommandManager.ADMIN_PERMISSION + ".default"));
    }
}
