package ru.truhot.rdang.menu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class MenuHolder implements InventoryHolder {

    private final MenuType type;
    private final int page;
    private Inventory inventory;

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
