package me.bambam250.fancytrains.station;

import me.bambam250.fancytrains.Fancytrains;
import me.bambam250.fancytrains.objects.Line;
import me.bambam250.fancytrains.objects.Station;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Level;

public class StationGUIManager {

    private final Fancytrains plugin;
    private final StationManager stationManager;

    private final List<Material> BANNERS = List.of(Material.BLACK_BANNER, Material.BLUE_BANNER, Material.BROWN_BANNER, Material.CYAN_BANNER, Material.GRAY_BANNER, Material.GREEN_BANNER, Material.LIGHT_BLUE_BANNER, Material.LIGHT_GRAY_BANNER, Material.LIME_BANNER, Material.MAGENTA_BANNER, Material.ORANGE_BANNER, Material.PINK_BANNER, Material.PURPLE_BANNER, Material.RED_BANNER, Material.WHITE_BANNER, Material.YELLOW_BANNER);

    public StationGUIManager(Fancytrains pl) {
        this.plugin = pl;
        this.stationManager = pl.stationManager;
    }

    /**
     * Opens the station menu for the player, allowing them to choose travel options.
     * @param player The player to open the menu for.
     * @param currentStationName The current station's name.
     */
    public void openStationMenu(Player player, String currentStationName) {
        Station currentStation = stationManager.getStation(currentStationName);
        if (currentStation == null) {
            player.sendMessage(ChatColor.RED + "Station not found!");
            return;
        }
        List<Station> connections = currentStation.getConnections();
        if (connections == null || connections.isEmpty()) {
            player.sendMessage(ChatColor.RED + "This station has no connections!");
            return;
        }

        Line currentLine = currentStation.getLine();

        boolean hasInternational = false;
        boolean hasDomestic = false;

        for (Station connectedStation : connections) {
            Line connectedLine = connectedStation.getLine();
            if (connectedLine != null && currentLine != null && connectedLine.getName().equals(currentLine.getName())) {
                hasDomestic = true;
            } else {
                hasInternational = true;
            }
        }

        if (hasInternational && hasDomestic) {
            openTravelTypeMenu(player, currentStationName);
        } else {
            openDestinationMenu(player, currentStationName, hasInternational ? "international" : "domestic");
        }
    }

    /**
     * Opens the travel type selection menu (domestic/international) for the player.
     * @param player The player to open the menu for.
     * @param currentStationName The current station's name.
     */
    public void openTravelTypeMenu(Player player, String currentStationName) {
        Station currentStation = stationManager.getStation(currentStationName);
        if (currentStation == null) {
            player.sendMessage(ChatColor.RED + "Station not found!");
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_BLUE + "Travel Type - " +
                currentStation.getDisplayName());

        // Domestic travel option
        ItemStack domestic = new ItemStack(Material.EMERALD);
        ItemMeta domesticMeta = domestic.getItemMeta();
        domesticMeta.setDisplayName(ChatColor.GREEN + "Domestic Travel");
        Line currentLine = currentStation.getLine();
        String lineDisplayName = currentLine != null ? currentLine.getDisplayName() : "Unknown";
        domesticMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Travel within " + lineDisplayName));
        domestic.setItemMeta(domesticMeta);
        inv.setItem(3, domestic);

        // International travel option
        ItemStack international = new ItemStack(Material.DIAMOND);
        ItemMeta internationalMeta = international.getItemMeta();
        internationalMeta.setDisplayName(ChatColor.BLUE + "International Travel");
        internationalMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Travel to other nations"));
        international.setItemMeta(internationalMeta);
        inv.setItem(5, international);

        player.openInventory(inv);
    }

