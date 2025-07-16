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
        // Create plugin folder
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        configManager = new ConfigManager(this);
        stationManager = new StationManager(this);
        GUIManager = new StationGUIManager(this);

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


    public List<Station> getStations() {
        return stations;
    }

    public boolean isStation(String name) {
        for (Station st : stations) {
            if (st.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Station getStation(String name) {
        for (Station st : stations) {
            if (st.getName().equalsIgnoreCase(name)) {
                return st;
            }
        }
        return null;
    }

    public void addStation(Station station) {
        this.stations.add(station);
    }

    public List<Line> getLines() {
        return lines;
    }

    public boolean isLine(String name) {
        for (Line line : lines) {
            if (line.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public Line getLine(String name) {
        for (Line line : lines) {
            if (line.getName().equalsIgnoreCase(name)) {
                return line;
            }
        }
        return null;
    }

    public void addLine(Line line) {
        this.lines.add(line);
    }

}
