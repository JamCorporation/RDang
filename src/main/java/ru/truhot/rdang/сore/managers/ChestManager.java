package ru.truhot.rdang.сore.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import ru.truhot.rdang.storage.Storage;

import java.util.UUID;

public class ChestManager {
    private final Storage chests;
    private final LootManager lootManager;

    public ChestManager(Storage chests, LootManager lootManager) {
        this.chests = chests;
        this.lootManager = lootManager;
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
            chests.getConfig().createSection("locs");
            addChestConfig(id, location, opened);
        } else {
            itemsSection = itemsSection.createSection(String.valueOf(id));
            itemsSection.set("location", location);
            itemsSection.set("opened", opened);
            chests.save();
        }
    }

    public boolean isChest(Block placedBlock) {
        Material type = placedBlock.getType();
        return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL;
    }
}