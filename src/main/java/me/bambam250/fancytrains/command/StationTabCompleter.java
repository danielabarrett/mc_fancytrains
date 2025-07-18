package me.bambam250.fancytrains.command;

import me.bambam250.fancytrains.Fancytrains;
import me.bambam250.fancytrains.objects.Line;
import me.bambam250.fancytrains.objects.Station;
import me.bambam250.fancytrains.station.StationManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class StationTabCompleter implements TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "list", "create", "remove", "connect", "disconnect", "modify"
    );

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        Fancytrains plugin = Fancytrains.getPlugin(Fancytrains.class);
        StationManager stationManager = plugin.stationManager;

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
                case "modify":
                    completions.add("<station> <attribute> [<value>]");
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
            for (Line line : stationManager.getLines()) {
                if (line.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(line.getName());
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
                for (Station station : stationManager.getStations()) {
                    if (station.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(station.getName());
                    }
                }
                return completions;
            }
            // Tab complete lines for "/station list <line>"
            if (sub.equals("list")) {
                for (Line line : stationManager.getLines()) {
                    if (line.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(line.getName());
                    }
                }
                return completions;
            }
        }

        // Tab complete station names for connect/disconnect (both args)
        if (args.length == 2 || args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("connect") || sub.equals("disconnect")) {
                for (Station station : stationManager.getStations()) {
                    if (args.length == 2 && station.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(station.getName());
                    } else if (args.length == 3 && station.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(station.getName());
                    }
                }
                return completions;
            }
        }

        // Tab complete station names for /station modify <station> <attribute>
        if (args.length == 2 && args[0].equalsIgnoreCase("modify")) {
            for (Station station : stationManager.getStations()) {
                if (station.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(station.getName());
                }
            }
            return completions;
        }

        // Tab complete attributes for /station modify <station> <attribute> [<value>]
        if (args.length == 3 && args[0].equalsIgnoreCase("modify")) {
            List<String> attributes = Arrays.asList("display-name", "line", "npc-location", "station-location");
            for (String attr : attributes) {
                if (attr.startsWith(args[2].toLowerCase())) {
                    completions.add(attr);
                }
            }
            return completions;
        }

        // Tab complete line names for /station modify <station> line <value>
        if (args.length == 4 && args[0].equalsIgnoreCase("modify") && args[2].equalsIgnoreCase("line")) {
            for (Line line : stationManager.getLines()) {
                if (line.getName().toLowerCase().startsWith(args[3].toLowerCase())) {
                    completions.add(line.getName());
                }
            }
            return completions;
        }

        // No color completion for station modify anymore

        return completions;
    }
}
