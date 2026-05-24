package ru.truhot.rdang.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.permission.Permissions;
import ru.truhot.rdang.util.MessageUtil;

import java.util.List;

public class MainMenu extends AbstractMenu {

    private static final int SIZE = 54;
    private static final int SLOT_DUNGEONS = 20;
    private static final int SLOT_LOOT = 21;
    private static final int[] CHAIN_SLOTS = {18, 26, 27, 35};

    private final MenuManager menuManager;
    private final ItemStack bottomGlass = pane(Material.GRAY_STAINED_GLASS_PANE);
    private final ItemStack chain = pane(Material.CHAIN);
    private final ItemStack dungeonsButton;
    private final ItemStack lootButton;

    public MainMenu(ConfigManager configManager, RDang plugin, MenuManager menuManager) {
        super(configManager, plugin);
        this.menuManager = menuManager;
        this.dungeonsButton = dungeonsButton();
        this.lootButton = lootButton();
    }

    @Override
    public void openMenu(Player player, int page) {
        open(player, 0);
    }

    @Override
    protected Inventory buildInventory(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(
                new MenuHolder(getType(), 0),
                SIZE,
                MessageUtil.colorize("&0Главное меню")
        );

        for (int slot = 45; slot < SIZE; slot++) {
            inventory.setItem(slot, bottomGlass);
        }
        for (int slot : CHAIN_SLOTS) {
            inventory.setItem(slot, chain);
        }
        inventory.setItem(SLOT_DUNGEONS, dungeonsButton);
        inventory.setItem(SLOT_LOOT, lootButton);
        return inventory;
    }

    @Override
    protected void onMenuClick(Player player, InventoryClickEvent event) {
        event.setCancelled(true);
        if (!event.isRightClick()) return;

        switch (event.getRawSlot()) {
            case SLOT_DUNGEONS -> openSubmenu(player, Permissions.LIST, MenuType.DUNGEON_LIST);
            case SLOT_LOOT -> openSubmenu(player, Permissions.ADD_ITEM, MenuType.LOOT_PAGES);
            default -> { }
        }
    }

    private void openSubmenu(Player player, String permission, MenuType target) {
        if (!Permissions.has(player, permission)) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            player.sendMessage(MessageUtil.colorize(msg("no_permission")));
            return;
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
        menuManager.navigateTo(player, MenuType.MAIN, target);
    }

    private ItemStack dungeonsButton() {
        ItemStack item = new ItemStack(Material.BARREL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize("&#FEF06AСписок данжей"));
            meta.setLore(MessageUtil.colorize(List.of(
                    "",
                    " &fНажмите &#557c93пкм&f",
                    " &fчтоб &cперейти&f в меню",
                    ""
            )));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack lootButton() {
        ItemStack item = new ItemStack(Material.ITEM_FRAME);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize("&#6AFE76Редактор лута"));
            meta.setLore(MessageUtil.colorize(List.of(
                    "",
                    " &fНажмите &#557c93пкм&f",
                    " &fчтоб &cперейти&f в меню",
                    ""
            )));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    protected MenuType getType() {
        return MenuType.MAIN;
    }
}
