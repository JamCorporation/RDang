package ru.truhot.rdang.comands.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import lombok.RequiredArgsConstructor;
import ru.truhot.rdang.permission.Permissions;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.dung.DungActions;
import ru.truhot.rdang.util.MessageUtil;
import java.util.Random;

@RequiredArgsConstructor
public class SpawnCommand implements CommandExecutor {
    private final DungActions dungActions;
    private final ConfigManager configManager;
    private final Random random = new Random();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, Permissions.SPAWN)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("no_permission")));
            return true;
        }

        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(MessageUtil.colorize(getMessage("spawn.usage")));
            return true;
        }

        World targetWorld = null;

        if (args.length == 3) {
            targetWorld = Bukkit.getWorld(args[2]);
            if (targetWorld == null) {
                sender.sendMessage(MessageUtil.colorize(getMessage("spawn.world_not_found").replace("{world}", args[2])));
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MessageUtil.colorize(getMessage("only_player")));
                return true;
            }
            targetWorld = player.getWorld();
        }
        if (configManager.getDangManager().getWorldDangs(targetWorld.getName()).isEmpty()) {
            sender.sendMessage(MessageUtil.colorize(getMessage("spawn.no_dang_world").replace("{world}", targetWorld.getName())));
            return true;
        }
        try {
            int amount = Integer.parseInt(args[1]);
            if (amount <= 0) {
                sender.sendMessage(MessageUtil.colorize(getMessage("spawn.amount_positive")));
                return true;
            }
            int spawnedCount = 0;
            for (int i = 0; i < amount; i++) {
                Location loc = configManager.getSpawnManager().findDungLocation(targetWorld, random);
                if (loc != null) {
                    dungActions.spawn(loc);
                    String spawnedMsg = getMessage("spawn.spawned")
                            .replace("{x}", String.valueOf(loc.getBlockX()))
                            .replace("{y}", String.valueOf(loc.getBlockY()))
                            .replace("{z}", String.valueOf(loc.getBlockZ()))
                            .replace("{world}", targetWorld.getName());
                    sender.sendMessage(MessageUtil.colorize(spawnedMsg));
                    spawnedCount++;
                }
            }
            if (spawnedCount == 0) {
                sender.sendMessage(MessageUtil.colorize(getMessage("spawn.none_spawned")));
            }
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.colorize(getMessage("spawn.invalid_number")));
            return true;
        }
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}