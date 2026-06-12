package ru.truhot.rdang.menu;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.*;

import java.util.*;

public class ListMenu extends AbstractMenu {
    private static final int ITEMS_PER_PAGE = 45, INVENTORY_SIZE = 54, NEXT_PAGE_SLOT = 50, PREV_PAGE_SLOT = 48, DELETE_ALL_SLOT = 49;
    private final TeleportUtil teleportUtil;
    private final Storage shulkers, blockStorage;
    private final String prefix;
    private final String suffix;
    private final ItemStack guiGlass = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
    private final ItemStack nextBtn, prevBtn, deleteAllBtn;
    private final Map<Material, ItemStack> dungeonTemplates = new EnumMap<>(Material.class);

    public ListMenu(ConfigManager configManager, Storage shulkers, Storage blockStorage, RDang plugin) {
        super(configManager, plugin);
        this.shulkers = shulkers;
        this.blockStorage = blockStorage;
        this.teleportUtil = new TeleportUtil(configManager);
        String format = configManager.getRegion().getString("region.name_format", "dang_{id}");
        if (format.contains("{id}")) {
            String[] parts = format.split("\\{id\\}", 2);
            this.prefix = parts[0].toLowerCase();
            this.suffix = parts[1].toLowerCase();
        } else {
            this.prefix = format.toLowerCase();
            this.suffix = "";
        }

        this.nextBtn = createNextBtn();
        this.prevBtn = createPrevBtn();
        this.deleteAllBtn = createDeleteBtn();
        setupTemplates();
    }

