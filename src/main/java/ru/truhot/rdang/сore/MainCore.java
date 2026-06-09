package ru.truhot.rdang.сore;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.сore.managers.EventManager;
import ru.truhot.rdang.сore.managers.ItemChecker;
import ru.truhot.rdang.сore.managers.LootManager;
import ru.truhot.rdang.сore.managers.ChestManager;

public class MainCore implements Listener {
    private final Storage items;
    private final Storage shulkers;
    private final ConfigManager configManager;
    private final LootManager lootManager;
    private final ChestManager chestManager;
    private final ItemChecker itemChecker;
    private final EventManager eventHandler;

    public MainCore(Storage items, Storage shulkers, ConfigManager configManager, LootManager lootManager, ChestManager chestManager, ItemChecker itemChecker, EventManager eventHandler) {
        this.items = items;
        this.shulkers = shulkers;
        this.configManager = configManager;
        this.lootManager = lootManager;
        this.chestManager = chestManager;
        this.itemChecker = itemChecker;
        this.eventHandler = eventHandler;
    }

    public Storage getItems() {
        return items;
    }

    public Storage getShulkers() {
        return shulkers;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public ChestManager getChestManager() {
        return chestManager;
    }

    public ItemChecker getItemChecker() {
        return itemChecker;
    }

    public EventManager getEventHandler() {
        return eventHandler;
    }

    public void fillRandomLoot(Inventory inventory) {
        lootManager.fillRandomLoot(inventory);
    }

    public void addChest(Location location) {
        chestManager.addChest(location);
    }

    public void addChestConfig(String id, Location location, boolean opened) {
        chestManager.addChestConfig(id, location, opened);
    }

    public void addItem(String id, ItemStack item, int chance) {
        lootManager.addItem(id, item, chance);
    }

    public boolean isChest(Block placedBlock) {
        return chestManager.isChest(placedBlock);
    }

    public boolean isValidKey(ItemStack item) {
        return itemChecker.isValidKey(item);
    }

    public boolean isKeyItem(ItemStack item) {
        return itemChecker.isKeyItem(item);
    }

    public boolean isCompassItem(ItemStack item) {
        return itemChecker.isCompassItem(item);
    }

    public int getRandomNumber(int min, int max) {
        return lootManager.getRandomNumber(min, max);
    }
}