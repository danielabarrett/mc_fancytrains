package me.bambam250.fancytrains;

import me.bambam250.fancytrains.command.LineCommand;
import me.bambam250.fancytrains.command.LineTabCompleter;
import me.bambam250.fancytrains.command.StationCommand;
import me.bambam250.fancytrains.command.StationTabCompleter;
import me.bambam250.fancytrains.config.ConfigManager;
import me.bambam250.fancytrains.station.StationManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Fancytrains extends JavaPlugin {
    public ConfigManager configManager;
    public StationManager stationManager;

    @Override
    public void onEnable() {
        // Create plugin folder
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        configManager = new ConfigManager(this);
        stationManager = new StationManager(this);

        // Register Command Classes
        getCommand("station").setExecutor(new StationCommand(this));
        getCommand("station").setTabCompleter(new StationTabCompleter());
        getCommand("line").setExecutor(new LineCommand(this));
        getCommand("line").setTabCompleter(new LineTabCompleter());

        // Register events
        getServer().getPluginManager().registerEvents(stationManager, this);

        getLogger().info("FancyTrains Plugin enabled!");
    }

    @Override
    public void onDisable() {
        stationManager.saveStations();
        getLogger().info("FancyTrains Plugin disabled!");
    }

    public static String formatTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds %= 60;
        return minutes > 0 ? String.format("%02d:%02d", minutes, seconds) : String.format("%02d", seconds);
    }

}
