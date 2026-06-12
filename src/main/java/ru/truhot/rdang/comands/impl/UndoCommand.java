package ru.truhot.rdang.comands.impl;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.truhot.rdang.permission.Permissions;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.UndoUtil;

public class UndoCommand implements CommandExecutor {
    private final ConfigManager configManager;
    private final UndoUtil undoUtil;

    public UndoCommand(ConfigManager configManager, UndoUtil undoUtil) {
        this.configManager = configManager;
        this.undoUtil = undoUtil;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, Permissions.UNDO)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("no_permission")));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(MessageUtil.colorize(getMessage("undo.usage")));
            return true;
        }

        String dungeonId = args[1];
        String regionName = configManager.getRegion().getString("region.name_format")
                .replace("{id}", dungeonId);

        sender.sendMessage(MessageUtil.colorize("&aЗапуск удаления данжа... Пожалуйста, подождите."));
        undoUtil.performUndo(regionName, result -> {
            if (!result.found) {
                sender.sendMessage(MessageUtil.colorize(getMessage("undo.region_not_found")
                        .replace("{id}", dungeonId)
                        .replace("{region}", regionName)));
                return;
            }

            sender.sendMessage(MessageUtil.colorize(getMessage("undo.region_deleted")
                    .replace("{id}", dungeonId)
                    .replace("{region}", regionName)
                    .replace("{world}", result.worldName)));
            sender.sendMessage(MessageUtil.colorize(getMessage("undo.shulkers_deleted")
                    .replace("{shulker}", String.valueOf(result.chestCount))));
        });
        return true;
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}
