package ru.truhot.rdang.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.сore.managers.LootManager;
import ru.truhot.rdang.сore.managers.LootManager.LootEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LootEditMenu extends AbstractMenu {

    private static final int SIZE = 54;
    private static final int CONTENT = 45;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_CHANCE = 49;

    private final MenuManager manager;
    private final LootManager loot;
    private final ItemStack glass = pane(Material.GRAY_STAINED_GLASS_PANE);

    public LootEditMenu(ConfigManager configManager, RDang plugin, MenuManager manager, LootManager loot) {
        super(configManager, plugin);
        this.manager = manager;
        this.loot = loot;
    }

    @Override
    public void openMenu(Player player, int page) {
        open(player, page);
    }

    @Override
    protected Inventory buildInventory(Player player, int page) {
        Inventory inv = Bukkit.createInventory(
                new MenuHolder(getType(), page),
                SIZE,
                MessageUtil.colorize("&0Настройка лута")
        );

        fillBottom(inv);
        inv.setItem(SLOT_BACK, button(Material.BARRIER, "",
                "", " &fНажмите &#557c93пкм&f", " &fчтоб &cвернуться&f назад", ""));
        inv.setItem(SLOT_CHANCE, button(Material.CLOCK, "&#7CAFF1Настроить шансы",
                "", " &fНажмите &#557c93пкм&f", " &fчтоб &cоткрыть&f меню", ""));

        for (Map.Entry<Integer, LootEntry> entry : loot.pageItems(page).entrySet()) {
            if (entry.getKey() < CONTENT) {
                inv.setItem(entry.getKey(), entry.getValue().item().clone());
            }
        }
        return inv;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;
        if (holder.getType() != getType()) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < CONTENT || rawSlot >= SIZE) {
            return;
        }
        event.setCancelled(true);
        onMenuClick(player, event);
    }

    @Override
    protected void onMenuClick(Player player, InventoryClickEvent event) {
        if (!event.isRightClick()) return;

        MenuHolder holder = (MenuHolder) event.getInventory().getHolder();
        int page = holder.getPage();

        switch (event.getRawSlot()) {
            case SLOT_BACK -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
                manager.navigateTo(player, MenuType.LOOT_EDIT, MenuType.LOOT_PAGES);
            }
            case SLOT_CHANCE -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
                manager.navigateTo(player, MenuType.LOOT_EDIT, MenuType.LOOT_CHANCE, page);
            }
            default -> { }
        }
    }

    @Override
    public void close(Player player, InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;
        if (holder.getType() != getType()) return;

        writeLoot(event.getInventory(), holder.getPage());
        stripContent(event.getInventory());
        if (!manager.CloseMessage(player, MenuType.LOOT_EDIT)) {
            sendMsg(player, "loot.saved");
        }
        super.close(player, event);
    }

    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;
        if (holder.getType() != getType()) return;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= CONTENT && rawSlot < SIZE) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void writeLoot(Inventory inventory, int page) {
        Map<Integer, LootEntry> saved = new LinkedHashMap<>(loot.pageItems(page));

        for (int slot = 0; slot < CONTENT; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                saved.remove(slot);
                continue;
            }
            LootEntry prev = saved.get(slot);
            int chance = prev != null ? prev.chance() : LootManager.DEFAULT_CHANCE;
            saved.put(slot, new LootEntry(item.clone(), chance));
        }
        loot.savePage(page, saved);
    }

    private void stripContent(Inventory inventory) {
        for (int slot = 0; slot < CONTENT; slot++) {
            inventory.setItem(slot, null);
        }
    }

    private void fillBottom(Inventory inv) {
        for (int slot = CONTENT; slot < SIZE; slot++) {
            inv.setItem(slot, glass);
        }
    }

    @Override
    protected MenuType getType() {
        return MenuType.LOOT_EDIT;
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
