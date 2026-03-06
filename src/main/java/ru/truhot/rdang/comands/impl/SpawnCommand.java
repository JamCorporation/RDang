package ru.truhot.rdang.comands.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.dung.DungActions;
import ru.truhot.rdang.util.MessageUtil;
import java.util.Random;

public class SpawnCommand implements CommandExecutor {
    private final DungActions dungActions;
    private final ConfigManager configManager;
    private final Random random = new Random();

    public SpawnCommand(DungActions dungActions, ConfigManager configManager) {
        this.dungActions = dungActions;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("only-player")));
            return true;
        }
        if (args.length != 2) {
            player.sendMessage(MessageUtil.colorize(getMessage("spawn.usage")));
            return true;
        }
        World world = player.getWorld();
        try {
            int amount = Integer.parseInt(args[1]);
            if (amount <= 0) {
                player.sendMessage(MessageUtil.colorize(getMessage("spawn.amount-positive")));
                return true;
            }
            int spawnedCount = 0;
            for (int i = 0; i < amount; i++) {
                Location loc = configManager.getSpawnManager().findSuitableDungLocation(world, random);
                if (loc != null) {
                    dungActions.spawn(loc);
                    String spawnedMsg = getMessage("spawn.spawned")
                            .replace("{x}", String.valueOf(loc.getBlockX()))
                            .replace("{y}", String.valueOf(loc.getBlockY()))
                            .replace("{z}", String.valueOf(loc.getBlockZ()));
                    player.sendMessage(MessageUtil.colorize(spawnedMsg));
                    spawnedCount++;
                }
            }
            if (spawnedCount == 0) {
                player.sendMessage(MessageUtil.colorize(getMessage("spawn.none-spawned")));
            }
            return true;
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtil.colorize(getMessage("spawn.invalid-number")));
            return true;
        }
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}