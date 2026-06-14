package ru.truhot.rdang.comands.impl;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.truhot.rdang.autospawn.AutoSpawnManager;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.permission.Permissions;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.TimeUtil;

public class AutoSpawnCommand implements CommandExecutor {

    private final AutoSpawnManager autoSpawnManager;
    private final ConfigManager configManager;

    public AutoSpawnCommand(AutoSpawnManager autoSpawnManager, ConfigManager configManager) {
        this.autoSpawnManager = autoSpawnManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, Permissions.AUTOSPAWN)) {
            sender.sendMessage(MessageUtil.colorize(msg("no_permission")));
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "on", "enable" -> {
                if (autoSpawnManager.isRunning()) {
                    sender.sendMessage(MessageUtil.colorize(msg("autospawn.already_enabled")));
                    return true;
                }
                autoSpawnManager.setEnabled(true);
                sender.sendMessage(MessageUtil.colorize(msg("autospawn.enabled")
                        .replace("{interval}", configManager.getAuto().getString("auto.spawn.interval", "1h"))));
            }
            case "off", "disable" -> {
                if (!autoSpawnManager.isRunning()) {
                    sender.sendMessage(MessageUtil.colorize(msg("autospawn.already_disabled")));
                    return true;
                }
                autoSpawnManager.setEnabled(false);
                sender.sendMessage(MessageUtil.colorize(msg("autospawn.disabled")));
            }
            case "interval" -> {
                if (args.length < 3) {
                    sender.sendMessage(MessageUtil.colorize(msg("autospawn.interval_usage")));
                    return true;
                }
                String timeStr = args[2];
                long parsed = TimeUtil.parse(timeStr);
                if (parsed <= 0) {
                    sender.sendMessage(MessageUtil.colorize(msg("autospawn.invalid_interval")));
                    return true;
                }
                autoSpawnManager.setInterval(timeStr);
                sender.sendMessage(MessageUtil.colorize(msg("autospawn.interval_set")
                        .replace("{interval}", timeStr)));
            }
            case "status" -> {
                boolean running = autoSpawnManager.isRunning();
                if (running) {
                    String interval = configManager.getAuto().getString("auto.spawn.interval", "1h");
                    long remaining = autoSpawnManager.getRemainingSeconds();
                    String remainingStr = TimeUtil.format(remaining);
                    sender.sendMessage(MessageUtil.colorize(msg("autospawn.status_enabled")
                            .replace("{interval}", interval)
                            .replace("{remaining}", remainingStr)));
                } else {
                    sender.sendMessage(MessageUtil.colorize(msg("autospawn.status_disabled")));
                }
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MessageUtil.colorize(msg("autospawn.usage")));
    }

    private String msg(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}
