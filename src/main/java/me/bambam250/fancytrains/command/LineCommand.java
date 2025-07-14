package me.bambam250.fancytrains.command;

import me.bambam250.fancytrains.Fancytrains;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.jetbrains.annotations.NotNull;

public class LineCommand implements CommandExecutor {
    Fancytrains plugin;

    public LineCommand(Fancytrains pl) {
        plugin = pl;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

//        Player player = (Player) sender;

        if (args[0].equalsIgnoreCase("list")) {
            listLines(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 4) {
                player.sendMessage(ChatColor.RED + "Usage: /line create <line> <color> <display-name>");
                return true;
            }

            // Concatenate args[3] and onward into a displayName string
            StringBuilder displayNameBuilder = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                if (i > 3) displayNameBuilder.append(" ");
                displayNameBuilder.append(args[i]);
            }
            String displayName = displayNameBuilder.toString();

            addLine(player, args[1].toLowerCase(), displayName, args[2].toUpperCase());
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
            ItemStack flag = player.getInventory().getItemInMainHand();
            if (!(flag.getItemMeta() instanceof BannerMeta)) {
                player.sendMessage(ChatColor.RED + "Please hold a banner when using this command");
                return true;
            }
            setLineFlag(player, args[1], flag);
            player.sendMessage(ChatColor.GREEN + "Saved this flag for " + args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("modify")) {
            if (args.length < 4) {
                player.sendMessage(ChatColor.RED + "Usage: /line modify <line name> <attribute> <value>");
                return true;
            }
            String lineName = args[1].toLowerCase();
            String attribute = args[2].toLowerCase();
            // Concatenate the rest of the args as the value (to allow spaces)
            StringBuilder valueBuilder = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                if (i > 3) valueBuilder.append(" ");
                valueBuilder.append(args[i]);
            }
            String value = valueBuilder.toString();
            modifyLine(player, lineName, attribute, value);
            return true;
        }

        return false;
    }

    private void listLines(Player player) {
        if (plugin.configManager.ftConfig.getConfigurationSection("lines") == null) {
            player.sendMessage(ChatColor.YELLOW + "No train lines found!");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Available Train Lines:");
        for (String lineName : plugin.configManager.ftConfig.getConfigurationSection("lines").getKeys(false)) {
            String displayName = plugin.configManager.ftConfig.getString("lines." + lineName + ".display-name");
            ChatColor color = ChatColor.valueOf(plugin.configManager.ftConfig.getString("lines." + lineName + ".color"));
            player.sendMessage(color + "- " + displayName + " (" + lineName + ")");
        }
    }

    private void addLine(Player player, String lineName, String displayName, String colorName) {
        if (plugin.configManager.ftConfig.contains("lines." + lineName)) {
            player.sendMessage(ChatColor.RED + "Train line already exists!");
            return;
        }

        try {
            ChatColor.valueOf(colorName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid color! Use: RED, BLUE, GREEN, YELLOW, etc.");
            return;
        }

        plugin.configManager.ftConfig.set("lines." + lineName + ".display-name", displayName);
        plugin.configManager.ftConfig.set("lines." + lineName + ".color", colorName);
        plugin.configManager.saveStations();

        ChatColor color = ChatColor.valueOf(colorName);
        player.sendMessage(ChatColor.GREEN + "Added train line: " + color + displayName);
    }

    private void removeLine(Player player, String lineName) {
        if (!plugin.configManager.ftConfig.contains("lines." + lineName)) {
            player.sendMessage(ChatColor.RED + "Train line does not exist!");
            return;
        }

        // Prevent removal if any station is assigned to this line
        if (plugin.configManager.ftConfig.getConfigurationSection("stations") != null) {
            for (String station : plugin.configManager.ftConfig.getConfigurationSection("stations").getKeys(false)) {
                String stationLine = plugin.configManager.ftConfig.getString("stations." + station + ".line");
                if (lineName.equalsIgnoreCase(stationLine)) {
                    player.sendMessage(ChatColor.RED + "Cannot remove line: There are stations assigned to this line (" + station + ").");
                    return;
                }
            }
        }

        // Remove the line from config
        plugin.configManager.ftConfig.set("lines." + lineName, null);

        plugin.configManager.saveStations();
        player.sendMessage(ChatColor.GREEN + "Removed train line: " + lineName);
    }

    private void setLineFlag(Player player, String lineName, ItemStack flag) {
        plugin.stationManager.saveBannerToConfig(flag, "lines." + lineName + ".flag");
        plugin.configManager.saveStations();
        player.sendMessage(ChatColor.GREEN + "Added flag to " + lineName);
    }

    private void modifyLine(Player player, String lineName, String attribute, String value) {
        if (!plugin.configManager.ftConfig.contains("lines." + lineName)) {
            player.sendMessage(ChatColor.RED + "Train line does not exist!");
            return;
        }

        switch (attribute) {
            case "display-name":
                plugin.configManager.ftConfig.set("lines." + lineName + ".display-name", value);
                plugin.configManager.saveStations();
                player.sendMessage(ChatColor.GREEN + "Set display name for " + lineName + " to " + value);
                break;
            case "color":
                try {
                    ChatColor.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Invalid color! Use: RED, BLUE, GREEN, YELLOW, etc.");
                    return;
                }
                plugin.configManager.ftConfig.set("lines." + lineName + ".color", value.toUpperCase());
                plugin.configManager.saveStations();
                player.sendMessage(ChatColor.GREEN + "Set color for " + lineName + " to " + value.toUpperCase());
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown attribute. Supported: display-name, color");
        }
    }

}
