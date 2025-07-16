package me.bambam250.fancytrains.objects;

import me.bambam250.fancytrains.Fancytrains;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Station {
    Fancytrains plugin = Fancytrains.getPlugin(Fancytrains.class);

    private String name;
    private String displayName;
    private Location location;
    private Line trainLine;
    private List<Station> connections;
    private Entity stationMaster;


    /**
     * Constructor for creating a new Station.
     * @param name the unique name of the station
     * @param displayName the display name of the station
     * @param location the location of the station in the world
     * @param trainLine the name of the train line this station belongs to
     * @param stationMasterUUID the UUID of the station master NPC, or null if not yet spawned
     */
    public Station(String name, String displayName, Location location, String trainLine, UUID stationMasterUUID) {
        this.name = name;
        this.displayName = displayName;
        this.location = location;
        this.trainLine = plugin.getLine(trainLine);
        this.connections = new ArrayList<>();

        this.stationMaster = location.getWorld().getEntity(stationMasterUUID);
        if (this.stationMaster == null) {
            spawnStationMaster();
        }

        plugin.configManager.ftConfig.set("stations." + name + ".location.world", location.getWorld().getName());
        plugin.configManager.ftConfig.set("stations." + name + ".location.x", location.getX());
        plugin.configManager.ftConfig.set("stations." + name + ".location.y", location.getY());
        plugin.configManager.ftConfig.set("stations." + name + ".location.z", location.getZ());
        plugin.configManager.ftConfig.set("stations." + name + ".location.yaw", location.getYaw());
        plugin.configManager.ftConfig.set("stations." + name + ".location.pitch", location.getPitch());
        plugin.configManager.ftConfig.set("stations." + name + ".display-name", displayName);
        plugin.configManager.ftConfig.set("stations." + name + ".line", trainLine);
//        plugin.configManager.ftConfig.set("stations." + name + ".npc-spawned", false);
        plugin.configManager.ftConfig.set("stations." + name + ".connections", Arrays.asList());

        plugin.configManager.saveStations();
    }

    /**
     * Constructor for loading an existing Station from config.
     * @param name the unique name of the station
     */
    public Station(String name) {
        this.name = name;
        this.displayName = plugin.configManager.ftConfig.getString("stations." + name + ".display-name", name);
        this.location = new Location(Bukkit.getWorld(plugin.configManager.ftConfig.getString("stations." + name + ".location.world")),
                plugin.configManager.ftConfig.getDouble("stations." + name + ".location.x"),
                plugin.configManager.ftConfig.getDouble("stations." + name + ".location.y"),
                plugin.configManager.ftConfig.getDouble("stations." + name + ".location.z"),
                (float) plugin.configManager.ftConfig.getDouble("stations." + name + ".location.yaw"),
                (float) plugin.configManager.ftConfig.getDouble("stations." + name + ".location.pitch"));
        this.trainLine = plugin.getLine(plugin.configManager.ftConfig.getString("stations." + name + ".line", "default"));
        this.connections = new ArrayList<>();
    }

    public void spawnStationMaster() {
        if (stationMaster != null) {
            stationMaster.remove();
        }

        // Spawn a new station master NPC at the station location
        // This is a placeholder, actual NPC spawning logic will depend on the NPC library used
        stationMaster = Bukkit.getWorld(location.getWorld().getName()).spawnEntity(location, org.bukkit.entity.EntityType.VILLAGER);
        stationMaster.setCustomName(displayName);
        stationMaster.setCustomNameVisible(true);

        plugin.configManager.ftConfig.set("stations." + name + ".npc-spawned", true);
        plugin.configManager.ftConfig.set("stations." + name + ".station-master-uuid", stationMaster.getUniqueId().toString());
        plugin.configManager.saveStations();
    }

    /**
     * Load connections from the config file.
     * This method populates the connections list with Station objects based on the stored connection names.
     * This should be called after loading all stations from the config.
     */
    public void loadConnections() {
        for (String conn : plugin.configManager.ftConfig.getStringList("stations." + name + ".connections")) {
            Station station = plugin.getStation(conn);
            if (station != null) {
                connections.add(station);
            }
        }
    }


    // Getters and Setters

    public String getName() {
        return name;
    }

    // Cannot set name after creation

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        plugin.configManager.ftConfig.set("stations." + name + ".display-name", displayName);
        plugin.configManager.saveStations();
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
        plugin.configManager.ftConfig.set("stations." + name + ".location.world", location.getWorld().getName());
        plugin.configManager.ftConfig.set("stations." + name + ".location.x", location.getX());
        plugin.configManager.ftConfig.set("stations." + name + ".location.y", location.getY());
        plugin.configManager.ftConfig.set("stations." + name + ".location.z", location.getZ());
        plugin.configManager.ftConfig.set("stations." + name + ".location.yaw", location.getYaw());
        plugin.configManager.ftConfig.set("stations." + name + ".location.pitch", location.getPitch());
        plugin.configManager.saveStations();
    }

    public Line getTrainLine() {
        return trainLine;
    }

    public void setTrainLine(Line line) {
        this.trainLine.removeStation(this);
        this.trainLine = line;
        this.trainLine.addStation(this);
        plugin.configManager.ftConfig.set("stations." + name + ".line", line.getName());
        plugin.configManager.saveStations();
    }

    public List<Station> getConnections() {
        return connections;
    }

    public void addConnection(Station station) {
        if (!connections.contains(station)) {
            connections.add(station);
            plugin.configManager.ftConfig.set("stations." + name + ".connections", connections.stream().map(Station::getName).toList());
            plugin.configManager.saveStations();
        }
    }

    public void removeConnection(Station station) {
        if (connections.contains(station)) {
            connections.remove(station);
            plugin.configManager.ftConfig.set("stations." + name + ".connections", connections.stream().map(Station::getName).toList());
            plugin.configManager.saveStations();
        }
    }

}
