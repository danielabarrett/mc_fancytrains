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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LineCommand implements CommandExecutor {
    private final Fancytrains plugin;
    private final StationManager stationManager;

    public LineCommand(Fancytrains pl) {
        this.plugin = pl;
        this.stationManager = pl.stationManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Train Line Commands:");
            player.sendMessage(ChatColor.GREEN + "/line list" + ChatColor.GRAY + " - List all train lines");
            player.sendMessage(ChatColor.GREEN + "/line create" + ChatColor.GRAY + " <line> <color> <display-name> - Create a new line");
            player.sendMessage(ChatColor.GREEN + "/line remove" + ChatColor.GRAY + " <line> - Remove a line");
            player.sendMessage(ChatColor.GREEN + "/line setflag" + ChatColor.GRAY + " <line> - Set the flag/banner for a line");
            player.sendMessage(ChatColor.GREEN + "/line modify" + ChatColor.GRAY + " <line> <attribute> <value> - Modify a line's attribute");
            player.sendMessage(ChatColor.GREEN + "/line settrain" + ChatColor.GRAY + " <line> - Set the train location for a line");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            listLines(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 4) {
                player.sendMessage(ChatColor.RED + "Usage: /line create <line> <color> <display-name>");
                return true;
            }

            String lineName = args[1].toLowerCase();
            if (stationManager.isLine(lineName)) {
                player.sendMessage(ChatColor.RED + "Train line already exists!");
                return true;
            }

            String colorName = args[2].toUpperCase();
            try {
                ChatColor.valueOf(colorName);
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid color! Use: RED, BLUE, GREEN, YELLOW, etc.");
                return true;
            }

            StringBuilder displayNameBuilder = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                if (i > 3) displayNameBuilder.append(" ");
                displayNameBuilder.append(args[i]);
            }
            String displayName = displayNameBuilder.toString();

            Line line = new Line(lineName, colorName, displayName, null);
            stationManager.addLine(line);

            player.sendMessage(ChatColor.GREEN + "Added train line: " + ChatColor.valueOf(colorName) + displayName);
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /line remove <line name>");
                return true;
            }
            removeLine(player, args[1].toLowerCase());
            return true;
        }

        if (args[0].equalsIgnoreCase("setflag")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /line setflag <line name>");
                return true;
            }
            Line line = stationManager.getLine(args[1].toLowerCase());
            if (line == null) {
                player.sendMessage(ChatColor.RED + "Line does not exist!");
                return true;
            }
            ItemStack flag = player.getInventory().getItemInMainHand();
            if (!(flag.getItemMeta() instanceof BannerMeta)) {
                player.sendMessage(ChatColor.RED + "Please hold a banner when using this command");
                return true;
            }
            line.setFlag(flag);
            player.sendMessage(ChatColor.GREEN + "Saved this flag for " + line.getName());
            return true;
        }

        if (args[0].equalsIgnoreCase("modify")) {
            if (args.length < 4) {
                player.sendMessage(ChatColor.RED + "Usage: /line modify <line name> <attribute> <value>");
                return true;
            }
            String lineName = args[1].toLowerCase();
            String attribute = args[2].toLowerCase();
            StringBuilder valueBuilder = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                if (i > 3) valueBuilder.append(" ");
                valueBuilder.append(args[i]);
            }
            String value = valueBuilder.toString();
            modifyLine(player, lineName, attribute, value);
            return true;
        }

        if (args[0].equalsIgnoreCase("settrain")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /line settrain <line>");
                return true;
            }

            String lineName = args[1].toLowerCase();
            Line line = stationManager.getLine(lineName);
            if (line == null) {
                player.sendMessage(ChatColor.RED + "Line does not exist!");
                return true;
            }

            setTrainLocation(player, line);
            return true;
        }

        return false;
    }

    private void listLines(Player player) {
        List<Line> lines = stationManager.getLines();
        if (lines.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No train lines found!");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Available Train Lines:");
        for (Line line : lines) {
            player.sendMessage(line.getColor() + "- " + line.getDisplayName() + " (" + line.getName() + ")");
        }
    }

    private void removeLine(Player player, String lineName) {
        Line line = stationManager.getLine(lineName);
        if (line == null) {
            player.sendMessage(ChatColor.RED + "Train line does not exist!");
            return;
        }

        // Prevent removal if any station is assigned to this line
        for (Station station : stationManager.getStations()) {
            if (lineName.equalsIgnoreCase(station.getLine().getName())) {
                player.sendMessage(ChatColor.RED + "Cannot remove line: There are stations assigned to this line (" + station.getName() + ").");
                return;
            }
        }

        stationManager.removeLine(line);
        player.sendMessage(ChatColor.GREEN + "Removed train line: " + lineName);
    }

    private void modifyLine(Player player, String lineName, String attribute, String value) {
        Line line = stationManager.getLine(lineName);
        if (line == null) {
            player.sendMessage(ChatColor.RED + "Train line does not exist!");
            return;
        }

        switch (attribute) {
            case "display-name":
                line.setDisplayName(value);
                player.sendMessage(ChatColor.GREEN + "Set display name for " + lineName + " to " + value);
                break;
            case "color":
                try {
                    line.setColor(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Invalid color! Use: RED, BLUE, GREEN, YELLOW, etc.");
                    return;
                }
                player.sendMessage(ChatColor.GREEN + "Set color for " + lineName + " to " + value.toUpperCase());
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown attribute. Supported: display-name, color");
        }
    }

    private void setTrainLocation(Player player, Line line) {
        Location loc = player.getLocation();
        line.setTrainLocation(loc);
        player.sendMessage(ChatColor.GREEN + "Train location set for line " + line.getName() + "!");
    }
}
