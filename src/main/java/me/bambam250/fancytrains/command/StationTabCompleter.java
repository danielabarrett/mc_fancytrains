package me.bambam250.fancytrains.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StationTabCompleter implements TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "list", "create", "remove", "connect", "disconnect"
    );

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            // Show usage prompt before a space is added after the subcommand
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "create":
                    completions.add("<station_name> <line> <display-name>");
                    break;
                case "remove":
                    completions.add("<station_name>");
                    break;
                case "connect":
                case "disconnect":
                    completions.add("<station1> <station2>");
                    break;
                case "list":
                    completions.add("[line]");
                    break;
            }
            return completions;
        }

        // Prompt for station name for /station create <name>
        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            completions.add("<station_name>");
            return completions;
        }

        // Prompt for line for /station create <name> <line>
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            me.bambam250.fancytrains.Fancytrains plugin = me.bambam250.fancytrains.Fancytrains.getPlugin(me.bambam250.fancytrains.Fancytrains.class);
            if (plugin.configManager.ftConfig.getConfigurationSection("lines") != null) {
                for (String line : plugin.configManager.ftConfig.getConfigurationSection("lines").getKeys(false)) {
                    if (line.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(line);
                    }
                }
            }
            return completions;
        }

        // Prompt for display name for /station create <name> <line> <display-name>
        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            completions.add("<Display Name>");
            return completions;
        }

        // Tab complete station names for relevant subcommands
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("remove")) {
                me.bambam250.fancytrains.Fancytrains plugin = me.bambam250.fancytrains.Fancytrains.getPlugin(me.bambam250.fancytrains.Fancytrains.class);
                if (plugin.configManager.ftConfig.getConfigurationSection("stations") != null) {
                    for (String station : plugin.configManager.ftConfig.getConfigurationSection("stations").getKeys(false)) {
                        if (station.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(station);
                        }
                    }
                }
                return completions;
            }
            // Tab complete lines for "/station list <line>"
            if (sub.equals("list")) {
                me.bambam250.fancytrains.Fancytrains plugin = me.bambam250.fancytrains.Fancytrains.getPlugin(me.bambam250.fancytrains.Fancytrains.class);
                if (plugin.configManager.ftConfig.getConfigurationSection("lines") != null) {
                    for (String line : plugin.configManager.ftConfig.getConfigurationSection("lines").getKeys(false)) {
                        if (line.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(line);
                        }
                    }
                }
                return completions;
            }
        }

        // Tab complete station names for connect/disconnect (both args)
        if (args.length == 2 || args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("connect") || sub.equals("disconnect")) {
                me.bambam250.fancytrains.Fancytrains plugin = me.bambam250.fancytrains.Fancytrains.getPlugin(me.bambam250.fancytrains.Fancytrains.class);
                if (plugin.configManager.ftConfig.getConfigurationSection("stations") != null) {
                    for (String station : plugin.configManager.ftConfig.getConfigurationSection("stations").getKeys(false)) {
                        if (args.length == 2 && station.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(station);
                        } else if (args.length == 3 && station.toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(station);
                        }
                    }
                }
                return completions;
            }
        }

        return completions;
    }
}
