package me.bambam250.fancytrains.station;

import me.bambam250.fancytrains.Fancytrains;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Level;

public class StationManager implements Listener {
    private final Fancytrains plugin;
    public Map<UUID, String> travelingPlayers = new HashMap<>();
    public Map<String, Location> trainLocations = new HashMap<>();
    public Map<String, Set<String>> stationConnections = new HashMap<>();
    private FileConfiguration ftConfig;

    private final List<Material> BANNERS = List.of(Material.BLACK_BANNER, Material.BLUE_BANNER, Material.BROWN_BANNER, Material.CYAN_BANNER, Material.GRAY_BANNER, Material.GREEN_BANNER, Material.LIGHT_BLUE_BANNER, Material.LIGHT_GRAY_BANNER, Material.LIME_BANNER, Material.MAGENTA_BANNER, Material.ORANGE_BANNER, Material.PINK_BANNER, Material.PURPLE_BANNER, Material.RED_BANNER, Material.WHITE_BANNER, Material.YELLOW_BANNER);

    // Track spawned NPCs by station name for easy removal
    private final Map<String, UUID> stationNPCs = new HashMap<>();

    public StationManager(Fancytrains plugin) {
        this.plugin = plugin;
        ftConfig = plugin.configManager.ftConfig;
        loadStations();
    }

    public void saveStations() {
        plugin.configManager.saveStations();
    }

    /**
     * Loads stations and train locations from the configuration file into memory.
     */
    public void loadStations() {
        trainLocations.clear();
        stationConnections.clear();

        // Load train locations per line
        if (ftConfig.getConfigurationSection("lines") != null) {
            for (String lineName : ftConfig.getConfigurationSection("lines").getKeys(false)) {
                String path = "lines." + lineName + ".train-location";
                if (ftConfig.contains(path + ".world")) {
                    String worldName = ftConfig.getString(path + ".world");
                    double x = ftConfig.getDouble(path + ".x");
                    double y = ftConfig.getDouble(path + ".y");
                    double z = ftConfig.getDouble(path + ".z");
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        trainLocations.put(lineName, new Location(world, x, y, z));
                    }
                }
            }
        }

