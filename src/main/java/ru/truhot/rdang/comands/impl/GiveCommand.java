package ru.truhot.rdang.comands.impl;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.permission.Permissions;
import ru.truhot.rdang.util.MessageUtil;

public class GiveCommand {

    private final ConfigManager configManager;

    public GiveCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3 || args.length > 4) {
            sender.sendMessage(MessageUtil.colorize(getMessage("give.usage")));
            return true;
        }

        String type = args[1].trim().toLowerCase();
        if (!type.equals("key") && !type.equals("compass")) {
            String msg = getMessage("give.unknown_type").replace("{type}", args[1].trim());
            sender.sendMessage(MessageUtil.colorize(msg));
            return true;
        }

        if (type.equals("key") && !Permissions.has(sender, Permissions.GIVE_KEY)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("no_permission")));
            return true;
        }
        if (type.equals("compass") && !Permissions.has(sender, Permissions.GIVE_COMPASS)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("no_permission")));
            return true;
        }

        String targetName = args[2];
        if (targetName == null || targetName.trim().isEmpty()) {
            sender.sendMessage(MessageUtil.colorize(getMessage("give.nick_player")));
            return true;
        }

        int amount = 1;
        if (args.length == 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1) {
                    sender.sendMessage(MessageUtil.colorize(getMessage("give.min_amount")));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(MessageUtil.colorize(getMessage("give.invalid_amount")));
                return true;
            }
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(MessageUtil.colorize(getMessage("give.no_player")));
            return true;
        }

        if (type.equals("key")) {
            giveKey(sender, target, amount);
        } else {
            giveCompass(sender, target, amount);
        }

        return true;
    }

    private void giveKey(CommandSender sender, Player target, int amount) {
        ItemStack key = configManager.getItemManager().getKey();
        if (key == null) {
            sender.sendMessage(MessageUtil.colorize(getMessage("give.key.key_null")));
            return;
        }

        int maxStackSize = key.getMaxStackSize();
        int given = 0;
        while (given < amount) {
            int stackAmount = Math.min(maxStackSize, amount - given);
            ItemStack stack = key.clone();
            stack.setAmount(stackAmount);
            if (target.getInventory().addItem(stack).isEmpty()) {
                given += stackAmount;
            } else {
                target.getWorld().dropItemNaturally(target.getLocation(), stack);
                given += stackAmount;
            }
        }

        String giveMsg = getMessage("give.key.give")
                .replace("{key}", String.valueOf(given))
                .replace("{player}", target.getName());
        sender.sendMessage(MessageUtil.colorize(giveMsg));
    }

    private void giveCompass(CommandSender sender, Player target, int amount) {
        ItemStack compass = configManager.getItemManager().getCompass();
        if (compass == null) {
            sender.sendMessage(MessageUtil.colorize(getMessage("give.compass.compass_null")));
            return;
        }

        int maxStackSize = compass.getMaxStackSize();
        int given = 0;
        while (given < amount) {
            int stackAmount = Math.min(maxStackSize, amount - given);
            ItemStack stack = compass.clone();
            stack.setAmount(stackAmount);
            if (target.getInventory().addItem(stack).isEmpty()) {
                given += stackAmount;
            } else {
                target.getWorld().dropItemNaturally(target.getLocation(), stack);
                given += stackAmount;
            }
        }

        String giveMsg = getMessage("give.compass.give")
                .replace("{amount}", String.valueOf(given))
                .replace("{player}", target.getName());

        if (sender != target) {
            sender.sendMessage(MessageUtil.colorize(giveMsg));
        }
        target.sendMessage(MessageUtil.colorize(giveMsg));
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}
