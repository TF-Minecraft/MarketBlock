package net.tfminecraft.marketblock;

import java.io.File;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.marketblock.database.TradeDatabase;
import net.tfminecraft.marketblock.loader.CategoryLoader;
import net.tfminecraft.marketblock.loader.ConfigLoader;
import net.tfminecraft.marketblock.loader.TradeLoader;
import net.tfminecraft.marketblock.manager.CommandManager;
import net.tfminecraft.marketblock.manager.TradeManager;
import net.tfminecraft.marketblock.manager.commands.ChatListener;
import net.tfminecraft.marketblock.manager.commands.TabCompletion;

public class MarketBlock extends JavaPlugin{
    public static MarketBlock plugin;

    private final CategoryLoader categoryLoader = new CategoryLoader();
	private final ConfigLoader configLoader = new ConfigLoader();
    private final TradeLoader tradeLoader = new TradeLoader();

	private final TradeManager tradeManager = new TradeManager();
	private final ChatListener chatListener = new ChatListener();

	private final CommandManager commandManager = new CommandManager();

    @Override
	public void onEnable() {
		plugin = this;
		createFolders();
		createConfigs();
		loadConfigs();
		registerListeners();
		startManagers();
	}
	
	@Override
	public void onDisable() {
		TradeDatabase.saveDemand();
	}
	
	public void registerListeners() {
		getServer().getPluginManager().registerEvents(tradeManager, this);
		getServer().getPluginManager().registerEvents(chatListener, this);

		getCommand(commandManager.cmd1).setExecutor(commandManager);
		getCommand(commandManager.cmd1).setTabCompleter(new TabCompletion());
	}

	public void loadConfigs() {
		configLoader.load(new File(getDataFolder(), "config.yml"));
		categoryLoader.load(new File(getDataFolder(), "categories.yml"));
    	tradeLoader.loadTrades();
	}
	public void startManagers() {
		tradeManager.start();
	}
	public void createFolders() {
		if (!getDataFolder().exists()) getDataFolder().mkdir();
		File subFolder = new File(getDataFolder(), "data");
		if(!subFolder.exists()) subFolder.mkdir();
	}
	
	public void createConfigs() {
		String[] files = {
				"config.yml",
				"categories.yml",
				"trades.yml"
				};
		for(String s : files) {
			File newConfigFile = new File(getDataFolder(), s);
	        if (!newConfigFile.exists()) {
	        	newConfigFile.getParentFile().mkdirs();
	            saveResource(s, false);
	        }
		}
	}

	public void reload(Player p) {
		p.sendMessage("§a[MarketBlock] §eReloading...");
		reload();
		p.sendMessage("§a[MarketBlock] §eReloaded!");
	}

	public void reload() {
		loadConfigs();
		tradeManager.rescheduleDemandCycle();
	}
}
