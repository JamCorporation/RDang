package ru.truhot.rdang.comands.impl;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.util.MessageUtil;

public class GiveCompassCommand {
    private final ConfigManager configManager;

    public GiveCompassCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(MessageUtil.colorize(getMessage("givecompass.usage")));
            return true;
        }

        if (!sender.hasPermission("rdang.compass")) {
            sender.sendMessage(MessageUtil.colorize(getMessage("no-permission")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MessageUtil.colorize(getMessage("givecompass.noplayer")));
            return true;
        }

        ItemStack compass = configManager.getItemManager().getCompass();
        if (compass == null) {
            sender.sendMessage(MessageUtil.colorize(getMessage("givecompass.compassnull")));
            return true;
        }

        int amount = 1;
        if (args.length == 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1) {
                    sender.sendMessage(MessageUtil.colorize(getMessage("givecompass.minamount")));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(MessageUtil.colorize(getMessage("givecompass.invalidamount")));
                return true;
            }
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

        String giveMsg = getMessage("givecompass.give")
                .replace("{amount}", String.valueOf(given))
                .replace("{player}", target.getName());

        if (sender != target) {
            sender.sendMessage(MessageUtil.colorize(giveMsg));
        }
        target.sendMessage(MessageUtil.colorize(giveMsg));

        return true;
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}