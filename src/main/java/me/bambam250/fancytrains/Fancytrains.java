package me.bambam250.fancytrains;

import me.bambam250.fancytrains.command.LineCommand;
import me.bambam250.fancytrains.command.LineTabCompleter;
import me.bambam250.fancytrains.command.StationCommand;
import me.bambam250.fancytrains.command.StationTabCompleter;
import me.bambam250.fancytrains.config.ConfigManager;
import me.bambam250.fancytrains.objects.Line;
import me.bambam250.fancytrains.objects.Station;
import me.bambam250.fancytrains.station.StationGUIManager;
import me.bambam250.fancytrains.station.StationManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.List;

public final class Fancytrains extends JavaPlugin {
    public ConfigManager configManager;
    public StationManager stationManager;
    public StationGUIManager GUIManager;

    private List<Station> stations;
    private List<Line> lines;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        configManager = new ConfigManager(this);
        stationManager = new StationManager(this);
        GUIManager = new StationGUIManager(this);

        getCommand("station").setExecutor(new StationCommand(this));
        getCommand("station").setTabCompleter(new StationTabCompleter());
        getCommand("line").setExecutor(new LineCommand(this));
        getCommand("line").setTabCompleter(new LineTabCompleter());

        getServer().getPluginManager().registerEvents(stationManager, this);

        getLogger().info("FancyTrains Plugin enabled!");
    }

    @Override
    public void onDisable() {
        stationManager.saveStations();
        getLogger().info("FancyTrains Plugin disabled!");
    }

    /**
     * Formats a time in ticks to a mm:ss or ss string.
     *
     * @param ticks The time in ticks.
     * @return The formatted time string.
     */
    public static String formatTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds %= 60;
        return minutes > 0 ? String.format("%02d:%02d", minutes, seconds) : String.format("%02d", seconds);
    }
}
