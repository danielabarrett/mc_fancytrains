package me.bambam250.fancytrains.command;

import me.bambam250.fancytrains.Fancytrains;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class StationCommand implements CommandExecutor {

    private Fancytrains plugin;

    public StationCommand(Fancytrains pl) {
        plugin = pl;
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
                player.sendMessage(ChatColor.GREEN + "/station create" + ChatColor.GRAY + " <name> <line> - Create a new station");
                player.sendMessage(ChatColor.GREEN + "/station remove" + ChatColor.GRAY + " <name> - Remove a station");
                player.sendMessage(ChatColor.GREEN + "/station connect" + ChatColor.GRAY + " <station1> <station2> - Connect two stations together");
                player.sendMessage(ChatColor.GREEN + "/station disconnect" + ChatColor.GRAY + " <station1> <station2> - Disconnect two stations");
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Usage: /station create <station name> <line> <display-name>");
                    return true;
                }

                String stationName = args[1].toLowerCase();
                if (plugin.configManager.ftConfig.contains("stations." + stationName)) {
                    player.sendMessage(ChatColor.RED + "Station already exists!");
                    return true;
                }

                String stationLine = args[2].toLowerCase();
                if (!plugin.configManager.ftConfig.contains("lines." + stationLine)) {
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

                createStation((Player) sender, stationName, stationLine, displayName);
                return true;
            }

            if (args[0].equalsIgnoreCase("remove")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /station remove <station name>");
                    return true;
                }

                String stationName = args[1].toLowerCase();
                if (!plugin.configManager.ftConfig.contains("stations." + stationName)) {
                    player.sendMessage(ChatColor.RED + "Station does not exist!");
                    return true;
                }

                removeStation(stationName);
                player.sendMessage(ChatColor.GREEN + "Station removed!");
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                // Optional: /station list [line]
                if (args.length == 2) {
                    String lineName = args[1].toLowerCase();
                    if (!plugin.configManager.ftConfig.contains("lines." + lineName)) {
                        player.sendMessage(ChatColor.RED + "Line does not exist!");
                        return true;
                    }
                    listStations(player, lineName);
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

                connectStations(player, args[1].toLowerCase(), args[2].toLowerCase());
                return true;
            }

            if (args[0].equalsIgnoreCase("disconnect")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /station disconnect <station1> <station2>");
                    return true;
                }

                disconnectStations(player, args[1].toLowerCase(), args[2].toLowerCase());
                return true;
            }

            if (args[0].equalsIgnoreCase("modify")) {
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Usage: /station modify <station name> <attribute> <value>");
                    return true;
                }
                String stationName = args[1].toLowerCase();
                String attribute = args[2].toLowerCase();
                StringBuilder valueBuilder = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (i > 3) valueBuilder.append(" ");
                    valueBuilder.append(args[i]);
                }
                String value = valueBuilder.toString();
                modifyStation(player, stationName, attribute, value);
                return true;
            }
        }
        return false;
    }

    // Adjusted to accept displayName
    private void createStation(Player player, String stationName, String trainLine, String displayName) {
        Location loc = player.getLocation();

        plugin.configManager.ftConfig.set("stations." + stationName + ".world", loc.getWorld().getName());
        plugin.configManager.ftConfig.set("stations." + stationName + ".x", loc.getX());
        plugin.configManager.ftConfig.set("stations." + stationName + ".y", loc.getY());
        plugin.configManager.ftConfig.set("stations." + stationName + ".z", loc.getZ());
        plugin.configManager.ftConfig.set("stations." + stationName + ".display-name", displayName);
        plugin.configManager.ftConfig.set("stations." + stationName + ".line", trainLine);
        plugin.configManager.ftConfig.set("stations." + stationName + ".npc-spawned", false);
        plugin.configManager.ftConfig.set("stations." + stationName + ".connections", Arrays.asList());

        // No longer set train location per station

        plugin.stationManager.stationConnections.put(stationName, new HashSet<>());
        plugin.configManager.saveStations();

        // Automatically connect all stations within the same line
        if (plugin.configManager.ftConfig.getConfigurationSection("stations") != null) {
            for (String otherStation : plugin.configManager.ftConfig.getConfigurationSection("stations").getKeys(false)) {
                if (!otherStation.equals(stationName)) {
                    String otherLine = plugin.configManager.ftConfig.getString("stations." + otherStation + ".line");
                    if (trainLine.equalsIgnoreCase(otherLine)) {
                        // Add bidirectional connection
                        plugin.stationManager.stationConnections.get(stationName).add(otherStation);
                        plugin.stationManager.stationConnections.get(otherStation).add(stationName);

                        // Save to config
                        plugin.configManager.ftConfig.set("stations." + stationName + ".connections", new ArrayList<>(plugin.stationManager.stationConnections.get(stationName)));
                        plugin.configManager.ftConfig.set("stations." + otherStation + ".connections", new ArrayList<>(plugin.stationManager.stationConnections.get(otherStation)));
                    }
                }
            }
            plugin.configManager.saveStations();
        }

        plugin.stationManager.spawnStationNPC(stationName);

        String lineDisplayName = plugin.configManager.ftConfig.getString("lines." + trainLine + ".display-name");
        player.sendMessage(ChatColor.GREEN + "Station created on " + lineDisplayName + " line! Use /station settrain " + trainLine + " to customize train location for this line.");
    }


    private void removeStation(String stationName) {
        // Remove the NPC associated with the station
        plugin.stationManager.removeStationNPC(stationName);

        // Remove this station from the connections of all other stations
        if (plugin.configManager.ftConfig.getConfigurationSection("stations") != null) {
            for (String otherStation : plugin.configManager.ftConfig.getConfigurationSection("stations").getKeys(false)) {
                if (otherStation.equals(stationName)) continue;
                // Get current connections
                java.util.List<String> connections = plugin.configManager.ftConfig.getStringList("stations." + otherStation + ".connections");
                if (connections.contains(stationName)) {
                    connections.remove(stationName);
                    plugin.configManager.ftConfig.set("stations." + otherStation + ".connections", connections);
                    // Also update in-memory connections if present
                    if (plugin.stationManager.stationConnections.containsKey(otherStation)) {
                        plugin.stationManager.stationConnections.get(otherStation).remove(stationName);
                    }
                }
            }
        }

        plugin.configManager.ftConfig.set("stations." + stationName, null);
        plugin.configManager.ftConfig.set("train-locations." + stationName, null);
        plugin.stationManager.trainLocations.remove(stationName);
        plugin.stationManager.stationConnections.remove(stationName);
        plugin.configManager.saveStations();
    }

    private void listStations(Player player) {
        if (plugin.configManager.ftConfig.getConfigurationSection("stations") == null) {
            player.sendMessage(ChatColor.YELLOW + "No stations found!");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Available Stations:");
        for (String stationName : plugin.configManager.ftConfig.getConfigurationSection("stations").getKeys(false)) {
            String displayName = plugin.configManager.ftConfig.getString("stations." + stationName + ".display-name");
            String trainLine = plugin.configManager.ftConfig.getString("stations." + stationName + ".line");
            String lineDisplayName = plugin.configManager.ftConfig.getString("lines." + trainLine + ".display-name");
            ChatColor lineColor = ChatColor.valueOf(plugin.configManager.ftConfig.getString("lines." + trainLine + ".color"));

            player.sendMessage(ChatColor.GRAY + "- " + displayName + " " + lineColor + "(" + lineDisplayName + ")");
        }
    }

    // New overloaded method to list only stations on a specific line
    private void listStations(Player player, String lineName) {
        if (plugin.configManager.ftConfig.getConfigurationSection("stations") == null) {
            player.sendMessage(ChatColor.YELLOW + "No stations found!");
            return;
        }

        String lineDisplayName = plugin.configManager.ftConfig.getString("lines." + lineName + ".display-name");
        ChatColor lineColor = ChatColor.valueOf(plugin.configManager.ftConfig.getString("lines." + lineName + ".color"));
        player.sendMessage(lineColor + "Stations on line: " + lineDisplayName);

        boolean found = false;
        for (String stationName : plugin.configManager.ftConfig.getConfigurationSection("stations").getKeys(false)) {
            String trainLine = plugin.configManager.ftConfig.getString("stations." + stationName + ".line");
            if (lineName.equalsIgnoreCase(trainLine)) {
                String displayName = plugin.configManager.ftConfig.getString("stations." + stationName + ".display-name");
                player.sendMessage(ChatColor.GRAY + "- " + displayName);
                found = true;
            }
        }
        if (!found) {
            player.sendMessage(ChatColor.YELLOW + "No stations found on this line.");
        }
    }


    private void connectStations(Player player, String station1, String station2) {
        if (!plugin.configManager.ftConfig.contains("stations." + station1)) {
            player.sendMessage(ChatColor.RED + "Station " + station1 + " does not exist!");
            return;
        }

        if (!plugin.configManager.ftConfig.contains("stations." + station2)) {
            player.sendMessage(ChatColor.RED + "Station " + station2 + " does not exist!");
            return;
        }

        // Prevent connecting a station to itself
        if (station1.equalsIgnoreCase(station2)) {
            player.sendMessage(ChatColor.RED + "You cannot connect a station to itself!");
            return;
        }

        // Add bidirectional connection
        plugin.stationManager.stationConnections.get(station1).add(station2);
        plugin.stationManager.stationConnections.get(station2).add(station1);

        // Save to config
        plugin.configManager.ftConfig.set("stations." + station1 + ".connections", new ArrayList<>(plugin.stationManager.stationConnections.get(station1)));
        plugin.configManager.ftConfig.set("stations." + station2 + ".connections", new ArrayList<>(plugin.stationManager.stationConnections.get(station2)));

        plugin.configManager.saveStations();

        String display1 = plugin.configManager.ftConfig.getString("stations." + station1 + ".display-name");
        String display2 = plugin.configManager.ftConfig.getString("stations." + station2 + ".display-name");
        player.sendMessage(ChatColor.GREEN + "Connected " + display1 + " and " + display2 + "!");
    }

    private void disconnectStations(Player player, String station1, String station2) {
        if (!plugin.configManager.ftConfig.contains("stations." + station1) || !plugin.configManager.ftConfig.contains("stations." + station2)) {
            player.sendMessage(ChatColor.RED + "One or both stations do not exist!");
            return;
        }

        plugin.stationManager.stationConnections.get(station1).remove(station2);
        plugin.stationManager.stationConnections.get(station2).remove(station1);

        plugin.configManager.ftConfig.set("stations." + station1 + ".connections", new ArrayList<>(plugin.stationManager.stationConnections.get(station1)));
        plugin.configManager.ftConfig.set("stations." + station2 + ".connections", new ArrayList<>(plugin.stationManager.stationConnections.get(station2)));

        plugin.configManager.saveStations();

        String display1 = plugin.configManager.ftConfig.getString("stations." + station1 + ".display-name");
        String display2 = plugin.configManager.ftConfig.getString("stations." + station2 + ".display-name");
        player.sendMessage(ChatColor.GREEN + "Disconnected " + display1 + " and " + display2 + "!");
    }

    private void modifyStation(Player player, String stationName, String attribute, String value) {
        if (!plugin.configManager.ftConfig.contains("stations." + stationName)) {
            player.sendMessage(ChatColor.RED + "Station does not exist!");
            return;
        }

        switch (attribute) {
            case "display-name":
                plugin.configManager.ftConfig.set("stations." + stationName + ".display-name", value);
                plugin.configManager.saveStations();
                player.sendMessage(ChatColor.GREEN + "Set display name for " + stationName + " to " + value);
                break;
            case "line":
                if (!plugin.configManager.ftConfig.contains("lines." + value.toLowerCase())) {
                    player.sendMessage(ChatColor.RED + "Line does not exist!");
                    return;
                }
                plugin.configManager.ftConfig.set("stations." + stationName + ".line", value.toLowerCase());
                plugin.configManager.saveStations();
                player.sendMessage(ChatColor.GREEN + "Set line for " + stationName + " to " + value);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown attribute. Supported: display-name, line");
        }
    }

    private String formatStationName(String name) {
        return ChatColor.translateAlternateColorCodes('&', name.replace("_", " "));
    }
}
