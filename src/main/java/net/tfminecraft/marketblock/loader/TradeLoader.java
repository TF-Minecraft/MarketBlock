package net.tfminecraft.marketblock.loader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.marketblock.MarketBlock;
import net.tfminecraft.marketblock.database.TradeDatabase;
import net.tfminecraft.marketblock.trade.Trade;

public class TradeLoader {

    private static final HashMap<String, Trade> trades = new HashMap<>();

    public void loadTrades() {
        Map<String, Double> live = new HashMap<>();
        for (Trade trade : trades.values()) {
            live.put(trade.getId(), trade.getDemand());
        }
        Map<String, Double> saved = TradeDatabase.loadDemand();
        trades.clear();

        Logger log = MarketBlock.plugin.getLogger();
        File file = new File(MarketBlock.plugin.getDataFolder(), "trades.yml");
        if (!file.exists()) {
            log.warning("trades.yml does not exist: " + file.getAbsolutePath());
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            double demandLimit = Math.max(1, section.getDouble("demand-limit", 20));
            double demand;
            if (live.containsKey(id)) {
                demand = live.get(id);
            } else if (saved.containsKey(id)) {
                demand = saved.get(id);
            } else {
                demand = demandLimit / 2.0;
            }
            Trade trade = Trade.fromYaml(id, section, demand);
            trades.put(trade.getId(), trade);
            log.info("Loaded trade: " + trade.getId());
        }
    }

    public static void add(Trade t) {
        trades.put(t.getId(), t);
        TradeDatabase.addTradeDefinition(t);
    }

    public static Trade getTradeById(String id) {
        return trades.get(id);
    }

    public static HashMap<String, Trade> getTrades() {
        return trades;
    }
}
