package me.bambam250.fancytrains.command;

import me.bambam250.fancytrains.Fancytrains;
import me.bambam250.fancytrains.objects.Line;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LineTabCompleter implements TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "list", "create", "remove", "setflag", "modify", "settrain"
    );

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        Fancytrains plugin = Fancytrains.getPlugin(Fancytrains.class);
        List<Line> lines = plugin.stationManager.getLines();

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
                    completions.add("<line> <color> <display-name>");
                    break;
                case "remove":
                    completions.add("<line>");
                    break;
                case "setflag":
                    completions.add("<line>");
                    break;
                case "modify":
                    completions.add("<line> <attribute> <value>");
                    break;
                case "settrain":
                    completions.add("<line>");
                    break;
            }
            return completions;
        }

        // Prompt for line name for /line create <name>
        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            completions.add("<line_name>");
            return completions;
        }

        // Tab complete color for /line create <name> <color> <display-name>
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            for (ChatColor color : ChatColor.values()) {
                if (color.isColor() && color.name().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(color.name());
                }
            }
            return completions;
        }

        // Prompt for display name for /line create <name> <color> <display-name>
        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            completions.add("<Display Name>");
            return completions;
        }

        // Tab complete line names for relevant subcommands
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("remove") || sub.equals("setflag") || sub.equals("modify") || sub.equals("settrain")) {
                for (Line line : lines) {
                    if (line.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(line.getName());
                    }
                }
                return completions;
            }
        }

        // Tab complete attributes for /line modify <line> <attribute> <value>
        if (args.length == 3 && args[0].equalsIgnoreCase("modify")) {
            List<String> attributes = Arrays.asList("display-name", "color");
            for (String attr : attributes) {
                if (attr.startsWith(args[2].toLowerCase())) {
                    completions.add(attr);
                }
            }
            return completions;
        }

        // Tab complete color for /line modify <line> color <value>
        if (args.length == 4 && args[0].equalsIgnoreCase("modify") && args[2].equalsIgnoreCase("color")) {
            for (ChatColor color : ChatColor.values()) {
                if (color.isColor() && color.name().toLowerCase().startsWith(args[3].toLowerCase())) {
                    completions.add(color.name());
                }
            }
            return completions;
        }

        return completions;
    }
}
