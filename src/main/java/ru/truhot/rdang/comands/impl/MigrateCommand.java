package ru.truhot.rdang.comands.impl;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import lombok.RequiredArgsConstructor;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.config.MigrateConfig;
import ru.truhot.rdang.permission.Permissions;
import ru.truhot.rdang.util.MessageUtil;

@RequiredArgsConstructor
public class MigrateCommand implements CommandExecutor {

    private final RDang plugin;
    private final ConfigManager configManager;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, Permissions.MIGRATE)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("no_permission")));
            return true;
        }
        if (args.length > 1) {
            sender.sendMessage(MessageUtil.colorize(getMessage("migrate.usage")));
            return true;
        }

        MigrateConfig.MigrateResult result = new MigrateConfig().migrate(plugin);

        if (result.isAllUpToDate()) {
            sender.sendMessage(MessageUtil.colorize(getMessage("migrate.all_up_to_date")));
            return true;
        }

        if (!result.getUpdated().isEmpty()) {
            String files = String.join(", ", result.getUpdated());
            sender.sendMessage(MessageUtil.colorize(
                    getMessage("migrate.success").replace("{files}", files)));
            sender.sendMessage(MessageUtil.colorize(getMessage("migrate.reload_hint")));
        }

        for (String failed : result.getFailed()) {
            sender.sendMessage(MessageUtil.colorize(
                    getMessage("migrate.failed").replace("{file}", failed)));
        }

        return true;
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}
