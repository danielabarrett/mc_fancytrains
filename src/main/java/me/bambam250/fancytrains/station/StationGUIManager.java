package me.bambam250.fancytrains.station;

import me.bambam250.fancytrains.Fancytrains;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StationGUIManager {

    Fancytrains plugin = Fancytrains.getPlugin(Fancytrains.class);
    FileConfiguration ftConfig = plugin.configManager.ftConfig;
    Map<String, Set<String>> stationConnections = plugin.stationManager.stationConnections;
    StationManager stationManager = plugin.stationManager;

    public StationGUIManager(Fancytrains pl) {
        plugin = pl;
    }

    /**
     * Opens the station menu for the player, allowing them to choose travel options.
     * @param player The player to open the menu for.
     * @param currentStation The current station's name.
     */
    public void openStationMenu(Player player, String currentStation) {
        Set<String> connections = stationConnections.get(currentStation);
        if (connections == null || connections.isEmpty()) {
            player.sendMessage(ChatColor.RED + "This station has no connections!");
            return;
        }

        String currentLine = ftConfig.getString("stations." + currentStation + ".line");

        // Check if there are international connections
        boolean hasInternational = false;
        boolean hasDomestic = false;

        for (String connectedStation : connections) {
            String connectedLine = ftConfig.getString("stations." + connectedStation + ".line");
            if (connectedLine.equals(currentLine)) {
                hasDomestic = true;
            } else {
                hasInternational = true;
            }
        }

        if (hasInternational && hasDomestic) {
            // Show domestic/international choice menu
            openTravelTypeMenu(player, currentStation);
        } else {
            // Show direct destination menu
            openDestinationMenu(player, currentStation, hasInternational ? "international" : "domestic");
        }
    }

    /**
     * Opens the travel type selection menu (domestic/international) for the player.
     * @param player The player to open the menu for.
     * @param currentStation The current station's name.
     */
    public void openTravelTypeMenu(Player player, String currentStation) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_BLUE + "Travel Type - " +
                ftConfig.getString("stations." + currentStation + ".display-name"));

        // Domestic travel option
        ItemStack domestic = new ItemStack(Material.EMERALD);
        ItemMeta domesticMeta = domestic.getItemMeta();
        domesticMeta.setDisplayName(ChatColor.GREEN + "Domestic Travel");
        String currentLine = ftConfig.getString("stations." + currentStation + ".line");
        String lineDisplayName = ftConfig.getString("lines." + currentLine + ".display-name");
        domesticMeta.setLore(Arrays.asList(ChatColor.GRAY + "Travel within " + lineDisplayName));
        domestic.setItemMeta(domesticMeta);
        inv.setItem(3, domestic);

        // International travel option
        ItemStack international = new ItemStack(Material.DIAMOND);
        ItemMeta internationalMeta = international.getItemMeta();
        internationalMeta.setDisplayName(ChatColor.BLUE + "International Travel");
        internationalMeta.setLore(Arrays.asList(ChatColor.GRAY + "Travel to other nations"));
        international.setItemMeta(internationalMeta);
        inv.setItem(5, international);

        player.openInventory(inv);
    }

    /**
     * Opens the destination selection menu for the player based on travel type.
     * @param player The player to open the menu for.
     * @param currentStation The current station's name.
     * @param travelType The type of travel ("domestic" or "international").
     */
    public void openDestinationMenu(Player player, String currentStation, String travelType) {
        Set<String> connections = stationConnections.get(currentStation);
        String currentLine = ftConfig.getString("stations." + currentStation + ".line");

        // Filter connections based on travel type
        Set<String> filteredConnections = new HashSet<>();
        for (String connectedStation : connections) {
            String connectedLine = ftConfig.getString("stations." + connectedStation + ".line");
            if (travelType.equals("domestic") && connectedLine.equals(currentLine)) {
                filteredConnections.add(connectedStation);
            } else if (travelType.equals("international") && !connectedLine.equals(currentLine)) {
                filteredConnections.add(connectedStation);
            }
        }

        if (filteredConnections.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No " + travelType + " connections available!");
            return;
        }

        int size = Math.min(54, ((filteredConnections.size() + 8) / 9) * 9);
        String menuTitle = travelType.equals("domestic") ? "Domestic Destinations" : "International Destinations";
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_BLUE + menuTitle);

        // Get current line train location for travel time calculation
        Location currentLoc = stationManager.trainLocations.get(currentLine);
        if (currentLoc == null) {
            // fallback to station location if train location not set
            String worldName = ftConfig.getString("stations." + currentStation + ".world");
            double x = ftConfig.getDouble("stations." + currentStation + ".x");
            double y = ftConfig.getDouble("stations." + currentStation + ".y");
            double z = ftConfig.getDouble("stations." + currentStation + ".z");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                currentLoc = new Location(world, x, y, z);
            }
        }

        int slot = 0;
        for (String stationName : filteredConnections) {
            String trainLine = ftConfig.getString("stations." + stationName + ".line");
            String lineDisplayName = ftConfig.getString("lines." + trainLine + ".display-name");
            ChatColor lineColor = ChatColor.valueOf(ftConfig.getString("lines." + trainLine + ".color"));

            ItemStack item;
            if (travelType.equals("domestic")) {
                item = new ItemStack(Material.RAIL);
            } else {
                String lineName = ftConfig.getString("stations." + stationName + ".line");
                item = stationManager.loadBannerFromConfig("lines." + lineName + ".flag");
            }
            ItemMeta meta = item.getItemMeta();

            String displayName = ftConfig.getString("stations." + stationName + ".display-name");

            // Calculate travel time
            Location destLoc = stationManager.trainLocations.get(trainLine);
            if (destLoc == null) {
                String worldName = ftConfig.getString("stations." + stationName + ".world");
                double x = ftConfig.getDouble("stations." + stationName + ".x");
                double y = ftConfig.getDouble("stations." + stationName + ".y");
                double z = ftConfig.getDouble("stations." + stationName + ".z");
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    destLoc = new Location(world, x, y, z);
                }
            }
            String travelTimeStr = "";
            if (currentLoc != null && destLoc != null && currentLoc.getWorld().equals(destLoc.getWorld())) {
                int travelTimeTicks = (int) currentLoc.distance(destLoc);
                String formattedTime = Fancytrains.formatTime(travelTimeTicks);
                travelTimeStr = ChatColor.AQUA + "Travel Time: " + formattedTime + "s";
            }

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to travel to " + displayName);
            lore.add(lineColor + "Line: " + lineDisplayName);
            if (!travelTimeStr.isEmpty()) {
                lore.add(travelTimeStr);
            }

            meta.setDisplayName(ChatColor.YELLOW + displayName);
            meta.setLore(lore);

            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    /**
     * Opens a confirmation GUI for the player to confirm travel to a destination.
     * @param player The player to open the menu for.
     * @param destinationStation The destination station's name.
     * @param travelType The type of travel ("domestic" or "international").
     * @param travelTimeSeconds The travel time in seconds.
     */
    public void openTravelConfirmMenu(Player player, String destinationStation, String travelType, int travelTimeSeconds) {
        String displayName = ftConfig.getString("stations." + destinationStation + ".display-name");
        String menuTitle = ChatColor.DARK_GREEN + "Confirm Travel";
        Inventory inv = Bukkit.createInventory(null, 9, menuTitle);

        // Confirm item
        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Confirm Travel");
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.YELLOW + "Destination: " + displayName);
        confirmLore.add(ChatColor.AQUA + "Travel Time: " + travelTimeSeconds + "s");
        confirmMeta.setLore(confirmLore);
        confirm.setItemMeta(confirmMeta);
        inv.setItem(3, confirm);

        // Cancel item
        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
        cancel.setItemMeta(cancelMeta);
        inv.setItem(5, cancel);

        player.openInventory(inv);

        // Store pending travel info in metadata or a map if needed
        // For simplicity, you can use a map in StationManager:
//        pendingTravel.put(player.getUniqueId(), new StationManager.PendingTravel(destinationStation, travelType, travelTimeSeconds));
        stationManager.addPendingTraveler(player, destinationStation, travelType, travelTimeSeconds);
    }
}