        // Load connections and spawn NPCs for existing stations
        if (ftConfig.getConfigurationSection("stations") != null) {
            for (String stationName : ftConfig.getConfigurationSection("stations").getKeys(false)) {
                List<String> connections = ftConfig.getStringList("stations." + stationName + ".connections");
                stationConnections.put(stationName, new HashSet<>(connections));
                if (!ftConfig.getBoolean("stations." + stationName + ".npc-spawned", false)) {
                    spawnStationNPC(stationName);
                }
            }
        }
    }

    /**
     * Spawns a Station Master NPC at the specified station's location.
     * @param stationName The name of the station where the NPC should be spawned.
     */
    public void spawnStationNPC(String stationName) {
//        ConfigurationSection stationSection = ftConfig.getConfigurationSection("stations." + stationName);
//        if (stationSection == null) return;
//        String worldName = stationSection.getString("world");
//        double x = stationSection.getDouble("x");
//        double y = stationSection.getDouble("y");
//        double z = stationSection.getDouble("z");
//        World world = Bukkit.getWorld(worldName);
//        if (world == null) return;
//        Location loc = new Location(world, x, y, z);
//
//        Villager villager = (Villager) world.spawnEntity(loc, EntityType.VILLAGER);
//        villager.setSilent(true);
//
//        ftConfig.set("stations." + stationName + ".npc-spawned", true);
//        plugin.configManager.saveStations();
        String worldName = ftConfig.getString("stations." + stationName + ".world");
        double x = ftConfig.getDouble("stations." + stationName + ".x");
        double y = ftConfig.getDouble("stations." + stationName + ".y");
        double z = ftConfig.getDouble("stations." + stationName + ".z");

        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        Location loc = new Location(world, x, y, z);
        Villager npc = (Villager) world.spawnEntity(loc, EntityType.VILLAGER);

        String displayName = ftConfig.getString("stations." + stationName + ".display-name");
        String trainLine = ftConfig.getString("stations." + stationName + ".line");
        String lineDisplayName = ftConfig.getString("lines." + trainLine + ".display-name");
        ChatColor lineColor = ChatColor.valueOf(ftConfig.getString("lines." + trainLine + ".color"));

        npc.setCustomName(ChatColor.GOLD + "Station Master - " + displayName + " " + lineColor + "(" + lineDisplayName + ")");
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setCollidable(false);
        npc.setSilent(true);

        // Store that NPC was spawned
        ftConfig.set("stations." + stationName + ".npc-spawned", true);
        saveStations();

        // Track NPC by station name
        stationNPCs.put(stationName, npc.getUniqueId());
    }

    /**
     * Removes the Station Master NPC associated with the given station.
     * @param stationName The name of the station.
     */
    public void removeStationNPC(String stationName) {
        UUID npcId = stationNPCs.get(stationName);
        if (npcId != null) {
            for (World world : Bukkit.getWorlds()) {
                Entity entity = world.getEntity(npcId);
                if (entity instanceof Villager) {
                    entity.remove();
                    break;
                }
            }
            stationNPCs.remove(stationName);
        } else {
            // Fallback: try to find by location if not tracked
            String worldName = ftConfig.getString("stations." + stationName + ".world");
            double x = ftConfig.getDouble("stations." + stationName + ".x");
            double y = ftConfig.getDouble("stations." + stationName + ".y");
            double z = ftConfig.getDouble("stations." + stationName + ".z");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Location loc = new Location(world, x, y, z);
                for (Entity entity : world.getNearbyEntities(loc, 2, 2, 2)) {
                    if (entity instanceof Villager villager && villager.getCustomName() != null && villager.getCustomName().contains("Station Master")) {
                        entity.remove();
                    }
                }
            }
        }
        ftConfig.set("stations." + stationName + ".npc-spawned", false);
        saveStations();
    }

    /**
     * Handles player interaction with Station Master NPCs.
     * @param event The PlayerInteractEntityEvent triggered by the interaction.
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
//        if (!(event.getRightClicked() instanceof Villager villager)) return;
//        if (!ChatColor.stripColor(villager.getCustomName()).equalsIgnoreCase("Station Master")) return;
//        String stationName = findStationByNPCLocation(villager.getLocation());
//        if (stationName == null) return;
//        openStationMenu(event.getPlayer(), stationName);
//        event.setCancelled(true);
        if (!(event.getRightClicked() instanceof Villager)) return;

        Villager villager = (Villager) event.getRightClicked();
        if (villager.getCustomName() == null || !villager.getCustomName().contains("Station Master")) return;

        event.setCancelled(true);

        // Find which station this NPC belongs to
        String npcStation = findStationByNPCLocation(villager.getLocation());
        if (npcStation == null) {
            event.getPlayer().sendMessage(ChatColor.RED + "Station not found!");
            return;
        }

        openStationMenu(event.getPlayer(), npcStation);
    }

    /**
     * Finds the station name by matching the NPC's location to station locations.
     * @param npcLoc The location of the NPC.
     * @return The station name if found, otherwise null.
     */
    public String findStationByNPCLocation(Location npcLoc) {
//        for (String stationName : ftConfig.getConfigurationSection("stations").getKeys(false)) {
//            ConfigurationSection section = ftConfig.getConfigurationSection("stations." + stationName);
//            if (section == null) continue;
//            String worldName = section.getString("world");
//            double x = section.getDouble("x");
//            double y = section.getDouble("y");
//            double z = section.getDouble("z");
//            if (npcLoc.getWorld().getName().equals(worldName)
//                    && Math.abs(npcLoc.getX() - x) < 0.5
//                    && Math.abs(npcLoc.getY() - y) < 0.5
//                    && Math.abs(npcLoc.getZ() - z) < 0.5) {
//                return stationName;
//            }
//        }
//        return null;
        for (String stationName : ftConfig.getConfigurationSection("stations").getKeys(false)) {
            String worldName = ftConfig.getString("stations." + stationName + ".world");
            double x = ftConfig.getDouble("stations." + stationName + ".x");
            double y = ftConfig.getDouble("stations." + stationName + ".y");
            double z = ftConfig.getDouble("stations." + stationName + ".z");

            if (npcLoc.getWorld().getName().equals(worldName) &&
                    npcLoc.distanceSquared(new Location(npcLoc.getWorld(), x, y, z)) < 25) { // Within 5 blocks
                return stationName;
            }
        }
        return null;
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
        Location currentLoc = trainLocations.get(currentLine);
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
                item = loadBannerFromConfig("lines." + lineName + ".flag");
            }
            ItemMeta meta = item.getItemMeta();

            String displayName = ftConfig.getString("stations." + stationName + ".display-name");

            // Calculate travel time
            Location destLoc = trainLocations.get(trainLine);
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
        pendingTravel.put(player.getUniqueId(), new PendingTravel(destinationStation, travelType, travelTimeSeconds));
    }

    // Helper class to track pending travel confirmations
    private static class PendingTravel {
        public final String destinationStation;
        public final String travelType;
        public final int travelTimeSeconds;

        public PendingTravel(String destinationStation, String travelType, int travelTimeSeconds) {
            this.destinationStation = destinationStation;
            this.travelType = travelType;
            this.travelTimeSeconds = travelTimeSeconds;
        }
    }

    // Map to track pending travel confirmations
    private final Map<UUID, PendingTravel> pendingTravel = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.startsWith(ChatColor.DARK_BLUE + "Travel Type")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;

            String currentStation = title.substring(title.lastIndexOf(" - ") + 3);
            // Find station by display name
            String stationName = null;
            for (String name : ftConfig.getConfigurationSection("stations").getKeys(false)) {
                if (ftConfig.getString("stations." + name + ".display-name").equals(currentStation)) {
                    stationName = name;
                    break;
                }
            }

            if (stationName == null) return;

            if (event.getCurrentItem().getType() == Material.EMERALD) {
                player.closeInventory();
                openDestinationMenu(player, stationName, "domestic");
            } else if (event.getCurrentItem().getType() == Material.DIAMOND) {
                player.closeInventory();
                openDestinationMenu(player, stationName, "international");
            }
            return;
        }

        if (title.equals(ChatColor.DARK_BLUE + "Domestic Destinations") ||
                title.equals(ChatColor.DARK_BLUE + "International Destinations")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null ||
                    (event.getCurrentItem().getType() != Material.RAIL &&
                            !BANNERS.contains(event.getCurrentItem().getType()))) return;

            String displayName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            // Find station by display name
            String stationName = null;
            for (String name : ftConfig.getConfigurationSection("stations").getKeys(false)) {
                if (ftConfig.getString("stations." + name + ".display-name").equals(displayName)) {
                    stationName = name;
                    break;
                }
            }

            if (stationName == null) {
                player.sendMessage(ChatColor.RED + "Station not found!");
                return;
            }

            player.closeInventory();

            // Determine travel type based on title
            String travelType = title.contains("International") ? "international" : "domestic";

            // Calculate travel time in seconds
            Location currentLoc = player.getLocation();
            Location destLoc = trainLocations.get(stationName);
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
            int travelTimeSeconds = 0;
            if (currentLoc != null && destLoc != null && currentLoc.getWorld().equals(destLoc.getWorld())) {
                travelTimeSeconds = (int) currentLoc.distance(destLoc) / 20;
                if (travelTimeSeconds < 1) travelTimeSeconds = 1;
            } else {
                travelTimeSeconds = 5; // fallback default
            }

            // Open confirmation GUI
            openTravelConfirmMenu(player, stationName, travelType, travelTimeSeconds);
            return;
        }

        // Confirmation GUI
        if (title.equals(ChatColor.DARK_GREEN + "Confirm Travel")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            PendingTravel pending = pendingTravel.get(player.getUniqueId());
            if (pending == null) {
                player.closeInventory();
                return;
            }

            if (event.getCurrentItem().getType() == Material.LIME_CONCRETE) {
                player.closeInventory();
                // Start the journey with the stored travel time
                startTrainJourney(player, pending.destinationStation, pending.travelType, pending.travelTimeSeconds * 20);
                pendingTravel.remove(player.getUniqueId());
            } else if (event.getCurrentItem().getType() == Material.RED_CONCRETE) {
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Travel cancelled.");
                pendingTravel.remove(player.getUniqueId());
            }
            return;
        }
    }

    // Overload to support custom travel time in ticks
    public void startTrainJourney(Player player, String destinationStation, String travelType, int travelTimeTicks) {
        if (travelingPlayers.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already traveling!");
            return;
        }

        // Get line for this station
        String lineName = ftConfig.getString("stations." + destinationStation + ".line");
        Location trainLoc = trainLocations.get(lineName);
        if (trainLoc == null) {
            player.sendMessage(ChatColor.RED + "Train location not set for this line!");
            return;
        }

        travelingPlayers.put(player.getUniqueId(), destinationStation);

        // Teleport to train location for the line
        player.teleport(trainLoc);

        String destinationDisplayName = ftConfig.getString("stations." + destinationStation + ".display-name");
        String destinationLine = ftConfig.getString("stations." + destinationStation + ".line");
        String lineDisplayName = ftConfig.getString("lines." + destinationLine + ".display-name");

        if (travelType.equals("international")) {
            player.sendMessage(ChatColor.GREEN + "You've boarded the train to " + ChatColor.GOLD + destinationDisplayName + ChatColor.GREEN + " in " + ChatColor.GOLD + lineDisplayName + ChatColor.GREEN + ". You will arrive in " + ChatColor.GOLD + (travelTimeTicks / 20) + ChatColor.GREEN + " seconds");
        } else {
            player.sendMessage(ChatColor.GREEN + "You've boarded the train to " + ChatColor.GOLD + destinationDisplayName + ChatColor.GREEN + ". You will arrive in " + ChatColor.GOLD + (travelTimeTicks / 20) + ChatColor.GREEN + " seconds");
        }

        // Start journey with effects
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= travelTimeTicks) {
                    // Complete journey
                    completeJourney(player, destinationStation, travelType);
                    cancel();
                    return;
                }

                // Create travel effects
                Location loc = player.getLocation();
                if (travelType.equals("international")) {
                    loc.getWorld().spawnParticle(Particle.PORTAL, loc, 15);
                    loc.getWorld().spawnParticle(Particle.SMOKE, loc, 5);
                } else {
                    loc.getWorld().spawnParticle(Particle.SMOKE, loc, 10);
                }

                if (ticks % 20 == 0) {
                    Sound sound = Sound.ENTITY_MINECART_RIDING;
                    player.playSound(loc, sound, 0.5f, 1.0f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /**
     * Completes the train journey for the player, teleporting them to the destination.
     * @param player The player completing the journey.
     * @param stationName The destination station's name.
     * @param travelType The type of travel ("domestic" or "international").
     */
    public void completeJourney(Player player, String stationName, String travelType) {
        // Get station location
        String worldName = ftConfig.getString("stations." + stationName + ".world");
        double x = ftConfig.getDouble("stations." + stationName + ".x");
        double y = ftConfig.getDouble("stations." + stationName + ".y");
        double z = ftConfig.getDouble("stations." + stationName + ".z");

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Destination world not found!");
            travelingPlayers.remove(player.getUniqueId());
            return;
        }

        Location destination = new Location(world, x, y, z);
        player.teleport(destination);

        // Effects
        player.playSound(destination, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        destination.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, destination, 20);

        String displayName = ftConfig.getString("stations." + stationName + ".display-name");
        String trainLine = ftConfig.getString("stations." + stationName + ".line");
        String lineDisplayName = ftConfig.getString("lines." + trainLine + ".display-name");

        if (travelType.equals("international")) {
            player.sendMessage(ChatColor.GOLD + "Welcome to " + lineDisplayName + "!");
            player.sendMessage(ChatColor.YELLOW + "Arrived at " + displayName + "!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Arrived at " + displayName + "!");
        }

        travelingPlayers.remove(player.getUniqueId());
    }

    /**
     * Prevents Station Master NPCs from taking damage.
     * @param event The EntityDamageEvent triggered by damage.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        // Only protect villagers that are tracked as station NPCs
        if (!isStationMasterNPC(villager)) return;
        event.setCancelled(true);
    }

    /**
     * Prevents Station Master NPCs from dying.
     * @param event The EntityDeathEvent triggered by villager death.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        // Only protect villagers that are tracked as station NPCs
        if (!isStationMasterNPC(villager)) return;
        event.setCancelled(true);
    }

    /**
     * Checks if a villager is a Station Master NPC managed by this plugin.
     */
    private boolean isStationMasterNPC(Villager villager) {
        // Check by UUID
        if (stationNPCs.containsValue(villager.getUniqueId())) return true;
        // Fallback: check custom name
        return villager.getCustomName() != null && villager.getCustomName().contains("Station Master");
    }

    /**
     * Save a banner item to config using serialization.
     * @param banner The banner ItemStack to save.
     * @param configPath The path in config (should be under "lines.<line>.<flag>").
     * @return True if saved successfully, false otherwise.
     */
    public boolean saveBannerToConfig(ItemStack banner, String configPath) {
        if (!(banner.getItemMeta() instanceof BannerMeta meta)) return false;
        ftConfig.set(configPath, banner);
        return true;
    }

    /**
     * Load a banner item from config using deserialization.
     * @param configPath The path in config (should be under "lines.<line>.<flag>").
     * @return The loaded banner ItemStack, or null if not found/invalid.
     */
    public ItemStack loadBannerFromConfig(String configPath) {
        Object obj = ftConfig.get(configPath);
        if (obj instanceof ItemStack stack && stack.getItemMeta() instanceof BannerMeta) {
            return stack;
        }
        return new ItemStack(Material.WHITE_BANNER); // Return a default banner if not found
    }
}
