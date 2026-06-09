package ru.truhot.rdang.addchests;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.chest.ChestActions;

public class AddChests {
    private final RDang plugin;
    private final ChestActions actions;

    public AddChests(RDang plugin, ChestActions actions) {
        this.plugin = plugin;
        this.actions = actions;
    }

    public void addChests(Location center, int radiusX, int radiusZ, int minY, int maxY) {
        int startX = center.getBlockX() - radiusX;
        int endX = center.getBlockX() + radiusX;
        int startZ = center.getBlockZ() - radiusZ;
        int endZ = center.getBlockZ() + radiusZ;
        new BukkitRunnable() {
            int currentY = minY;
            @Override
            public void run() {
                if (currentY > maxY) {
                    this.cancel();
                    return;
                }
                for (int x = startX; x <= endX; x++) {
                    for (int z = startZ; z <= endZ; z++) {
                        Block block = center.getWorld().getBlockAt(x, currentY, z);
                        if (isChest(block.getType())) {
                            actions.addChest(block.getLocation());
                        }
                    }
                }
                currentY++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private boolean isChest(Material material) {
        return material == Material.CHEST || material == Material.TRAPPED_CHEST || material == Material.BARREL;
    }
}