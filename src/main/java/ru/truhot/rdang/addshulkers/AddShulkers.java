package ru.truhot.rdang.addshulkers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.shulker.ShulkerActions;

public class AddShulkers {
    private final RDang plugin;
    private final ShulkerActions actions;

    public AddShulkers(RDang plugin, ShulkerActions actions) {
        this.plugin = plugin;
        this.actions = actions;
    }

    public void addShulkers(Location center, int radiusX, int radiusZ, int minY, int maxY) {
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
                        if (isShulker(block.getType())) {
                            actions.addShulker(block.getLocation());
                        }
                    }
                }
                currentY++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private boolean isShulker(Material material) {
        return material.name().endsWith("SHULKER_BOX");
    }
}