    private void setupTemplates() {
        for (Material m : List.of(Material.DIRT, Material.NETHERRACK, Material.END_STONE)) {
            ItemStack item = new ItemStack(m);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(MessageUtil.colorize("&fДанж &7[%d]"));
                meta.setLore(MessageUtil.colorize(List.of("", " &fМир: &#557c93%s", " &fКорды: &#FEF06A%s", "", " &fПри нажатии &#557c93ПКМ&f телепортирует", " &fПри нажатии &#FE6A6AЛКМ&f удаляет")));
                item.setItemMeta(meta);
            }
            dungeonTemplates.put(m, item);
        }
    }

    @Override
    public void openMenu(Player player, int page) {
        List<String> allIds = getIds();
        if (allIds.isEmpty() && page == 0) {
            player.sendMessage(MessageUtil.colorize(configManager.getMessages().getString("messages.list.no_dungeons")));
            player.closeInventory();
            return;
        }
        open(player, page);
    }

    @Override
    protected Inventory buildInventory(Player player, int page) {
        List<String> allIds = getIds();
        int maxPages = Math.max(1, (int) Math.ceil((double) allIds.size() / ITEMS_PER_PAGE));
        int curPage = Math.max(0, Math.min(page, maxPages - 1));
        Inventory inv = Bukkit.createInventory(new MenuHolder(getType(), curPage), INVENTORY_SIZE, MessageUtil.getFormatted("§7Список Данжей &8(%d/%d)", curPage + 1, maxPages));
        int start = curPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (start + i) < allIds.size(); i++) inv.setItem(i, createIcon(allIds.get(start + i)));
        for (int s = 45; s < 54; s++) inv.setItem(s, guiGlass);

        inv.setItem(DELETE_ALL_SLOT, deleteAllBtn);
        if (curPage > 0) inv.setItem(PREV_PAGE_SLOT, prevBtn);
        if (curPage < maxPages - 1) inv.setItem(NEXT_PAGE_SLOT, nextBtn);
        return inv;
    }

    private ItemStack createIcon(String regionId) {
        World world = getWorld(regionId);
        Material m = Material.DIRT;
        if (world != null) {
            String name = world.getName().toLowerCase();
            if (name.contains("nether")) m = Material.NETHERRACK;
            else if (name.contains("end")) m = Material.END_STONE;
        }
        ItemStack item = dungeonTemplates.get(m).clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(String.format(meta.getDisplayName(), getNumber(regionId)));
            List<String> lore = meta.getLore();
            if (lore != null) {
                lore.set(1, String.format(lore.get(1), world != null ? world.getName() : "Unknown"));
                lore.set(2, String.format(lore.get(2), getCoords(world, regionId)));
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    protected void onMenuClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        MenuHolder holder = (MenuHolder) e.getInventory().getHolder();
        int slot = e.getRawSlot();
        int currentPage = holder.getPage();
        if (slot == DELETE_ALL_SLOT) {
            List<String> allIds = getIds();
            if (allIds.isEmpty()) return;
            UndoUtil undoUtil = new UndoUtil(configManager, shulkers, blockStorage, plugin);
            player.sendMessage(MessageUtil.colorize("&aЗапуск удаления всех данжей..."));
            player.closeInventory();

            new BukkitRunnable() {
                int count = 0;
                int processed = 0;
                @Override
                public void run() {
                    for (String rId : allIds) {
                        undoUtil.performUndo(rId, res -> {
                            if (res.found) count++;
                            processed++;
                            if (processed == allIds.size()) {
                                player.sendMessage(MessageUtil.colorize("&7[&#6AFE76☑&7] &fУспешно удалено &#557c93" + count + " &fданжей."));
                                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);
                            }
                        });
                    }
                }
            }.runTask(plugin);
            return;
        }
        if (slot == NEXT_PAGE_SLOT) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            openMenu(player, currentPage + 1);
            return;
        }
        if (slot == PREV_PAGE_SLOT) {
            if (currentPage > 0) {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
                openMenu(player, currentPage - 1);
            }
            return;
        }

        if (slot >= ITEMS_PER_PAGE) return;
        if (dungeonTemplates.containsKey(item.getType())) {
            List<String> ids = getIds();
            int index = (currentPage * ITEMS_PER_PAGE) + slot;
            if (index < ids.size()) {
                String rId = ids.get(index);
                if (e.isRightClick()) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.0f);
                    player.closeInventory();
                    teleportUtil.teleport(player, rId);
                } else if (e.isLeftClick()) {
                    new UndoUtil(configManager, shulkers, blockStorage, plugin).performUndo(rId, res -> {
                        if (res.found) {
                            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_BREAK, 0.5f, 1.5f);
                            openMenu(player, currentPage);
                        } else {
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                        }

                        String msg = configManager.getMessages().getString(res.found ? "messages.undo.region_deleted" : "messages.undo.region_not_found");
                        player.sendMessage(MessageUtil.getFormatted(msg, getNumber(rId), rId, res.worldName));
                    });
                }
            }
        }
    }

    private List<String> getIds() {
        List<String> ids = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(w));
            if (rm != null) { for (String n : rm.getRegions().keySet()) if (n.toLowerCase().startsWith(prefix)) ids.add(n); }
        }
        ids.sort(Comparator.comparingInt(this::getNumber));
        return ids;
    }

    private String getCoords(World w, String id) {
        if (w == null) return "N/A";
        try {
            ProtectedRegion r = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(w)).getRegion(id);
            if (r == null) return "N/A";
            BlockVector3 min = r.getMinimumPoint(), max = r.getMaximumPoint();
            return String.format("%d, %d, %d", (min.getBlockX()+max.getBlockX())/2, (min.getBlockY()+max.getBlockY())/2, (min.getBlockZ()+max.getBlockZ())/2);
        } catch (Exception e) { return "N/A"; }
    }

    private int getNumber(String id) {
        if (id == null || id.isEmpty()) return 0;
        String lowered = id.toLowerCase();
        try {
            if (!prefix.isEmpty() && lowered.startsWith(prefix)) {
                String remainder = id.substring(prefix.length());
                if (!suffix.isEmpty() && remainder.toLowerCase().endsWith(suffix)) {
                    remainder = remainder.substring(0, remainder.length() - suffix.length());
                }
                remainder = remainder.trim();
                if (!remainder.isEmpty() && remainder.chars().allMatch(Character::isDigit)) {
                    return (int) Math.min(Integer.MAX_VALUE, Long.parseLong(remainder));
                }
            }
            
            String digits = id.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return 0;
            if (digits.length() > 9) digits = digits.substring(0, 9); // Prevent overflow
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    private ItemStack createPane(Material m, String n) {
        ItemStack i = new ItemStack(m);
        ItemMeta mt = i.getItemMeta();
        if (mt != null) { mt.setDisplayName(n); i.setItemMeta(mt); }
        return i;
    }

    private ItemStack createNextBtn() {
        ItemStack i = HeadUtil.createSkullBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDRiZThhZWVjMTE4NDk2OTdhZGM2ZmQxZjE4OWIxNjY0MmRmZjE5ZjI5NTVjMDVkZWFiYTY4YzlkZmYxYmUifX19", "menu");
        ItemMeta m = i.getItemMeta();
        if (m != null) m.setDisplayName(MessageUtil.colorize("§6Следующая страница §f[ §7→ §f]"));
        i.setItemMeta(m);
        return i;
    }

    private ItemStack createDeleteBtn() {
        String texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTllYjg2Y2FlNzAzMTIxZWIxN2MzNjc4YzFkOWQxYzI4YzMwNzljMTAyODhjODQyYTQ4Mzk4ZWQ3ZDkzMzY2ZSJ9fX0=";
        ItemStack i = HeadUtil.createSkullBase64(texture, "menu");
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName(MessageUtil.colorize("&fМусорка &8(:/)"));
            m.setLore(MessageUtil.colorize(List.of("", " &fНажмите &#557c93пкм&f чтоб &cудалить&f все данжи", "")));
            i.setItemMeta(m);
        }
        return i;
    }

    private ItemStack createPrevBtn() {
        ItemStack i = HeadUtil.createSkullBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzYyNTkwMmIzODllZDZjMTQ3NTc0ZTQyMmRhOGY4ZjM2MWM4ZWI1N2U3NjMxNjc2YTcyNzc3ZTdiMWQifX19", "menu");
        ItemMeta m = i.getItemMeta();
        if (m != null) m.setDisplayName(MessageUtil.colorize("§f[ §7← §f] §6Предыдущая страница"));
        i.setItemMeta(m);
        return i;
    }

    private World getWorld(String id) {
        for (World w : Bukkit.getWorlds()) {
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(w));
            if (rm != null && rm.hasRegion(id)) return w;
        }
        return null;
    }

    @Override
    protected MenuType getType() {
        return MenuType.DUNGEON_LIST;
    }
}
