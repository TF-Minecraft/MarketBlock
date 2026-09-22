package net.tfminecraft.marketblock.loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.tlibs.interfaces.LoaderInterface;
import net.tfminecraft.marketblock.trade.Category;

public class CategoryLoader implements LoaderInterface{

	public static LinkedHashMap<String, Category> categories = new LinkedHashMap<>();
	
	public static HashMap<String, Category> get(){
		return categories;
	}

	public static List<Category> getAsList() {
		return new ArrayList<>(categories.values());
	}
	
	@Override
	public void load(File configFile) {
		
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
		categories.clear();
		for (String key : config.getKeys(false)) {
			Category o = new Category(key, config.getConfigurationSection(key));
			categories.put(key, o);
		}
		if (!categories.containsKey("unknown")) {
			categories.put("unknown", new Category());
		}
	}

    public static Category getDefaultCategory() {
        return categories.get("unknown");
    }

	public static Category getByString(String id) {
		if(categories.containsKey(id)) return categories.get(id);
		return getDefaultCategory();
	}
}
