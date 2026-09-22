package net.tfminecraft.marketblock.loader;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.tlibs.interfaces.LoaderInterface;
import net.tfminecraft.marketblock.Cache;

public class ConfigLoader implements LoaderInterface{

	@Override
	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
		
		Cache.marketBlock = config.getString("market-block");
		
		Cache.slots = config.getIntegerList("slots");

		Cache.freshnessPrice.clear();
		Cache.freshnessPrice.put("fresh", config.getDouble("freshness-price.fresh", 1.0));
		Cache.freshnessPrice.put("stale", config.getDouble("freshness-price.stale", 0.5));
		Cache.freshnessPrice.put("rotten", config.getDouble("freshness-price.rotten", 0.1));
	}
}
