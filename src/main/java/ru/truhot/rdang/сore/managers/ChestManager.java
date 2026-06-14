package ru.truhot.rdang.сore.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.truhot.rdang.storage.Storage;

import java.util.UUID;

public class ChestManager {
    private final Storage chests;
    private final LootManager lootManager;
    private Plugin plugin;

    public ChestManager(Storage chests, LootManager lootManager) {
        this.chests = chests;
        this.lootManager = lootManager;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    private final java.util.Map<Location, String> locationToId = new java.util.HashMap<>();

    public void loadCache() {
        locationToId.clear();
        ConfigurationSection locs = chests.getConfig().getConfigurationSection("locs");
        if (locs == null) return;
        for (String id : locs.getKeys(false)) {
            Location loc = locs.getLocation(id + ".location");
            if (loc != null) {
                locationToId.put(loc, id);
            }
        }
    }

    public String getChestId(Location loc) {
        return locationToId.get(loc);
    }

    public void removeChestFromCache(Location loc) {
        locationToId.remove(loc);
    }

    public void addChest(Location location) {
        if (!(location.getBlock().getState() instanceof Container container))
            return;
        lootManager.fillRandomLoot(container.getInventory());
        String uuid = UUID.randomUUID().toString();
        addChestConfig(uuid, location, false);
    }

    public void addChestConfig(String id, Location location, boolean opened) {
        ConfigurationSection itemsSection = chests.getConfig().getConfigurationSection("locs");
        if (itemsSection == null) {
            itemsSection = chests.getConfig().createSection("locs");
        }
        ConfigurationSection chestSection = itemsSection.createSection(id);
        chestSection.set("location", location);
        chestSection.set("opened", opened);
        locationToId.put(location, id);
        if (plugin != null) {
            new BukkitRunnable() {
                @Override public void run() { chests.save(); }
            }.runTaskAsynchronously(plugin);
        } else {
            chests.save();
        }
    }

    public boolean isChest(Block placedBlock) {
        Material type = placedBlock.getType();
        return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL;
    }
}