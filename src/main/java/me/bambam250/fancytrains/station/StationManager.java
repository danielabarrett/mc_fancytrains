package me.bambam250.fancytrains.station;

import me.bambam250.fancytrains.Fancytrains;
import me.bambam250.fancytrains.objects.Line;
import me.bambam250.fancytrains.objects.Station;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages stations and lines, including event handling and in-memory storage.
 */
public class StationManager implements Listener {
    private final Fancytrains plugin;

    private final List<Station> stations = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();

    /**
     * Helper class to track pending travel confirmations.
     */
    public static class PendingTravel {
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
    public Map<UUID, String> travelingPlayers = new HashMap<>();

    public StationManager(Fancytrains plugin) {
        this.plugin = plugin;
        loadStations();
    }

    public void saveStations() {
        plugin.configManager.saveStations();
    }

    /**
     * Loads stations and lines from the configuration file into memory.
     */
    public void loadStations() {
        stations.clear();
        lines.clear();

        var ftConfig = plugin.configManager.getFtConfig();
        // Load lines
        if (ftConfig.getConfigurationSection("lines") != null) {
            for (String lineName : ftConfig.getConfigurationSection("lines").getKeys(false)) {
                lines.add(new Line(lineName));
            }
        }

        // Load stations
        if (ftConfig.getConfigurationSection("stations") != null) {
            for (String stationName : ftConfig.getConfigurationSection("stations").getKeys(false)) {
                Bukkit.getLogger().log(Level.INFO, "Loading station: " + stationName);
                Line stationLine = getLine(ftConfig.getString("stations." + stationName + ".line"));
                Station station = new Station(stationName, stationLine);
                stations.add(station);
                if (stationLine != null) {
                    stationLine.addStation(station);
                }
            }
            // Load connections
            for (Station station : stations) {
                List<String> connections = ftConfig.getStringList("stations." + station.getName() + ".connections");
                for (String conn : connections) {
                    Station connectedStation = getStation(conn);
                    if (connectedStation != null) {
                        station.addConnection(connectedStation);
                    }
                }
            }
        }
    }

    // --- Event Handlers and Logic ---

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        if (villager.getCustomName() == null || !villager.getCustomName().contains("Station Master")) return;

        event.setCancelled(true);

        // Find which station this NPC belongs to (by UUID)
        String npcStation = null;
        UUID npcId = villager.getUniqueId();
        for (Station station : stations) {
            String uuidStr = station.getStationMaster().getUniqueId().toString();
            if (uuidStr != null && uuidStr.equals(npcId.toString())) {
                npcStation = station.getName();
                break;
            }
        }
        if (npcStation == null) {
            event.getPlayer().sendMessage(ChatColor.RED + "Station not found!");
            return;
        }

        plugin.GUIManager.openStationMenu(event.getPlayer(), npcStation);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Delegate to GUIManager for menu logic
        plugin.GUIManager.handleInventoryClick(event, player, title, this);
    }

    /**
     * Adds a pending traveler for confirmation.
     * @param player The player.
     * @param destinationStation The destination station.
     * @param travelType The type of travel.
     * @param travelTimeSeconds The travel time in seconds.
     */
    public void addPendingTraveler(Player player, String destinationStation, String travelType, int travelTimeSeconds) {
        pendingTravel.put(player.getUniqueId(), new PendingTravel(destinationStation, travelType, travelTimeSeconds));
    }

    /**
     * Starts a train journey for a player.
     * @param player The player.
     * @param destinationStation The destination station.
     * @param travelType The type of travel.
     * @param travelTimeTicks The travel time in ticks.
     */
    public void startTrainJourney(Player player, String destinationStation, String travelType, int travelTimeTicks) {
        if (travelingPlayers.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already traveling!");
            return;
        }

        Station destStation = getStation(destinationStation);
        if (destStation == null) {
            player.sendMessage(ChatColor.RED + "Destination station not found!");
            return;
        }
        Line line = destStation.getLine();
        if (line == null || line.getTrainLocation() == null) {
            player.sendMessage(ChatColor.RED + "Train location not set for this line!");
            return;
        }

        travelingPlayers.put(player.getUniqueId(), destinationStation);

        // Teleport to train location for the line
        player.teleport(line.getTrainLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);

        String destinationDisplayName = destStation.getDisplayName();
        String lineDisplayName = line.getDisplayName();

        if (travelType.equals("international")) {
            player.sendMessage(ChatColor.GREEN + "You've boarded the train to " + ChatColor.GOLD + destinationDisplayName + ChatColor.GREEN + " in " + ChatColor.GOLD + lineDisplayName + ChatColor.GREEN + ". You will arrive in " + ChatColor.GOLD + (travelTimeTicks / 20) + ChatColor.GREEN + " seconds");
        } else {
            player.sendMessage(ChatColor.GREEN + "You've boarded the train to " + ChatColor.GOLD + destinationDisplayName + ChatColor.GREEN + ". You will arrive in " + ChatColor.GOLD + (travelTimeTicks / 20) + ChatColor.GREEN + " seconds");
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= travelTimeTicks) {
                    completeJourney(player, destinationStation, travelType);
                    cancel();
                    return;
                }
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
     * Completes a train journey for a player.
     * @param player The player.
     * @param stationName The destination station name.
     * @param travelType The type of travel.
     */
    public void completeJourney(Player player, String stationName, String travelType) {
        Station destStation = getStation(stationName);
        if (destStation == null) {
            player.sendMessage(ChatColor.RED + "Destination station not found!");
            travelingPlayers.remove(player.getUniqueId());
            return;
        }
        Location destination = destStation.getLocation();
        player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);

        player.playSound(destination, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        destination.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, destination, 20);

        String displayName = destStation.getDisplayName();
        Line line = destStation.getLine();
        String lineDisplayName = line != null ? line.getDisplayName() : "";

        if (travelType.equals("international")) {
            player.sendMessage(ChatColor.GOLD + "Welcome to " + lineDisplayName + "!");
            player.sendMessage(ChatColor.YELLOW + "Arrived at " + displayName + "!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Arrived at " + displayName + "!");
        }

        travelingPlayers.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (!isStationMasterNPC(villager)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (!isStationMasterNPC(villager)) return;
        event.setCancelled(true);
    }

    private boolean isStationMasterNPC(Villager villager) {
        UUID uuid = villager.getUniqueId();
        for (Station station : stations) {
            String uuidStr = station.getStationMaster().getUniqueId().toString();
            if (uuidStr != null && uuidStr.equals(uuid.toString())) {
                return true;
            }
        }
        return villager.getCustomName() != null && villager.getCustomName().contains("Station Master");
    }

    // --- In-memory containers and accessors ---

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

    public void removeStation(Station station) {
        this.stations.remove(station);
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

    public void removeLine(Line line) {
        this.lines.remove(line);
    }

    /**
     * Get the PendingTravel object for a player.
     * @param traveller The player's UUID.
     * @return The PendingTravel object, or null if none exists.
     */
    public PendingTravel getPendingTravel(UUID traveller) {
        return pendingTravel.get(traveller);
    }

    /**
     * Add or update a PendingTravel for a player.
     * @param traveller The player's UUID.
     * @param pending The PendingTravel object.
     */
    public void addPendingTravel(UUID traveller, PendingTravel pending) {
        pendingTravel.put(traveller, pending);
    }

    /**
     * Remove a PendingTravel for a player.
     * @param traveller The player's UUID.
     */
    public void removePendingTravel(UUID traveller) {
        pendingTravel.remove(traveller);
    }

}
