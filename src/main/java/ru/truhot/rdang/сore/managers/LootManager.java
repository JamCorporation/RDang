package ru.truhot.rdang.сore.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.truhot.rdang.storage.Storage;

import java.util.*;

public class LootManager {

    public static final int MAX_PAGES = 45;
    public static final int DEFAULT_CHANCE = 10;

    private static final String PAGES = "loot_pages";
    private static final Set<String> RESERVED = Set.of("key", "compass", PAGES);

    private final Storage items;
    private final Random random = new Random();

    public LootManager(Storage items) {
        this.items = items;
    }

    public void fillRandomLoot(Inventory inventory) {
        List<LootEntry> pool = getAllLoot();
        if (pool.isEmpty()) return;

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) slots.add(i);

        for (LootEntry entry : pool) {
            if (random.nextDouble() * 100.0D >= entry.chance()) continue;
            if (slots.isEmpty()) break;

            ItemStack stack = entry.item().clone();
            if (stack.getAmount() < 1) stack.setAmount(1);

            int index = random.nextInt(slots.size());
            inventory.setItem(slots.remove(index), stack);
        }
    }

    public int pageCount() {
        migrateLegacyLoot();
        ConfigurationSection pages = pagesSection();
        if (pages == null || pages.getKeys(false).isEmpty()) {
            ensurePage(0);
            return 1;
        }
        return pages.getKeys(false).size();
    }

    public int itemsOnPage(int page) {
        ConfigurationSection slots = pageSection(page);
        if (slots == null) return 0;
        int count = 0;
        for (String key : slots.getKeys(false)) {
            ItemStack item = slots.getItemStack(key + ".item");
            if (item != null && item.getType() != org.bukkit.Material.AIR) count++;
        }
        return count;
    }

    public Map<Integer, LootEntry> pageItems(int page) {
        Map<Integer, LootEntry> result = new LinkedHashMap<>();
        ConfigurationSection slots = pageSection(page);
        if (slots == null) return result;

        for (String key : slots.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                ConfigurationSection entry = slots.getConfigurationSection(key);
                if (entry == null) continue;
                ItemStack item = entry.getItemStack("item");
                if (item == null || item.getType() == org.bukkit.Material.AIR) continue;
                result.put(slot, new LootEntry(item, entry.getInt("chance", 10)));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public void savePage(int page, Map<Integer, LootEntry> entries) {
        migrateLegacyLoot();
        String path = PAGES + "." + page;
        items.getConfig().set(path, null);
        ConfigurationSection section = items.getConfig().createSection(path);

        for (Map.Entry<Integer, LootEntry> entry : entries.entrySet()) {
            ConfigurationSection slot = section.createSection(String.valueOf(entry.getKey()));
            LootEntry loot = entry.getValue();
            slot.set("item", loot.item());
            slot.set("chance", loot.chance());
        }
        items.save();
    }

    public boolean canAddPage() {
        return pageCount() < MAX_PAGES;
    }

    public void addPage() {
        if (!canAddPage()) return;
        migrateLegacyLoot();
        int next = pageCount();
        ensurePage(next);
        items.save();
    }

    public void removePage(int page) {
        if (pageCount() <= 1) return;
        items.getConfig().set(PAGES + "." + page, null);
        reindexPages();
        items.save();
    }

    public void addItem(String id, ItemStack item, int chance) {
        migrateLegacyLoot();
        ConfigurationSection page = pageSection(0);
        if (page == null) page = items.getConfig().createSection(PAGES + ".0");

        ConfigurationSection slot = page.createSection(id);
        slot.set("item", item);
        slot.set("chance", chance);
        items.save();
    }

    private List<LootEntry> getAllLoot() {
        migrateLegacyLoot();
        List<LootEntry> pool = new ArrayList<>();
        ConfigurationSection pages = pagesSection();
        if (pages != null) {
            for (String pageKey : pages.getKeys(false)) {
                pool.addAll(pageItems(Integer.parseInt(pageKey)).values());
            }
        }
        return pool;
    }

    private void migrateLegacyLoot() {
        ConfigurationSection root = items.getConfig().getConfigurationSection("items");
        if (root == null) return;

        List<String> legacy = new ArrayList<>();
        for (String key : root.getKeys(false)) {
            if (RESERVED.contains(key)) continue;
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section != null && section.contains("item")) legacy.add(key);
        }
        if (legacy.isEmpty()) return;

        ConfigurationSection page = items.getConfig().createSection(PAGES + ".0");
        int slot = 0;
        for (String key : legacy) {
            ConfigurationSection old = root.getConfigurationSection(key);
            if (old == null) continue;
            while (page.contains(String.valueOf(slot))) slot++;
            ConfigurationSection target = page.createSection(String.valueOf(slot++));
            target.set("item", old.getItemStack("item"));
            target.set("chance", old.getInt("chance", 10));
            root.set(key, null);
        }
        items.save();
    }

    private void reindexPages() {
        ConfigurationSection pages = pagesSection();
        if (pages == null) return;

        List<ConfigurationSection> data = new ArrayList<>();
        for (String key : new TreeSet<>(pages.getKeys(false))) {
            ConfigurationSection page = pages.getConfigurationSection(key);
            if (page != null) data.add(page);
        }
        items.getConfig().set(PAGES, null);
        for (int i = 0; i < data.size(); i++) {
            ConfigurationSection target = items.getConfig().createSection(PAGES + "." + i);
            for (String slotKey : data.get(i).getKeys(false)) {
                ConfigurationSection slot = data.get(i).getConfigurationSection(slotKey);
                if (slot != null) {
                    ConfigurationSection clean = target.createSection(slotKey);
                    clean.set("item", slot.getItemStack("item"));
                    clean.set("chance", slot.getInt("chance", 10));
                }
            }
        }
    }

    private void ensurePage(int page) {
        if (pageSection(page) == null) {
            items.getConfig().createSection(PAGES + "." + page);
            items.save();
        }
    }

    private ConfigurationSection pagesSection() {
        return items.getConfig().getConfigurationSection(PAGES);
    }

    private ConfigurationSection pageSection(int page) {
        return items.getConfig().getConfigurationSection(PAGES + "." + page);
    }

    public int getRandomNumber(int min, int max) {
        if (min >= max) return min;
        return min + random.nextInt(max - min + 1);
    }

    public record LootEntry(ItemStack item, int chance) {
    }
}
