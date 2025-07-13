package me.bambam250.fancytrains;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public final class Fancytrains extends JavaPlugin implements Listener {

    private File stationsFile;
    private FileConfiguration stationsConfig;
    private Map<UUID, String> travelingPlayers = new HashMap<>();
    private Map<String, Location> trainLocations = new HashMap<>();
    private Map<String, Set<String>> stationConnections = new HashMap<>();

    @Override
    public void onEnable() {
        // Create plugin folder
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Initialize stations file
        createStationsFile();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Load existing stations
        loadStations();

        getLogger().info("TrainTransit Plugin enabled!");
    }

    @Override
    public void onDisable() {
        saveStations();
        getLogger().info("TrainTransit Plugin disabled!");
    }

    private void createStationsFile() {
        stationsFile = new File(getDataFolder(), "stations.yml");
        if (!stationsFile.exists()) {
            try {
                stationsFile.createNewFile();
                stationsConfig = YamlConfiguration.loadConfiguration(stationsFile);

                // Create default structure
                stationsConfig.createSection("stations");
                stationsConfig.createSection("train-locations");
                stationsConfig.createSection("train-lines");

                // Add example train lines (countries)
                stationsConfig.set("train-lines.usa.display-name", "United States");
                stationsConfig.set("train-lines.usa.color", "BLUE");
                stationsConfig.set("train-lines.canada.display-name", "Canada");
                stationsConfig.set("train-lines.canada.color", "RED");
                stationsConfig.set("train-lines.uk.display-name", "United Kingdom");
                stationsConfig.set("train-lines.uk.color", "GREEN");

                // Add example station
                stationsConfig.set("stations.example.world", "world");
                stationsConfig.set("stations.example.x", 0.0);
                stationsConfig.set("stations.example.y", 64.0);
                stationsConfig.set("stations.example.z", 0.0);
                stationsConfig.set("stations.example.display-name", "Example Station");
                stationsConfig.set("stations.example.train-line", "usa");
                stationsConfig.set("stations.example.npc-spawned", false);
                stationsConfig.set("stations.example.connections", Arrays.asList());

                // Add example train location
                stationsConfig.set("train-locations.example.world", "world");
                stationsConfig.set("train-locations.example.x", 0.0);
                stationsConfig.set("train-locations.example.y", 64.0);
                stationsConfig.set("train-locations.example.z", 5.0);

                stationsConfig.save(stationsFile);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create stations.yml", e);
            }
        } else {
            stationsConfig = YamlConfiguration.loadConfiguration(stationsFile);
        }
    }

    private void loadStations() {
        if (stationsConfig.getConfigurationSection("train-locations") != null) {
            for (String stationName : stationsConfig.getConfigurationSection("train-locations").getKeys(false)) {
                String worldName = stationsConfig.getString("train-locations." + stationName + ".world");
                double x = stationsConfig.getDouble("train-locations." + stationName + ".x");
                double y = stationsConfig.getDouble("train-locations." + stationName + ".y");
                double z = stationsConfig.getDouble("train-locations." + stationName + ".z");

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    trainLocations.put(stationName, new Location(world, x, y, z));
                }

                Bukkit.getConsoleSender().sendMessage(console_prefix + "Loading connections for " + stationName);
                List<String> connections = stationsConfig.getStringList("stations." + stationName + ".connections");
                stationConnections.put(stationName, new HashSet<>(connections));
            }
        }

        // Spawn NPCs for existing stations
        if (stationsConfig.getConfigurationSection("stations") != null) {
            for (String stationName : stationsConfig.getConfigurationSection("stations").getKeys(false)) {
                if (!stationsConfig.getBoolean("stations." + stationName + ".npc-spawned", false)) {
                    spawnStationNPC(stationName);
                }
            }
        }

        // Load station connections
//        if (stationsConfig.getConfigurationSection("stations") != null) {
//            for (String stationName : stationsConfig.getConfigurationSection("stations").getKeys(false)) {
//                Bukkit.getConsoleSender().sendMessage(console_prefix + "Loading connections for " + stationName);
//                List<String> connections = stationsConfig.getStringList("stations." + stationName + ".connections");
//                stationConnections.put(stationName, new HashSet<>(connections));
//            }
//        } else {
//            Bukkit.getConsoleSender().sendMessage(console_prefix + "No station connections found!");
//            if (stationsConfig == null) Bukkit.getConsoleSender().sendMessage(console_prefix + "stationsConfig is Null!!");
//        }
    }

    private void saveStations() {
        try {
            stationsConfig.save(stationsFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save stations.yml", e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("station")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.YELLOW + "Train Station Commands:");
                player.sendMessage(ChatColor.GRAY + "/station create <name> - Create a new station");
                player.sendMessage(ChatColor.GRAY + "/station remove <name> - Remove a station");
                player.sendMessage(ChatColor.GRAY + "/station list - List all stations");
                player.sendMessage(ChatColor.GRAY + "/station settrain <name> - Set train location for station");
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /station create <name>");
                    return true;
                }

                String stationName = args[1].toLowerCase();
                if (stationsConfig.contains("stations." + stationName)) {
                    player.sendMessage(ChatColor.RED + "Station already exists!");
                    return true;
                }

                String stationLine = args[2].toLowerCase();
//                if (!stationsConfig.contains("lines." + stationLine)) {
//                    player.sendMessage(ChatColor.RED + "Line does not exist!");
//                    return true;
//                }

                createStation((Player) sender, stationName, stationLine);
                return true;
            }

            if (args[0].equalsIgnoreCase("remove")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /station remove <name>");
                    return true;
                }

                String stationName = args[1].toLowerCase();
                if (!stationsConfig.contains("stations." + stationName)) {
                    player.sendMessage(ChatColor.RED + "Station does not exist!");
                    return true;
                }

                removeStation(stationName);
                player.sendMessage(ChatColor.GREEN + "Station removed!");
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                listStations(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("settrain")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /station settrain <name>");
                    return true;
                }

                String stationName = args[1].toLowerCase();
                if (!stationsConfig.contains("stations." + stationName)) {
                    player.sendMessage(ChatColor.RED + "Station does not exist!");
                    return true;
                }

                setTrainLocation(player, stationName);
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

            if (args[0].equalsIgnoreCase("lines")) {
                listTrainLines(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("addline")) {
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Usage: /station addline <line> <display-name> <color>");
                    return true;
                }

                addTrainLine(player, args[1].toLowerCase(), args[2], args[3].toUpperCase());
                return true;
            }
        }

        return false;
    }

    private void createStation(Player player, String stationName, String trainLine) {
        Location loc = player.getLocation();

        stationsConfig.set("stations." + stationName + ".world", loc.getWorld().getName());
        stationsConfig.set("stations." + stationName + ".x", loc.getX());
        stationsConfig.set("stations." + stationName + ".y", loc.getY());
        stationsConfig.set("stations." + stationName + ".z", loc.getZ());
        stationsConfig.set("stations." + stationName + ".display-name", formatStationName(stationName));
        stationsConfig.set("stations." + stationName + ".train-line", trainLine);
        stationsConfig.set("stations." + stationName + ".npc-spawned", false);
        stationsConfig.set("stations." + stationName + ".connections", Arrays.asList());

        // Set default train location (5 blocks away)
        Location trainLoc = loc.clone().add(5, 0, 0);
        stationsConfig.set("train-locations." + stationName + ".world", trainLoc.getWorld().getName());
        stationsConfig.set("train-locations." + stationName + ".x", trainLoc.getX());
        stationsConfig.set("train-locations." + stationName + ".y", trainLoc.getY());
        stationsConfig.set("train-locations." + stationName + ".z", trainLoc.getZ());

        trainLocations.put(stationName, trainLoc);
        stationConnections.put(stationName, new HashSet<>());
        saveStations();

        spawnStationNPC(stationName);

        String lineDisplayName = stationsConfig.getString("train-lines." + trainLine + ".display-name");
        player.sendMessage(ChatColor.GREEN + "Station created on " + lineDisplayName + " line! Use /station settrain " + stationName + " to customize train location.");
    }

    private void removeStation(String stationName) {
        stationsConfig.set("stations." + stationName, null);
        stationsConfig.set("train-locations." + stationName, null);
        trainLocations.remove(stationName);
        saveStations();

        // Remove NPC (you might want to store NPC references to remove them properly)
    }

    private void listStations(Player player) {
        if (stationsConfig.getConfigurationSection("stations") == null) {
            player.sendMessage(ChatColor.YELLOW + "No stations found!");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Available Stations:");
        for (String stationName : stationsConfig.getConfigurationSection("stations").getKeys(false)) {
            String displayName = stationsConfig.getString("stations." + stationName + ".display-name");
            String trainLine = stationsConfig.getString("stations." + stationName + ".train-line");
            String lineDisplayName = stationsConfig.getString("train-lines." + trainLine + ".display-name");
            ChatColor lineColor = ChatColor.valueOf(stationsConfig.getString("train-lines." + trainLine + ".color"));

            player.sendMessage(ChatColor.GRAY + "- " + displayName + " " + lineColor + "(" + lineDisplayName + ")");
        }
    }

    private void setTrainLocation(Player player, String stationName) {
        Location loc = player.getLocation();

        stationsConfig.set("train-locations." + stationName + ".world", loc.getWorld().getName());
        stationsConfig.set("train-locations." + stationName + ".x", loc.getX());
        stationsConfig.set("train-locations." + stationName + ".y", loc.getY());
        stationsConfig.set("train-locations." + stationName + ".z", loc.getZ());

        trainLocations.put(stationName, loc);
        saveStations();

        player.sendMessage(ChatColor.GREEN + "Train location set for " + formatStationName(stationName) + "!");
    }

    private void connectStations(Player player, String station1, String station2) {
        if (!stationsConfig.contains("stations." + station1)) {
            player.sendMessage(ChatColor.RED + "Station " + station1 + " does not exist!");
            return;
        }

        if (!stationsConfig.contains("stations." + station2)) {
            player.sendMessage(ChatColor.RED + "Station " + station2 + " does not exist!");
            return;
        }

        // Add bidirectional connection
        stationConnections.get(station1).add(station2);
        stationConnections.get(station2).add(station1);

        // Save to config
        stationsConfig.set("stations." + station1 + ".connections", new ArrayList<>(stationConnections.get(station1)));
        stationsConfig.set("stations." + station2 + ".connections", new ArrayList<>(stationConnections.get(station2)));

        saveStations();

        String display1 = stationsConfig.getString("stations." + station1 + ".display-name");
        String display2 = stationsConfig.getString("stations." + station2 + ".display-name");
        player.sendMessage(ChatColor.GREEN + "Connected " + display1 + " and " + display2 + "!");
    }

    private void disconnectStations(Player player, String station1, String station2) {
        if (!stationsConfig.contains("stations." + station1) || !stationsConfig.contains("stations." + station2)) {
            player.sendMessage(ChatColor.RED + "One or both stations do not exist!");
            return;
        }

        stationConnections.get(station1).remove(station2);
        stationConnections.get(station2).remove(station1);

        stationsConfig.set("stations." + station1 + ".connections", new ArrayList<>(stationConnections.get(station1)));
        stationsConfig.set("stations." + station2 + ".connections", new ArrayList<>(stationConnections.get(station2)));

        saveStations();

        String display1 = stationsConfig.getString("stations." + station1 + ".display-name");
        String display2 = stationsConfig.getString("stations." + station2 + ".display-name");
        player.sendMessage(ChatColor.GREEN + "Disconnected " + display1 + " and " + display2 + "!");
    }

    private void listTrainLines(Player player) {
        if (stationsConfig.getConfigurationSection("train-lines") == null) {
            player.sendMessage(ChatColor.YELLOW + "No train lines found!");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Available Train Lines:");
        for (String lineName : stationsConfig.getConfigurationSection("train-lines").getKeys(false)) {
            String displayName = stationsConfig.getString("train-lines." + lineName + ".display-name");
            ChatColor color = ChatColor.valueOf(stationsConfig.getString("train-lines." + lineName + ".color"));
            player.sendMessage(color + "- " + displayName + " (" + lineName + ")");
        }
    }

    private void addTrainLine(Player player, String lineName, String displayName, String colorName) {
        if (stationsConfig.contains("train-lines." + lineName)) {
            player.sendMessage(ChatColor.RED + "Train line already exists!");
            return;
        }

        try {
            ChatColor.valueOf(colorName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid color! Use: RED, BLUE, GREEN, YELLOW, etc.");
            return;
        }

        stationsConfig.set("train-lines." + lineName + ".display-name", displayName);
        stationsConfig.set("train-lines." + lineName + ".color", colorName);
        saveStations();

        ChatColor color = ChatColor.valueOf(colorName);
        player.sendMessage(ChatColor.GREEN + "Added train line: " + color + displayName);
    }

    private void spawnStationNPC(String stationName) {
        String worldName = stationsConfig.getString("stations." + stationName + ".world");
        double x = stationsConfig.getDouble("stations." + stationName + ".x");
        double y = stationsConfig.getDouble("stations." + stationName + ".y");
        double z = stationsConfig.getDouble("stations." + stationName + ".z");

        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        Location loc = new Location(world, x, y, z);
        Villager npc = (Villager) world.spawnEntity(loc, EntityType.VILLAGER);
        npc.setSilent(true);

        String displayName = stationsConfig.getString("stations." + stationName + ".display-name");
        String trainLine = stationsConfig.getString("stations." + stationName + ".train-line");
        String lineDisplayName = stationsConfig.getString("train-lines." + trainLine + ".display-name");
        ChatColor lineColor = ChatColor.valueOf(stationsConfig.getString("train-lines." + trainLine + ".color"));

        npc.setCustomName(ChatColor.GOLD + "Station Master - " + displayName + " " + lineColor + "(" + lineDisplayName + ")");
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);

        // Store that NPC was spawned
        stationsConfig.set("stations." + stationName + ".npc-spawned", true);
        saveStations();
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
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

    private String findStationByNPCLocation(Location npcLoc) {
        for (String stationName : stationsConfig.getConfigurationSection("stations").getKeys(false)) {
            String worldName = stationsConfig.getString("stations." + stationName + ".world");
            double x = stationsConfig.getDouble("stations." + stationName + ".x");
            double y = stationsConfig.getDouble("stations." + stationName + ".y");
            double z = stationsConfig.getDouble("stations." + stationName + ".z");

            if (npcLoc.getWorld().getName().equals(worldName) &&
                    npcLoc.distanceSquared(new Location(npcLoc.getWorld(), x, y, z)) < 25) { // Within 5 blocks
                return stationName;
            }
        }
        return null;
    }

    private void openStationMenu(Player player, String currentStation) {
        Set<String> connections = stationConnections.get(currentStation);
        if (connections == null || connections.isEmpty()) {
            player.sendMessage(ChatColor.RED + "This station has no connections!");
            return;
        }

        String currentLine = stationsConfig.getString("stations." + currentStation + ".train-line");

        // Check if there are international connections
        boolean hasInternational = false;
        boolean hasDomestic = false;

        for (String connectedStation : connections) {
            String connectedLine = stationsConfig.getString("stations." + connectedStation + ".train-line");
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

    private void openTravelTypeMenu(Player player, String currentStation) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_BLUE + "Travel Type - " +
                stationsConfig.getString("stations." + currentStation + ".display-name"));

        // Domestic travel option
        ItemStack domestic = new ItemStack(Material.EMERALD);
        ItemMeta domesticMeta = domestic.getItemMeta();
        domesticMeta.setDisplayName(ChatColor.GREEN + "Domestic Travel");
        String currentLine = stationsConfig.getString("stations." + currentStation + ".train-line");
        String lineDisplayName = stationsConfig.getString("train-lines." + currentLine + ".display-name");
        domesticMeta.setLore(Arrays.asList(ChatColor.GRAY + "Travel within " + lineDisplayName));
        domestic.setItemMeta(domesticMeta);
        inv.setItem(3, domestic);

        // International travel option
        ItemStack international = new ItemStack(Material.DIAMOND);
        ItemMeta internationalMeta = international.getItemMeta();
        internationalMeta.setDisplayName(ChatColor.BLUE + "International Travel");
        internationalMeta.setLore(Arrays.asList(ChatColor.GRAY + "Travel to other countries"));
        international.setItemMeta(internationalMeta);
        inv.setItem(5, international);

        player.openInventory(inv);
    }

    private void openDestinationMenu(Player player, String currentStation, String travelType) {
        Set<String> connections = stationConnections.get(currentStation);
        String currentLine = stationsConfig.getString("stations." + currentStation + ".train-line");

        // Filter connections based on travel type
        Set<String> filteredConnections = new HashSet<>();
        for (String connectedStation : connections) {
            String connectedLine = stationsConfig.getString("stations." + connectedStation + ".train-line");
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

        int slot = 0;
        for (String stationName : filteredConnections) {
            String trainLine = stationsConfig.getString("stations." + stationName + ".train-line");
            String lineDisplayName = stationsConfig.getString("train-lines." + trainLine + ".display-name");
            ChatColor lineColor = ChatColor.valueOf(stationsConfig.getString("train-lines." + trainLine + ".color"));

            ItemStack item = new ItemStack(travelType.equals("domestic") ? Material.RAIL : Material.POWERED_RAIL);
            ItemMeta meta = item.getItemMeta();

            String displayName = stationsConfig.getString("stations." + stationName + ".display-name");
            meta.setDisplayName(ChatColor.YELLOW + displayName);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click to travel to " + displayName,
                    lineColor + "Line: " + lineDisplayName
            ));

            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

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
            for (String name : stationsConfig.getConfigurationSection("stations").getKeys(false)) {
                if (stationsConfig.getString("stations." + name + ".display-name").equals(currentStation)) {
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
                            event.getCurrentItem().getType() != Material.POWERED_RAIL)) return;

            String displayName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            // Find station by display name
            String stationName = null;
            for (String name : stationsConfig.getConfigurationSection("stations").getKeys(false)) {
                if (stationsConfig.getString("stations." + name + ".display-name").equals(displayName)) {
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
            startTrainJourney(player, stationName, travelType);
        }
    }

    private void startTrainJourney(Player player, String destinationStation, String travelType) {
        if (travelingPlayers.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already traveling!");
            return;
        }

        // Get train location for this station
        Location trainLoc = trainLocations.get(destinationStation);
        if (trainLoc == null) {
            player.sendMessage(ChatColor.RED + "Train location not set for this station!");
            return;
        }

        travelingPlayers.put(player.getUniqueId(), destinationStation);

        // Teleport to train location first
        Location sourceLoc = player.getLocation();
        player.teleport(trainLoc);

        String destinationDisplayName = stationsConfig.getString("stations." + destinationStation + ".display-name");
        String destinationLine = stationsConfig.getString("stations." + destinationStation + ".train-line");
        String lineDisplayName = stationsConfig.getString("train-lines." + destinationLine + ".display-name");

//        if (travelType.equals("international")) {
//            player.sendMessage(ChatColor.GOLD + "All aboard the international express!");
//            player.sendMessage(ChatColor.YELLOW + "Traveling to " + destinationDisplayName + " in " + lineDisplayName);
//        } else {
//            player.sendMessage(ChatColor.GREEN + "All aboard! Traveling to " + destinationDisplayName);
//        }

        // Play sound and effects
        player.playSound(player.getLocation(), Sound.ENTITY_MINECART_RIDING, 1.0f, 1.0f);

        // Travel Time based on distance
//        int travelTime = travelType.equals("international") ? 100 : 60; // 5 seconds vs 3 seconds
        String worldName = stationsConfig.getString("stations." + destinationStation + ".world");
        double x = stationsConfig.getDouble("stations." + destinationStation + ".x");
        double y = stationsConfig.getDouble("stations." + destinationStation + ".y");
        double z = stationsConfig.getDouble("stations." + destinationStation + ".z");

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Destination world not found!");
            travelingPlayers.remove(player.getUniqueId());
            return;
        }
        Location destinationLoc = new Location(world, x, y, z);
        int travelTime = (int) sourceLoc.distance(destinationLoc);

        if (travelType.equals("international")) {
            player.sendMessage(ChatColor.GREEN + "You've boarded the train to " + ChatColor.GOLD + destinationDisplayName + ChatColor.GREEN + " in " + ChatColor.GOLD + lineDisplayName + ChatColor.GREEN + ". You will arrive in " + ChatColor.GOLD + travelTime / 20 + ChatColor.GREEN + " seconds");
        } else {
            player.sendMessage(ChatColor.GREEN + "You've boarded the train to " + ChatColor.GOLD + destinationDisplayName + ChatColor.GREEN + ". You will arrive in " + ChatColor.GOLD + travelTime / 20 + ChatColor.GREEN + " seconds");
        }

        // Start journey with effects
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= travelTime) {
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
                    Sound sound = travelType.equals("international") ? Sound.BLOCK_PORTAL_AMBIENT : Sound.ENTITY_MINECART_RIDING;
                    player.playSound(loc, sound, 0.5f, 1.0f);
                }

                ticks++;
            }
        }.runTaskTimer(this, 0, 1);
    }

    private void completeJourney(Player player, String stationName, String travelType) {
        // Get station location
        String worldName = stationsConfig.getString("stations." + stationName + ".world");
        double x = stationsConfig.getDouble("stations." + stationName + ".x");
        double y = stationsConfig.getDouble("stations." + stationName + ".y");
        double z = stationsConfig.getDouble("stations." + stationName + ".z");

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

        String displayName = stationsConfig.getString("stations." + stationName + ".display-name");
        String trainLine = stationsConfig.getString("stations." + stationName + ".train-line");
        String lineDisplayName = stationsConfig.getString("train-lines." + trainLine + ".display-name");

        if (travelType.equals("international")) {
            player.sendMessage(ChatColor.GOLD + "Welcome to " + lineDisplayName + "!");
            player.sendMessage(ChatColor.YELLOW + "Arrived at " + displayName + "!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Arrived at " + displayName + "!");
        }

        travelingPlayers.remove(player.getUniqueId());
    }

    private String formatStationName(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase().replace("_", " ");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerDamage(EntityDamageEvent event) {
        // Check if the entity is a villager
        if (event.getEntityType() != EntityType.VILLAGER) {
            return;
        }

        Villager villager = (Villager) event.getEntity();

        // Check if the villager has a custom name
        if (villager.getCustomName() == null) {
            return;
        }

        // Check if this is the protected villager
        if (villager.getCustomName().contains("Station Master")) {
            // Cancel the damage event to prevent the villager from being hurt
            event.setCancelled(true);

            // Optional: Send a message to the player if they're the cause
//            if (event.getDamageSource().getCausingEntity() != null &&
//                    event.getDamageSource().getCausingEntity().getType() == EntityType.PLAYER) {
//                event.getDamageSource().getCausingEntity().sendMessage("§cThis villager is protected and cannot be harmed!");
//            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerDeath(EntityDeathEvent event) {
        // Double safety check - prevent death event for protected villagers
        if (event.getEntityType() != EntityType.VILLAGER) {
            return;
        }

        Villager villager = (Villager) event.getEntity();

        if (villager.getCustomName() != null &&
                villager.getCustomName().contains("Station Master")) {
            // Cancel the death event
            event.setCancelled(true);

            // Restore the villager's health
            villager.setHealth(villager.getMaxHealth());
        }
    }

    public static String console_prefix = ChatColor.GRAY + "[" + ChatColor.GREEN + "FancyTrains" + ChatColor.GRAY + "] " + ChatColor.RESET;

}
