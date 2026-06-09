package ru.truhot.rdang.comands.impl;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.permission.Permissions;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.UpdateUtil;

public class UpdateCommand implements CommandExecutor {

    private final RDang plugin;
    private final ConfigManager configManager;
    private final UpdateUtil updateUtil;

    public UpdateCommand(RDang plugin, ConfigManager configManager, UpdateUtil updateUtil) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.updateUtil = updateUtil;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!Permissions.has(sender, Permissions.UPDATE)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("no_permission")));
            return true;
        }

        if (args.length > 1) {
            sender.sendMessage(MessageUtil.colorize(getMessage("update.usage")));
            return true;
        }

        if (updateUtil.isUpdateAvailable() && updateUtil.getLatestVersion() != null) {
            sender.sendMessage(MessageUtil.colorize(getMessage("update.starting").replace("{version}", updateUtil.getLatestVersion())));

            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                updateUtil.downloadUpdate(updateUtil.getLatestVersion(), sender);
                sender.sendMessage(MessageUtil.colorize(getMessage("update.restart_needed")));
            });
        } else {
            sender.sendMessage(MessageUtil.colorize(getMessage("update.latest_version")));
        }

        return true;
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }

}