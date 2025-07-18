package me.bambam250.fancytrains.command;

import me.bambam250.fancytrains.Fancytrains;
import me.bambam250.fancytrains.objects.Line;
import me.bambam250.fancytrains.objects.Station;
import me.bambam250.fancytrains.station.StationManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StationCommand implements CommandExecutor {

    private final Fancytrains plugin;
    private final StationManager stationManager;

    public StationCommand(Fancytrains pl) {
        this.plugin = pl;
        this.stationManager = pl.stationManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("station")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.YELLOW + "Train Station Commands:");
                player.sendMessage(ChatColor.GREEN + "/station list" + ChatColor.GRAY + " - List all available stations");
                player.sendMessage(ChatColor.GREEN + "/station create" + ChatColor.GRAY + " <name> <line> <display-name> - Create a new station");
                player.sendMessage(ChatColor.GREEN + "/station remove" + ChatColor.GRAY + " <name> - Remove a station");
                player.sendMessage(ChatColor.GREEN + "/station connect" + ChatColor.GRAY + " <station1> <station2> - Connect two stations together");
                player.sendMessage(ChatColor.GREEN + "/station disconnect" + ChatColor.GRAY + " <station1> <station2> - Disconnect two stations");
                player.sendMessage(ChatColor.GREEN + "/station modify" + ChatColor.GRAY + " <station> <attribute> [<value>] - Modify a station's attribute (display-name, line, npc-location, station-location)");
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Usage: /station create <station name> <line> <display-name>");
                    return true;
                }

                String stationName = args[1].toLowerCase();
                if (stationManager.isStation(stationName)) {
                    player.sendMessage(ChatColor.RED + "Station already exists!");
                    return true;
                }

                String stationLine = args[2].toLowerCase();
                Line line = stationManager.getLine(stationLine);
                if (line == null) {
                    player.sendMessage(ChatColor.RED + "Line does not exist!");
                    return true;
                }

                // Concatenate args[3] and onward into a displayName string
                StringBuilder displayNameBuilder = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (i > 3) displayNameBuilder.append(" ");
                    displayNameBuilder.append(args[i]);
                }
                String displayName = displayNameBuilder.toString();

                Location loc = player.getLocation();
                Station station = new Station(stationName, displayName, loc, line);
                stationManager.addStation(station);
                line.addStation(station);

                // Automatically connect all stations within the same line
                for (Station other : new ArrayList<>(line.getStations())) {
                    if (!other.getName().equalsIgnoreCase(stationName)) {
                        station.addConnection(other);
                        other.addConnection(station);
                    }
                }

