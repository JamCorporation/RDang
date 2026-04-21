package ru.truhot.rdang.comands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.truhot.rdang.RDang;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RTabCompleter implements TabCompleter {

    private final RDang plugin;

    public RTabCompleter(RDang plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {

        if (!sender.hasPermission("rdang.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Stream.of("additem", "spawn", "givekey", "reload", "schem", "undo", "compass", "list", "admins", "update")
                    .filter(arg -> arg.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        String subCommand = args[0].toLowerCase();
        if (args.length == 2 && subCommand.equals("schem")) {
            List<String> schemFiles = new ArrayList<>();
            List<File> foldersToScan = new ArrayList<>();

            foldersToScan.add(new File(plugin.getDataFolder(), "schem"));

            org.bukkit.plugin.Plugin fawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
            if (fawe != null) foldersToScan.add(new File(fawe.getDataFolder(), "schematics"));
            org.bukkit.plugin.Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");
            if (we != null) foldersToScan.add(new File(we.getDataFolder(), "schematics"));
            for (File folder : foldersToScan) {
                if (folder.exists() && folder.isDirectory()) {
                    File[] files = folder.listFiles((dir, name) ->
                            name.toLowerCase().endsWith(".schem") || name.toLowerCase().endsWith(".schematic"));
                    if (files != null) {
                        for (File file : files) {
                            if (!schemFiles.contains(file.getName())) {
                                schemFiles.add(file.getName());
                            }
                        }
                    }
                }
            }

            return schemFiles.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (subCommand.equals("additem")) {
            if (args.length == 2) {
                return Stream.of("10", "25", "50", "75", "100")
                        .filter(arg -> arg.startsWith(args[1]))
                        .collect(Collectors.toList());
            }
            if (args.length == 3) {
                return Stream.of("1", "16", "32", "64")
                        .filter(arg -> arg.startsWith(args[2]))
                        .collect(Collectors.toList());
            }
            if (args.length == 4) {
                return Stream.of("1", "16", "32", "64")
                        .filter(arg -> arg.startsWith(args[3]))
                        .collect(Collectors.toList());
            }
        }

        if (subCommand.equals("spawn")) {
            if (args.length == 2) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("<количество>");
                return suggestions.stream()
                        .filter(arg -> arg.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 3) {
                return Bukkit.getWorlds().stream()
                        .map(org.bukkit.World::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admins")) {
            return Stream.of("test", "remove", "loot")
                    .filter(arg -> arg.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (subCommand.equals("givekey")) {
            if (args.length == 2) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 3) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("<количество>");
                return suggestions.stream()
                        .filter(arg -> arg.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (subCommand.equals("compass")) {
            if (args.length == 2) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 3) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("<количество>");
                return suggestions.stream()
                        .filter(arg -> arg.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 2 && subCommand.equals("undo")) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("<id региона>");
            return suggestions.stream()
                    .filter(arg -> arg.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}