package me.bambam250.fancytrains.config;

import me.bambam250.fancytrains.Fancytrains;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigManager {
    private final Fancytrains plugin;
    private File ftFile;
    private FileConfiguration ftConfig;

    public ConfigManager(Fancytrains plugin) {
        this.plugin = plugin;
        createStationsFile();
    }

    private void createStationsFile() {
        ftFile = new File(plugin.getDataFolder(), "stations.yml");
        if (!ftFile.exists()) {
            try {
                ftFile.createNewFile();
                ftConfig = YamlConfiguration.loadConfiguration(ftFile);
                ftConfig.createSection("stations");
                ftConfig.createSection("lines");
                ftConfig.save(ftFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create stations.yml", e);
            }
        } else {
            ftConfig = YamlConfiguration.loadConfiguration(ftFile);
        }
    }

    public void saveStations() {
        try {
            ftConfig.save(ftFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save stations.yml", e);
        }
    }

    public FileConfiguration getFtConfig() {
        return ftConfig;
    }
}