//                station.spawnStationMaster();

                player.sendMessage(ChatColor.GREEN + "Station created on " + line.getDisplayName() + " line!");
                return true;
            }

            if (args[0].equalsIgnoreCase("remove")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /station remove <station name>");
                    return true;
                }

                String stationName = args[1].toLowerCase();
                Station station = stationManager.getStation(stationName);
                if (station == null) {
                    player.sendMessage(ChatColor.RED + "Station does not exist!");
                    return true;
                }

                // Remove this station from connections of all other stations
                for (Station other : stationManager.getStations()) {
                    if (!other.getName().equalsIgnoreCase(stationName)) {
                        other.removeConnection(station);
                    }
                }
                // Remove from line
                Line line = station.getLine();
                if (line != null) {
                    line.removeStation(station);
                }
                // Remove NPC
                station.removeStationMaster();
                // Remove from manager
                stationManager.removeStation(station);

                player.sendMessage(ChatColor.GREEN + "Station removed!");
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                // Optional: /station list [line]
                if (args.length == 2) {
                    String lineName = args[1].toLowerCase();
                    Line line = stationManager.getLine(lineName);
                    if (line == null) {
                        player.sendMessage(ChatColor.RED + "Line does not exist!");
                        return true;
                    }
                    listStations(player, line);
                } else {
                    listStations(player);
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("connect")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /station connect <station1> <station2>");
                    return true;
                }
                String s1 = args[1].toLowerCase();
                String s2 = args[2].toLowerCase();
                connectStations(player, s1, s2);
                return true;
            }

            if (args[0].equalsIgnoreCase("disconnect")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /station disconnect <station1> <station2>");
                    return true;
                }
                String s1 = args[1].toLowerCase();
                String s2 = args[2].toLowerCase();
                disconnectStations(player, s1, s2);
                return true;
            }

            if (args[0].equalsIgnoreCase("modify")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /station modify <station name> <attribute> [<value>]");
                    player.sendMessage(ChatColor.YELLOW + "Attributes: display-name, line, npc-location, station-location");
                    return true;
                }
                String stationName = args[1].toLowerCase();
                String attribute = args[2].toLowerCase();
                String value = "";
                if (args.length > 3) {
                    StringBuilder valueBuilder = new StringBuilder();
                    for (int i = 3; i < args.length; i++) {
                        if (i > 3) valueBuilder.append(" ");
                        valueBuilder.append(args[i]);
                    }
                    value = valueBuilder.toString();
                }
                modifyStation(player, stationName, attribute, value);
                return true;
            }
        }
        return false;
    }

    private void listStations(Player player) {
        List<Station> stations = stationManager.getStations();
        if (stations.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No stations found!");
            return;
        }
        player.sendMessage(ChatColor.YELLOW + "Available Stations:");
        for (Station station : stations) {
            Line line = station.getLine();
            String lineDisplayName = line != null ? line.getDisplayName() : "Unknown";
            ChatColor lineColor = line != null ? line.getColor() : ChatColor.WHITE;
            player.sendMessage(ChatColor.GRAY + "- " + station.getDisplayName() + " " + lineColor + "(" + lineDisplayName + ")");
        }
    }

    private void listStations(Player player, Line line) {
        List<Station> stations = line.getStations();
        if (stations.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No stations found on this line.");
            return;
        }
        player.sendMessage(line.getColor() + "Stations on line: " + line.getDisplayName());
        for (Station station : stations) {
            player.sendMessage(ChatColor.GRAY + "- " + station.getDisplayName());
        }
    }

    private void connectStations(Player player, String station1, String station2) {
        Station s1 = stationManager.getStation(station1);
        Station s2 = stationManager.getStation(station2);

        if (s1 == null) {
            player.sendMessage(ChatColor.RED + "Station " + station1 + " does not exist!");
            return;
        }
        if (s2 == null) {
            player.sendMessage(ChatColor.RED + "Station " + station2 + " does not exist!");
            return;
        }
        if (s1.getName().equalsIgnoreCase(s2.getName())) {
            player.sendMessage(ChatColor.RED + "You cannot connect a station to itself!");
            return;
        }

        s1.addConnection(s2);
        s2.addConnection(s1);

        player.sendMessage(ChatColor.GREEN + "Connected " + s1.getDisplayName() + " and " + s2.getDisplayName() + "!");
    }

    private void disconnectStations(Player player, String station1, String station2) {
        Station s1 = stationManager.getStation(station1);
        Station s2 = stationManager.getStation(station2);

        if (s1 == null || s2 == null) {
            player.sendMessage(ChatColor.RED + "One or both stations do not exist!");
            return;
        }

        s1.removeConnection(s2);
        s2.removeConnection(s1);

        player.sendMessage(ChatColor.GREEN + "Disconnected " + s1.getDisplayName() + " and " + s2.getDisplayName() + "!");
    }

    private void modifyStation(Player player, String stationName, String attribute, String value) {
        Station station = stationManager.getStation(stationName);
        if (station == null) {
            player.sendMessage(ChatColor.RED + "Station does not exist!");
            return;
        }

        switch (attribute) {
            case "display-name":
                if (value.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "You must provide a value for display-name.");
                    return;
                }
                station.setDisplayName(value);
                // Update station master name if loaded
                if (station.getStationMaster() != null) {
                    station.getStationMaster().setCustomName(ChatColor.GOLD + "Station Master - " + station.getDisplayName() + " " + station.getLine().getColor() + "(" + station.getLine().getDisplayName() + ")");
                    player.sendMessage(ChatColor.GREEN + "Set display name for " + stationName + " to " + value);
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Set display name for " + stationName + " to " + value + ", but station master is not loaded in the world.");
                }
                break;
            case "line":
                if (value.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "You must provide a value for line.");
                    return;
                }
                Line newLine = stationManager.getLine(value.toLowerCase());
                if (newLine == null) {
                    player.sendMessage(ChatColor.RED + "Line does not exist!");
                    return;
                }
                station.setLine(newLine);
                // Update station master name if loaded
                if (station.getStationMaster() != null) {
                    station.getStationMaster().setCustomName(ChatColor.GOLD + "Station Master - " + station.getDisplayName() + " " + station.getLine().getColor() + "(" + station.getLine().getDisplayName() + ")");
                    player.sendMessage(ChatColor.GREEN + "Set line for " + stationName + " to " + value);
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Set line for " + stationName + " to " + value + ", but station master is not loaded in the world.");
                }
                break;
            case "npc-location":
                // Only allow if NPC is loaded
                if (station.getStationMaster() == null) {
                    player.sendMessage(ChatColor.RED + "Station master NPC is not loaded in the world. You must be near the NPC to move it.");
                    return;
                }
                if (station.setStationMasterLocation(player.getLocation())) {
                    player.sendMessage(ChatColor.GREEN + "Moved station master for " + stationName + " to your current location.");
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to move station master. NPC is not loaded or has been removed.");
                }
                break;
            case "station-location":
                // Only allow if NPC is loaded
                if (station.getStationMaster() == null) {
                    player.sendMessage(ChatColor.RED + "Station master NPC is not loaded in the world. You must be near the NPC to change the station location.");
                    return;
                }
                station.setLocation(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Set station location for " + stationName + " to your current location.");
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown attribute. Supported: display-name, line, npc-location, station-location");
        }
    }
}