    /**
     * Opens the destination selection menu for the player based on travel type.
     * @param player The player to open the menu for.
     * @param currentStationName The current station's name.
     * @param travelType The type of travel ("domestic" or "international").
     */
    public void openDestinationMenu(Player player, String currentStationName, String travelType) {
        Station currentStation = stationManager.getStation(currentStationName);
        if (currentStation == null) {
            player.sendMessage(ChatColor.RED + "Station not found!");
            return;
        }
        List<Station> connections = currentStation.getConnections();
        Line currentLine = currentStation.getLine();

        List<Station> filteredConnections = new ArrayList<>();
        for (Station connectedStation : connections) {
            Line connectedLine = connectedStation.getLine();
            if (travelType.equals("domestic") && connectedLine != null && currentLine != null && connectedLine.getName().equals(currentLine.getName())) {
                filteredConnections.add(connectedStation);
            } else if (travelType.equals("international") && (connectedLine == null || !connectedLine.getName().equals(currentLine.getName()))) {
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

        int slot = 0;
        for (Station station : filteredConnections) {
            Line trainLine = station.getLine();
            String lineDisplayName = trainLine != null ? trainLine.getDisplayName() : "Unknown";
            ChatColor lineColor = trainLine != null ? trainLine.getColor() : ChatColor.WHITE;

            ItemStack item = trainLine != null ? trainLine.getFlag() : new ItemStack(Material.WHITE_BANNER);
            ItemMeta meta = item.getItemMeta();

            String displayName = station.getDisplayName();

            String travelTimeStr = currentStation.getTravelTime(station);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to travel to " + displayName);
            lore.add(lineColor + "Line: " + lineDisplayName);
            lore.add(ChatColor.AQUA + "Travel Time: " + travelTimeStr);

            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
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
     * @param destinationStationName The destination station's name.
     * @param travelType The type of travel ("domestic" or "international").
     * @param travelTimeSeconds The travel time in seconds.
     */
    public void openTravelConfirmMenu(Player player, String destinationStationName, String travelType, int travelTimeSeconds) {
        Station destStation = stationManager.getStation(destinationStationName);
        if (destStation == null) {
            player.sendMessage(ChatColor.RED + "Destination station not found!");
            return;
        }
        String displayName = destStation.getDisplayName();
        String menuTitle = ChatColor.DARK_GREEN + "Confirm Travel";
        Inventory inv = Bukkit.createInventory(null, 9, menuTitle);

        String travelTimeStr = destStation.getTravelTime(player.getLocation());

        // Confirm item
        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Confirm Travel");
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.YELLOW + "Destination: " + displayName);
        confirmLore.add(ChatColor.AQUA + "Travel Time: " + travelTimeStr);
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

        // Store pending travel info in StationManager
        stationManager.addPendingTraveler(player, destinationStationName, travelType, destStation.getTravelTimeTicks(player.getLocation()) / 20);
    }

    /**
     * Handles inventory click events for the GUIs managed by this class.
     * @param event The InventoryClickEvent.
     * @param player The player who clicked.
     * @param title The inventory title.
     * @param stationManager The StationManager instance.
     */
    public void handleInventoryClick(InventoryClickEvent event, Player player, String title, StationManager stationManager) {
        if (title.startsWith(ChatColor.DARK_BLUE + "Travel Type")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;

            String currentStation = title.substring(title.lastIndexOf(" - ") + 3);
            // Find station by display name
            Station destStation = null;
            for (Station station : stationManager.getStations()) {
                if (station.getDisplayName().equals(currentStation)) {
                    destStation = station;
                    break;
                }
            }

            if (destStation == null) return;

            if (event.getCurrentItem().getType() == Material.EMERALD) {
                player.closeInventory();
                plugin.GUIManager.openDestinationMenu(player, destStation.getName(), "domestic");
            } else if (event.getCurrentItem().getType() == Material.DIAMOND) {
                player.closeInventory();
                plugin.GUIManager.openDestinationMenu(player, destStation.getName(), "international");
            }
            return;
        }

        if (title.equals(ChatColor.DARK_BLUE + "Domestic Destinations") ||
                title.equals(ChatColor.DARK_BLUE + "International Destinations")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || !BANNERS.contains(event.getCurrentItem().getType())) return;

            String displayName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            // Find station by display name
            Station destStation = null;
            for (Station station : stationManager.getStations()) {
                if (station.getDisplayName().equals(displayName)) {
                    destStation = station;
                    break;
                }
            }

            if (destStation == null) {
                player.sendMessage(ChatColor.RED + "Station not found!");
                return;
            }

            player.closeInventory();

            String travelType = title.contains("International") ? "international" : "domestic";

            // Calculate travel time in seconds
            Location currentLoc = player.getLocation();
            Location destLoc = destStation.getLocation();

            int travelTimeSeconds;
            if (currentLoc != null && destLoc != null && currentLoc.getWorld().equals(destLoc.getWorld())) {
                travelTimeSeconds = (int) currentLoc.distance(destLoc) / 20;
                if (travelTimeSeconds < 1) travelTimeSeconds = 1;
            } else {
                travelTimeSeconds = 5; // fallback default
                Bukkit.getLogger().log(Level.WARNING, "Could not calculate travel time for " + destStation.getName() + ". Using default 5 seconds.");
            }

            // Open confirmation GUI
            plugin.GUIManager.openTravelConfirmMenu(player, destStation.getName(), travelType, travelTimeSeconds);
            return;
        }

        // Confirmation GUI
        if (title.equals(ChatColor.DARK_GREEN + "Confirm Travel")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            StationManager.PendingTravel pending = stationManager.getPendingTravel(player.getUniqueId());
            if (pending == null) {
                player.closeInventory();
                return;
            }

            if (event.getCurrentItem().getType() == Material.LIME_CONCRETE) {
                player.closeInventory();
                stationManager.startTrainJourney(player, pending.destinationStation, pending.travelType, pending.travelTimeSeconds * 20);
                stationManager.removePendingTravel(player.getUniqueId());
            } else if (event.getCurrentItem().getType() == Material.RED_CONCRETE) {
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Travel cancelled.");
                stationManager.removePendingTravel(player.getUniqueId());
            }
        }
    }
}
