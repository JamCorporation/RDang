package ru.truhot.rdang.schem;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    public void createBackup(@NotNull Location location, @NotNull String regionName) {
        createBackup(location, regionName, null);
    }


    public void createBackup(@NotNull Location location, @NotNull String regionName, @Nullable Runnable onComplete) {
        CuboidRegion region = buildBackupRegion(location);
        File backupFile = backupFile(regionName);

        new BukkitRunnable() {
            @Override
            public void run() {
                BlockArrayClipboard clipboard;
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
                    clipboard = new BlockArrayClipboard(region);
                    clipboard.setOrigin(region.getMinimumPoint());
                    ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
                    copy.setCopyingEntities(false);
                    Operations.complete(copy);
                } catch (Exception e) {
                    Logger.error("Ошибка чтения ландшафта для бэкапа: " + regionName);
                    runCallback(onComplete);
                    return;
                }

                BlockArrayClipboard finalClipboard = clipboard;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            if (!backupFile.getParentFile().exists()) {
                                backupFile.getParentFile().mkdirs();
                            }
                            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(backupFile))) {
                                writer.write(finalClipboard);
                            }
                        } catch (Exception e) {
                            Logger.error("Ошибка записи бэкапа: " + regionName);
                        }
                        runCallback(onComplete);
                    }
                }.runTaskAsynchronously(plugin);
            }
        }.runTask(plugin);
    }

    public File backupFile(String regionName) {
        return new File(plugin.getDataFolder(), "backups/" + regionName + ".schem");
    }

    public CuboidRegion buildBackupRegion(Location location) {
        int radiusX = configManager.getRegion().getInt("region.size.x", 12);
        int radiusZ = configManager.getRegion().getInt("region.size.z", 12);
        int minY = configManager.getRegion().getInt("region.height.min", 0);
        int maxY = configManager.getRegion().getInt("region.height.max", 255);
        BlockVector3 min = BlockVector3.at(location.getBlockX() - radiusX, minY, location.getBlockZ() - radiusZ);
        BlockVector3 max = BlockVector3.at(location.getBlockX() + radiusX, maxY, location.getBlockZ() + radiusZ);
        return new CuboidRegion(BukkitAdapter.adapt(location.getWorld()), min, max);
    }

    private void runCallback(@Nullable Runnable onComplete) {
        if (onComplete == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                onComplete.run();
            }
        }.runTask(plugin);
    }
}
