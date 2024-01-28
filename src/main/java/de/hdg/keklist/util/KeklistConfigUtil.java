package de.hdg.keklist.util;

import de.hdg.keklist.Keklist;
import org.bukkit.configuration.ConfigurationOptions;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class KeklistConfigUtil {

    private static Keklist plugin;

    public KeklistConfigUtil(Keklist plugin) {
        this.plugin = plugin;
    }

    public void updateConfig() {
        try {
            FileConfiguration oldConfig = plugin.getConfig();
            ConfigurationOptions oldOptions = oldConfig.options();

            YamlConfiguration newConfig = new YamlConfiguration();
            InputStreamReader reader = new InputStreamReader(this.getClass().getResourceAsStream("/config.yml"));

            newConfig.load(reader);
            FileConfigurationOptions newOptions = newConfig.options();

            for (String key : newConfig.getKeys(true)) {
                if (!oldConfig.getKeys(true).contains(key)) {
                    oldConfig.set(key, newConfig.get(key));

                    if(Keklist.isDebug())
                        plugin.getLogger().info("Added new config value: " + key);
                }

                oldConfig.setComments(key, newConfig.getComments(key));
                oldConfig.setInlineComments(key, newConfig.getInlineComments(key));
            }

            oldConfig.save(new File(plugin.getDataFolder(), "config.yml"));
            plugin.reloadConfig();

            plugin.getLogger().info("Config updated to version: " + newConfig.get("config_version") + "!");

            reader.close();
        } catch (IOException | InvalidConfigurationException e) {
           plugin.getLogger().severe("Error while updating config! Please report this to the developer!");
           plugin.getLogger().severe(e.getMessage());
        }
    }
}
