package ru.truhot.rdang.schem;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.util.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

@AllArgsConstructor
public class SchemAction {
    private final RDang plugin;
    private final ConfigManager configManager;

    public void spawnSchem(@NotNull Location location, @NotNull String fileName) {
        File schemFile = new File(plugin.getDataFolder() + "/schem/" + fileName);
        if (!schemFile.exists()) {
            File faweFolder = new File(Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit").getDataFolder(), "schematics");
            File alternativeFile = new File(faweFolder, fileName);
            if (alternativeFile.exists()) {
                schemFile = alternativeFile;
            } else {
                File weFolder = new File(Bukkit.getPluginManager().getPlugin("WorldEdit").getDataFolder(), "schematics");
                alternativeFile = new File(weFolder, fileName);
                if (alternativeFile.exists()) {
                    schemFile = alternativeFile;
                }
            }
        }
        if (!schemFile.exists()) return;
        final File finalFile = schemFile;
        ClipboardFormat format = ClipboardFormats.findByFile(finalFile);
        if (format == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                try (FileInputStream fis = new FileInputStream(finalFile);
                     ClipboardReader reader = format.getReader(fis)) {
                    Clipboard clipboard = reader.read();
                    boolean ignoreAir = configManager.getSchem().getBoolean("ignore-air-blocks");
                    ConfigurationSection offsetSection = configManager.getSchem().getConfigurationSection("schem-offset");
                    double ox = offsetSection != null ? offsetSection.getDouble("x") : 0;
                    double oy = offsetSection != null ? offsetSection.getDouble("y") : 0;
                    double oz = offsetSection != null ? offsetSection.getDouble("z") : 0;
                    Location targetLoc = location.clone().add(ox, oy, oz);
                    BlockVector3 targetOrigin = BlockVector3.at(targetLoc.getX(), targetLoc.getY(), targetLoc.getZ());
                    BlockVector3 offset = targetOrigin.subtract(clipboard.getOrigin());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(targetLoc.getWorld()))) {
                                ForwardExtentCopy copy = new ForwardExtentCopy(
                                        clipboard, clipboard.getRegion(), editSession, clipboard.getMinimumPoint().add(offset)
                                );
                                copy.setCopyingEntities(true);

                                if (ignoreAir) {
                                    copy.setSourceMask(com.sk89q.worldedit.function.mask.Masks.negate(
                                            new com.sk89q.worldedit.function.mask.BlockTypeMask(clipboard, com.sk89q.worldedit.world.block.BlockTypes.AIR)
                                    ));
                                }
                                Operations.complete(copy);
                            } catch (Exception e) {
                                Logger.error("Не удалось вставить схему: " + fileName);
                            }
                        }
                    }.runTask(plugin);
                } catch (Exception e) {
                    Logger.error("Ошибка при чтении схемы: " + fileName);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void createBackup(@NotNull Location location, String regionName) {
        int radiusX = configManager.getRegion().getInt("region.size.x", 12);
        int radiusZ = configManager.getRegion().getInt("region.size.z", 12);
        int minY = configManager.getRegion().getInt("region.height.min", 0);
        int maxY = configManager.getRegion().getInt("region.height.max", 255);
        BlockVector3 min = BlockVector3.at(location.getBlockX() - radiusX, minY, location.getBlockZ() - radiusZ);
        BlockVector3 max = BlockVector3.at(location.getBlockX() + radiusX, maxY, location.getBlockZ() + radiusZ);
        com.sk89q.worldedit.regions.CuboidRegion region = new com.sk89q.worldedit.regions.CuboidRegion(BukkitAdapter.adapt(location.getWorld()), min, max);
        File backupFile = new File(plugin.getDataFolder() + "/backups/" + regionName + ".schem");
        if (!backupFile.getParentFile().exists()) backupFile.getParentFile().mkdirs();
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
            com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard clipboard = new com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard(region);
            clipboard.setOrigin(region.getMinimumPoint());
            Operations.complete(new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint()));
            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(backupFile))) {
                writer.write(clipboard);
            }
        } catch (Exception e) {
            Logger.error("Ошибка бекапа: " + regionName);
        }
    }
}