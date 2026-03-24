package ru.truhot.rdang.comands.impl;

import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.сore.managers.ShulkerManager;
import ru.truhot.rdang.сore.managers.LootManager;

public class AdminsCommand implements CommandExecutor {

    private final ShulkerManager shulkerManager;
    private final LootManager lootManager;
    private final Storage shulkersStorage;
    private final ConfigManager configManager;

    public AdminsCommand(ShulkerManager shulkerManager, LootManager lootManager, Storage shulkersStorage, ConfigManager configManager) {
        this.shulkerManager = shulkerManager;
        this.lootManager = lootManager;
        this.shulkersStorage = shulkersStorage;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("rdang.admin")) {
            sender.sendMessage(MessageUtil.colorize(getMessage("no-permission")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("only-player")));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(MessageUtil.colorize(getMessage("admins.usage")));
            return true;
        }

        String action = args[1].toLowerCase();
        Block target = player.getTargetBlockExact(5);

        if (target == null || !shulkerManager.isShulker(target)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("admins.not-shulker")));
            return true;
        }

        switch (action) {
            case "test" -> {
                shulkerManager.addShulker(target.getLocation());
                player.sendMessage(MessageUtil.colorize(getMessage("admins.test-success")));
            }
            case "remove" -> {
                removeShulkerData(target);
                player.sendMessage(MessageUtil.colorize(getMessage("admins.remove-success")));
            }
            case "loot" -> {
                if (target.getState() instanceof ShulkerBox box) {
                    lootManager.fillInventoryWithRandomLoot(box.getInventory());
                    player.sendMessage(MessageUtil.colorize(getMessage("admins.loot-success")));
                }
            }
            default -> player.sendMessage(MessageUtil.colorize(getMessage("admins.usage")));
        }
        return true;
    }

    private void removeShulkerData(Block block) {
        ConfigurationSection locs = shulkersStorage.getConfig().getConfigurationSection("locs");
        if (locs == null) return;
        for (String id : locs.getKeys(false)) {
            if (block.getLocation().equals(shulkersStorage.getConfig().getLocation("locs." + id + ".location"))) {
                locs.set(id, null);
                shulkersStorage.save();
                break;
            }
        }
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}