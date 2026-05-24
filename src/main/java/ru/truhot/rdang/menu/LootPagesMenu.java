package ru.truhot.rdang.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.сore.managers.LootManager;

import java.util.List;

public class LootPagesMenu extends AbstractMenu {

    private static final int SIZE = 54;
    private static final int CONTENT = 45;
    private static final int SLOT_CREATE = 49;

    private final MenuManager manager;
    private final LootManager loot;
    private final ItemStack glass = pane(Material.GRAY_STAINED_GLASS_PANE);

    public LootPagesMenu(ConfigManager configManager, RDang plugin, MenuManager manager, LootManager loot) {
        super(configManager, plugin);
        this.manager = manager;
        this.loot = loot;
    }

    @Override
    public void openMenu(Player player, int page) {
        open(player, 0);
    }

    @Override
    protected Inventory buildInventory(Player player, int page) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(getType(), 0), SIZE, MessageUtil.colorize("&0Страницы лута"));

        for (int slot = CONTENT; slot < SIZE; slot++) {
            inv.setItem(slot, glass);
        }

        inv.setItem(SLOT_CREATE, button(Material.LODESTONE, "&#6AFE76Создать страницу"));

        int pages = loot.pageCount();
        for (int i = 0; i < Math.min(pages, CONTENT); i++) {
            inv.setItem(i, button(Material.CHEST_MINECART, "&fСтраница &7" + (i + 1),
                    "",
                    " &fПредметов: &#557c93" + loot.itemsOnPage(i),
                    "",
                    " &fПКМ: &#81FF84редактировать",
                    " &fShift+ПКМ: &#FE6A6Aудалить",
                    ""
            ));
        }
        return inv;
    }

    @Override
    protected void onMenuClick(Player player, InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == SLOT_CREATE) {
            if (!event.isRightClick()) return;
            if (!loot.canAddPage()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                player.sendMessage(MessageUtil.colorize(msg("loot.page_limit")));
                return;
            }
            loot.addPage();
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);
            player.sendMessage(MessageUtil.colorize(msg("loot.page_created").replace("{count}", String.valueOf(loot.pageCount()))));
            openMenu(player, 0);
            return;
        }
        if (slot >= CONTENT || slot >= loot.pageCount()) return;

        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            if (loot.pageCount() <= 1) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                player.sendMessage(MessageUtil.colorize(msg("loot.page_remove_denied")));
                return;
            }
            loot.removePage(slot);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_BREAK, 0.5f, 1.5f);
            player.sendMessage(MessageUtil.colorize(msg("loot.page_removed").replace("{page}", String.valueOf(slot + 1))));
            openMenu(player, 0);
            return;
        }
        if (event.isRightClick()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            manager.navigateTo(player, MenuType.LOOT_PAGES, MenuType.LOOT_EDIT, slot);
        }
    }

    @Override
    protected MenuType getType() {
        return MenuType.LOOT_PAGES;
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

    private static ItemStack button(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
            meta.setLore(MessageUtil.colorize(List.of(lore)));
            item.setItemMeta(meta);
        }
        return item;
    }
}
