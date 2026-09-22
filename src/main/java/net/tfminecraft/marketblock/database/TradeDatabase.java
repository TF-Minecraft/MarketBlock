package net.tfminecraft.marketblock.database;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.marketblock.MarketBlock;
import net.tfminecraft.marketblock.loader.TradeLoader;
import net.tfminecraft.marketblock.trade.Trade;

public class TradeDatabase {

    private static File tradesFile() {
        return new File(MarketBlock.plugin.getDataFolder(), "trades.yml");
    }

    private static File demandFile() {
        File data = new File(MarketBlock.plugin.getDataFolder(), "data");
        if (!data.exists()) {
            data.mkdirs();
        }
        return new File(data, "demand.yml");
    }

    public static Map<String, Double> loadDemand() {
        Map<String, Double> out = new HashMap<>();
        File file = demandFile();
        if (!file.exists()) {
            return out;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            out.put(id, yaml.getDouble(id));
        }
        return out;
    }

    public static void saveDemand() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Trade trade : TradeLoader.getTrades().values()) {
            yaml.set(trade.getId(), trade.getDemand());
        }
        try {
            yaml.save(demandFile());
        } catch (IOException e) {
            Logger log = MarketBlock.plugin.getLogger();
            log.warning("Could not save demand.yml: " + e.getMessage());
        }
    }

    public static void addTradeDefinition(Trade trade) {
        File file = tradesFile();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String id = trade.getId();
        yaml.set(id + ".item", trade.getItemString());
        if (trade.getIconString() != null && !trade.getIconString().isBlank()) {
            yaml.set(id + ".icon", trade.getIconString());
        }
        yaml.set(id + ".amount", trade.getAmount());
        yaml.set(id + ".demand-limit", trade.getDemandLimit());
        yaml.set(id + ".category", trade.getCategory().getId());
        yaml.set(id + ".price-change", trade.getPriceChange());
        yaml.set(id + ".resting-price", trade.getItemRestingPrice());
        yaml.set(id + ".group", trade.getGroup());
        try {
            yaml.save(file);
        } catch (IOException e) {
            MarketBlock.plugin.getLogger().warning("Could not update trades.yml: " + e.getMessage());
        }
    }

    public static void deleteTrade(Trade trade) {
        File file = tradesFile();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set(trade.getId(), null);
        try {
            yaml.save(file);
        } catch (IOException e) {
            MarketBlock.plugin.getLogger().warning("Could not update trades.yml: " + e.getMessage());
        }
        File demand = demandFile();
        if (demand.exists()) {
            YamlConfiguration d = YamlConfiguration.loadConfiguration(demand);
            d.set(trade.getId(), null);
            try {
                d.save(demand);
            } catch (IOException e) {
                MarketBlock.plugin.getLogger().warning("Could not update demand.yml: " + e.getMessage());
            }
        }
    }
}
