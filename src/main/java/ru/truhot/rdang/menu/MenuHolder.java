package ru.truhot.rdang.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class MenuHolder implements InventoryHolder {

    private final MenuType type;
    private final int page;
    private Inventory inventory;

    public MenuHolder(MenuType type, int page) {
        this.type = type;
        this.page = page;
    }

    public MenuType getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
