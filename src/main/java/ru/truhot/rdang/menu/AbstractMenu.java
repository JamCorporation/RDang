package ru.truhot.rdang.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.util.MessageUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class AbstractMenu {

    protected final ConfigManager configManager;
    protected final RDang plugin;
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public abstract void openMenu(Player player, int page);

    protected abstract MenuType getType();

    protected abstract Inventory buildInventory(Player player, int page);

    protected abstract void onMenuClick(Player player, InventoryClickEvent event);

    public void open(Player player, int page) {
        Inventory inventory = buildInventory(player, page);
        if (inventory == null) {
            player.sendMessage(MessageUtil.colorize("&cОшибка при создании меню!"));
            return;
        }
        player.openInventory(inventory);
        openInventories.put(player.getUniqueId(), inventory);
    }

    public void close(Player player, InventoryCloseEvent event) {
        openInventories.remove(player.getUniqueId());
    }

    public void onClick(Player player, InventoryClickEvent event) {
        if (!ownsInventory(event)) return;
        onMenuClick(player, event);
    }

    private boolean ownsInventory(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return false;
        }
        return holder.getType() == getType();
    }

    protected String msg(String path) {
        return configManager.getMessages().getString("messages." + path, "");
    }

    protected void sendMsg(Player player, String path) {
        String text = msg(path);
        if (text == null || text.isBlank()) return;
        player.sendMessage(MessageUtil.colorize(text));
    }

    protected List<String> msgList(String path) {
        return configManager.getMessages().getStringList("messages." + path);
    }
}
