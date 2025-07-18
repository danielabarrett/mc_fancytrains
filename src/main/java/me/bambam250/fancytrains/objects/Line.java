package me.bambam250.fancytrains.objects;

import me.bambam250.fancytrains.Fancytrains;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class Line {
    Fancytrains plugin = Fancytrains.getPlugin(Fancytrains.class);
    FileConfiguration ftConfig = plugin.configManager.getFtConfig();

    private String name;
    private String displayName;
    private ChatColor color;
    private Location trainLocation;
    private List<Station> stations;
    private ItemStack flag;

    /**
     * Constructor for creating a new Line.
     * @param name the unique name of the line
     * @param color the color of the line in hex format (e.g., "#FF0000")
     * @param displayName the display name of the line
     */
    public Line(String name, String color, String displayName, Location trainLocation) {
        this.name = name;
        this.color = ChatColor.valueOf(color);
        this.displayName = displayName;
        this.trainLocation = trainLocation;
        this.stations = new ArrayList<>();
        this.flag = new ItemStack(Material.WHITE_BANNER);

        ftConfig.set("lines." + name + ".display-name", displayName);
        ftConfig.set("lines." + name + ".color", color);
        ftConfig.set("lines." + name + ".stations", new ArrayList<String>());
        plugin.configManager.saveStations();;
    }

    public Line(String name) {
        this.name = name;
        this.color = ChatColor.valueOf(ftConfig.getString("lines." + name + ".color", "WHITE"));
        this.displayName = ftConfig.getString("lines." + name + ".display-name", name);
        this.trainLocation = ftConfig.getLocation("lines." + name + ".train-location");
        this.stations = new ArrayList<>();
        this.flag = loadBannerFromConfig("lines." + name + ".flag");
    }

    public String getName() {
        return name;
    }

    // Cannot set name after creation

    public ChatColor getColor() {
        return color;
    }

    public void setColor(ChatColor color) {
        this.color = color;
        ftConfig.set("lines." + name + ".color", color.name());
        plugin.configManager.saveStations();
    }

    public void setColor(String color) {
        this.color = ChatColor.valueOf(color);
        ftConfig.set("lines." + name + ".color", color);
        plugin.configManager.saveStations();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        ftConfig.set("lines." + name + ".display-name", displayName);
        plugin.configManager.saveStations();
    }

    public Location getTrainLocation() {
        return trainLocation;
    }

    public void setTrainLocation(Location trainLocation) {
        this.trainLocation = trainLocation;
        ftConfig.set("lines." + name + ".train-location", trainLocation);
        plugin.configManager.saveStations();
    }

    public void addStation(Station station) {
        if (!stations.contains(station)) {
            stations.add(station);
        }
        ftConfig.set("lines." + name + ".stations", stations.stream().map(Station::getName).toList());
        plugin.configManager.saveStations();
    }

    public void removeStation(Station station) {
        stations.remove(station);
        ftConfig.set("lines." + name + ".stations", stations.stream().map(Station::getName).toList());
        plugin.configManager.saveStations();
    }

    public List<Station> getStations() {
        return stations;
    }

    @Nullable
    public Station getStation(String name) {
        for (Station station : stations) {
            if (station.getName().equalsIgnoreCase(name)) {
                return station;
            }
        }
        return null;
    }

    public ItemStack getFlag() {
        return flag;
    }

    public boolean setFlag(ItemStack flag) {
        this.flag = flag;
        return saveBannerToConfig(flag, "lines." + name + ".flag");
    }

    /**
     * Save a banner item to config using serialization.
     * @param banner The banner ItemStack to save.
     * @param configPath The path in config (should be under "lines.<line>.<flag>").
     * @return True if saved successfully, false otherwise.
     */
    private boolean saveBannerToConfig(ItemStack banner, String configPath) {
        if (!(banner.getItemMeta() instanceof BannerMeta meta)) return false;
        ftConfig.set(configPath, banner);
        plugin.configManager.saveStations();
        return true;
    }

    /**
     * Load a banner item from config using deserialization.
     * @param configPath The path in config (should be under "lines.<line>.<flag>").
     * @return The loaded banner ItemStack, or null if not found/invalid.
     */
    private ItemStack loadBannerFromConfig(String configPath) {
        Object obj = ftConfig.get(configPath);
        if (obj instanceof ItemStack stack && stack.getItemMeta() instanceof BannerMeta) {
            return stack;
        }
        return new ItemStack(Material.WHITE_BANNER); // Return a default banner if not found
    }

}
