package ru.truhot.rdang.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.сore.managers.LootManager;
import ru.truhot.rdang.сore.managers.LootManager.LootEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LootChanceMenu extends AbstractMenu {

    private static final int SIZE = 54;
    private static final int CONTENT = 45;
    private static final int SLOT_INFO = 49;

    private final MenuManager manager;
    private final LootManager loot;
    private final ItemStack glass = pane(Material.GRAY_STAINED_GLASS_PANE);
    private final Map<UUID, Map<Integer, LootEntry>> session = new HashMap<>();

    public LootChanceMenu(ConfigManager configManager, RDang plugin, MenuManager manager, LootManager loot) {
        super(configManager, plugin);
        this.manager = manager;
        this.loot = loot;
    }

    @Override
    public void openMenu(Player player, int page) {
        session.put(player.getUniqueId(), new LinkedHashMap<>(loot.pageItems(page)));
        open(player, page);
    }

    @Override
    protected Inventory buildInventory(Player player, int page) {
        Inventory inv = Bukkit.createInventory(
                new MenuHolder(getType(), page),
                SIZE,
                MessageUtil.colorize("&0Настройка шансов")
        );

        fillBottom(inv);
        inv.setItem(SLOT_INFO, infoPanel());

        Map<Integer, LootEntry> entries = session.getOrDefault(player.getUniqueId(), loot.pageItems(page));
        for (Map.Entry<Integer, LootEntry> entry : entries.entrySet()) {
            if (entry.getKey() < CONTENT) {
                inv.setItem(entry.getKey(), icon(entry.getValue()));
            }
        }
        return inv;
    }

    @Override
    protected void onMenuClick(Player player, InventoryClickEvent event) {
        event.setCancelled(true);
        MenuHolder holder = (MenuHolder) event.getInventory().getHolder();
        int page = holder.getPage();
        Inventory inv = event.getInventory();
        Map<Integer, LootEntry> entries = session.computeIfAbsent(player.getUniqueId(), u -> new LinkedHashMap<>());

        int slot = event.getRawSlot();
        if (slot == SLOT_INFO && event.isRightClick()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            manager.navigateTo(player, MenuType.LOOT_CHANCE, MenuType.LOOT_EDIT, page);
            return;
        }
        if (slot >= CONTENT) return;

        ItemStack clicked = inv.getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR) return;

        LootEntry entry = entries.get(slot);
        if (entry == null) return;

        int delta = chanceDelta(event.getClick());
        if (delta == 0) return;

        int newChance = clamp(entry.chance() + delta);
        LootEntry updated = new LootEntry(entry.item(), newChance);
        entries.put(slot, updated);
        inv.setItem(slot, icon(updated));
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
    }

    public void clearSession(UUID playerId) {
        session.remove(playerId);
    }

    @Override
    public void close(Player player, InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;
        if (holder.getType() != getType()) return;

        Map<Integer, LootEntry> entries = session.remove(player.getUniqueId());
        if (entries != null) {
            loot.savePage(holder.getPage(), entries);
            manager.CloseMessage(player, MenuType.LOOT_CHANCE);
        }
        super.close(player, event);
    }

    private int chanceDelta(ClickType click) {
        return switch (click) {
            case LEFT -> 1;
            case RIGHT -> -1;
            case SHIFT_LEFT -> 10;
            case SHIFT_RIGHT -> -10;
            default -> 0;
        };
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private ItemStack infoPanel() {
        return button(Material.BREWING_STAND, "&fШансы лута",
                "",
                " &fЛКМ: &#6AFE76+1%  &7/ &fПКМ: &#FE6A6A-1%",
                " &fShift+ЛКМ: &#6AFE76+10% &7/ &fShift+ПКМ: &#FE6A6A-10%",
                "",
                " &fПКМ по этой кнопке: &cназад"
        );
    }

    private ItemStack icon(LootEntry entry) {
        ItemStack stack = entry.item().clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(MessageUtil.colorize(" &fШанс: &#557c93" + entry.chance() + "%"));
            lore.add("");
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void fillBottom(Inventory inv) {
        for (int slot = CONTENT; slot < SIZE; slot++) {
            inv.setItem(slot, glass);
        }
    }

    @Override
    protected MenuType getType() {
        return MenuType.LOOT_CHANCE;
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
