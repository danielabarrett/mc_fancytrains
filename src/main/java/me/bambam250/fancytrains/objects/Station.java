package me.bambam250.fancytrains.objects;

import me.bambam250.fancytrains.Fancytrains;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Station {
    Fancytrains plugin = Fancytrains.getPlugin(Fancytrains.class);
    FileConfiguration ftConfig = plugin.configManager.getFtConfig();

    private final String name;
    private String displayName;
    private Location location;
    private Line line;
    private final List<Station> connections;
    private UUID stationMasterUUID;


    /**
     * Constructor for creating a new Station.
     * @param name the unique name of the station
     * @param displayName the display name of the station
     * @param location the location of the station in the world
     * @param line the train line this station belongs to
     */
    public Station(String name, String displayName, Location location, Line line) {
        this.name = name;
        this.displayName = displayName;
        this.location = location;
        this.line = line;
        this.connections = new ArrayList<>();

        spawnStationMaster();

        ftConfig.set("stations." + name + ".location.world", location.getWorld().getName());
        ftConfig.set("stations." + name + ".location.x", location.getX());
        ftConfig.set("stations." + name + ".location.y", location.getY());
        ftConfig.set("stations." + name + ".location.z", location.getZ());
        ftConfig.set("stations." + name + ".location.yaw", location.getYaw());
        ftConfig.set("stations." + name + ".location.pitch", location.getPitch());
        ftConfig.set("stations." + name + ".display-name", displayName);
        ftConfig.set("stations." + name + ".line", line.getName());
        ftConfig.set("stations." + name + ".connections", Arrays.asList());

        plugin.configManager.saveStations();
    }

    /**
     * Constructor for loading an existing Station from config.
     * @param name the unique name of the station
     */
    public Station(String name, Line line) {
        this.name = name;
        this.displayName = ftConfig.getString("stations." + name + ".display-name", name);
        this.location = new Location(Bukkit.getWorld(
                ftConfig.getString("stations." + name + ".location.world")),
                ftConfig.getDouble("stations." + name + ".location.x"),
                ftConfig.getDouble("stations." + name + ".location.y"),
                ftConfig.getDouble("stations." + name + ".location.z"),
                (float) ftConfig.getDouble("stations." + name + ".location.yaw"),
                (float) ftConfig.getDouble("stations." + name + ".location.pitch"));
        this.line = line;
        this.connections = new ArrayList<>();
        String uuidStr = ftConfig.getString("stations." + name + ".npc");
        this.stationMasterUUID = uuidStr != null ? UUID.fromString(uuidStr) : null;
        if (this.stationMasterUUID == null) {
            spawnStationMaster();
        }
    }

    private void spawnStationMaster() {
        World world = location.getWorld();
        if (world == null) return;
        Villager npc = (Villager) world.spawnEntity(location, EntityType.VILLAGER);

        npc.setCustomName(ChatColor.GOLD + "Station Master - " + displayName + " " + line.getColor() + "(" + line.getDisplayName() + ")");
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setCollidable(false);
        npc.setSilent(true);

        this.stationMasterUUID = npc.getUniqueId();

        ftConfig.set("stations." + name + ".npc", npc.getUniqueId().toString());
        plugin.configManager.saveStations();
    }

    public void removeStationMaster() {
        if (stationMasterUUID != null) {
            Entity entity = getStationMaster();
            if (entity != null) {
                entity.remove();
            }
            ftConfig.set("stations." + name + ".npc", null);
            plugin.configManager.saveStations();
            stationMasterUUID = null;
        }
    }

    /**
     * Get the station master NPC entity if it is loaded.
     * @return The Entity if loaded, otherwise null.
     */
    public Entity getStationMaster() {
        if (stationMasterUUID == null) return null;
        World world = location.getWorld();
        if (world == null) return null;
        for (Entity entity : world.getEntitiesByClass(Villager.class)) {
            if (entity.getUniqueId().equals(stationMasterUUID)) {
                return entity;
            }
        }
        return null;
    }

    public void setStationMasterUUID(UUID uuid) {
        this.stationMasterUUID = uuid;
        if (uuid != null) {
            ftConfig.set("stations." + name + ".npc", uuid.toString());
        } else {
            ftConfig.set("stations." + name + ".npc", null);
        }
        plugin.configManager.saveStations();
    }

    public UUID getStationMasterUUID() {
        return stationMasterUUID;
    }

    /**
     * Move the station master NPC to a new location without changing the station's location.
     * @param newLocation The new location for the station master NPC.
     * @return true if the NPC was moved, false if not found.
     */
    public boolean setStationMasterLocation(Location newLocation) {
        Entity stationMaster = getStationMaster();
        if (stationMaster == null) {
            return false;
        }
        stationMaster.teleport(newLocation);
        return true;
    }

    /**
     * Calculates the travel time from this station to another station and returns a formatted string.
     * The time is based on the distance between the train locations of the lines, or the station locations if not set.
     * @param destination The destination station.
     * @return A formatted string representing the travel time (e.g., "5s" or "01:23").
     */
    public String getTravelTime(Station destination) {
        return getTravelTime(destination.getLocation());
    }

    public String getTravelTime(Location destination) {
        if (location == null || destination == null || location.getWorld() == null || destination.getWorld() == null
                || !location.getWorld().equals(destination.getWorld())) {
            return "0s";
        }
        int ticks = (int) location.distance(destination); // 1 block = 20 ticks
        int seconds =  ticks / 20;
        int minutes = seconds / 60;
        seconds %= 60;
//        return minutes > 0 ? String.format("%02d:%02d", minutes, seconds) : String.format("00:%02d seconds", seconds);
        return String.format("%02d:%02d", minutes, seconds);
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
        ftConfig.set("stations." + name + ".display-name", displayName);
        plugin.configManager.saveStations();
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
        ftConfig.set("stations." + name + ".location.world", location.getWorld().getName());
        ftConfig.set("stations." + name + ".location.x", location.getX());
        ftConfig.set("stations." + name + ".location.y", location.getY());
        ftConfig.set("stations." + name + ".location.z", location.getZ());
        ftConfig.set("stations." + name + ".location.yaw", location.getYaw());
        ftConfig.set("stations." + name + ".location.pitch", location.getPitch());
        plugin.configManager.saveStations();
    }

    public Line getLine() {
        return line;
    }

    public void setLine(Line line) {
        this.line.removeStation(this);
        this.line = line;
        this.line.addStation(this);
        ftConfig.set("stations." + name + ".line", line.getName());
        plugin.configManager.saveStations();
    }

    public List<Station> getConnections() {
        return connections;
    }

    public void addConnection(Station station) {
        if (!connections.contains(station)) {
            connections.add(station);
            ftConfig.set("stations." + name + ".connections", connections.stream().map(Station::getName).toList());
            plugin.configManager.saveStations();
        }
    }

    public void removeConnection(Station station) {
        if (connections.contains(station)) {
            connections.remove(station);
            ftConfig.set("stations." + name + ".connections", connections.stream().map(Station::getName).toList());
            plugin.configManager.saveStations();
        }
    }

}
