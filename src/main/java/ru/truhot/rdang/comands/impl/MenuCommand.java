package ru.truhot.rdang.comands.impl;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import lombok.RequiredArgsConstructor;
import ru.truhot.rdang.permission.Permissions;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.menu.MenuManager;
import ru.truhot.rdang.menu.MenuType;
import ru.truhot.rdang.util.MessageUtil;

@RequiredArgsConstructor
public class MenuCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final MenuManager menuManager;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, Permissions.MENU)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("no_permission")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("only_player")));
            return true;
        }

        menuManager.open(MenuType.MAIN, player);
        return true;
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}
