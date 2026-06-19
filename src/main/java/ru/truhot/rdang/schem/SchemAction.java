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
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
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
import java.util.ArrayList;
import java.util.List;

public class SchemAction {
    private final RDang plugin;
    private final ConfigManager configManager;

    public SchemAction(RDang plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void spawnSchem(@NotNull Location location, @NotNull String fileName) {
        spawnSchem(location, fileName, null);
    }

    public void spawnSchem(@NotNull Location location, @NotNull String fileName, @Nullable Runnable onComplete) {
        File schemFile = new File(plugin.getDataFolder() + "/schem/" + fileName);
        if (!schemFile.exists()) {
            org.bukkit.plugin.Plugin fawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
            if (fawe != null) {
                File faweFolder = new File(fawe.getDataFolder(), "schematics");
                File alternativeFile = new File(faweFolder, fileName);
                if (alternativeFile.exists()) {
                    schemFile = alternativeFile;
                }
            }
            if (!schemFile.exists()) {
                org.bukkit.plugin.Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");
                if (we != null) {
                    File weFolder = new File(we.getDataFolder(), "schematics");
                    File alternativeFile = new File(weFolder, fileName);
                    if (alternativeFile.exists()) {
                        schemFile = alternativeFile;
                    }
                }
            }
        }
        if (!schemFile.exists()) {
            Logger.error("[Schem] Файл не найден: " + fileName);
            if (onComplete != null) onComplete.run();
            return;
        }
        final File finalFile = schemFile;
        ClipboardFormat format = ClipboardFormats.findByFile(finalFile);
        if (format == null) {
            Logger.error("[Schem] Формат не распознан для: " + finalFile.getAbsolutePath());
            if (onComplete != null) onComplete.run();
            return;
        }

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

                    boolean isFawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;

                    boolean clearArea = configManager.getSchem().getBoolean("clear-area-before-paste", true);

                    BukkitRunnable pasteTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(targetLoc.getWorld()))) {
                                // Очищаем объём будущей схематики в AIR, чтобы блоки террейна
                                // (земля/камень при спавне в земле) не замуровывали сундуки и не
                                // оставались внутри данжа. Сама схематика вставляется поверх.
                                if (clearArea) {
                                    BlockVector3 pasteMin = clipboard.getMinimumPoint().add(offset);
                                    BlockVector3 pasteMax = clipboard.getMaximumPoint().add(offset);
                                    CuboidRegion schemRegion = new CuboidRegion(
                                            BukkitAdapter.adapt(targetLoc.getWorld()), pasteMin, pasteMax);
                                    editSession.setBlocks((com.sk89q.worldedit.regions.Region) schemRegion,
                                            BlockTypes.AIR.getDefaultState());
                                }

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
                                editSession.flushSession();
                            } catch (Exception e) {
                                Logger.error("Не удалось вставить схему: " + fileName + " | " + e.getMessage());
                                e.printStackTrace();
                            }
                            if (onComplete != null) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        onComplete.run();
                                    }
                                }.runTask(plugin);
                            }
                        }
                    };

                    if (isFawe) {
                        pasteTask.runTaskAsynchronously(plugin);
                    } else {
                        pasteTask.runTask(plugin);
                    }

                } catch (Exception e) {
                    Logger.error("Ошибка при чтении схемы: " + fileName + " | " + e.getMessage());
                    e.printStackTrace();
                    if (onComplete != null) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                onComplete.run();
                            }
                        }.runTask(plugin);
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void createBackup(@NotNull Location location, @NotNull String regionName) {
        createBackup(location, regionName, null);
    }


    public void createBackup(@NotNull Location location, @NotNull String regionName, @Nullable java.util.function.Consumer<Boolean> onComplete) {
        CuboidRegion region = buildBackupRegion(location);
        File backupFile = backupFile(regionName);
        boolean isFawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;

        Runnable doRead = () -> {
            BlockArrayClipboard clipboard;
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
                clipboard = new BlockArrayClipboard(region);
                clipboard.setOrigin(region.getMinimumPoint());
                ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
                copy.setCopyingEntities(false);
                Operations.complete(copy);
            } catch (Exception e) {
                Logger.error("Ошибка чтения ландшафта для бэкапа: " + regionName + " | " + e.getMessage());
                if (onComplete != null) {
                    new BukkitRunnable() { @Override public void run() { onComplete.accept(false); } }.runTask(plugin);
                }
                return;
            }

            BlockArrayClipboard finalClipboard = clipboard;
            new BukkitRunnable() {
                @Override
                public void run() {
                    boolean success = false;
                    try {
                        if (!backupFile.getParentFile().exists()) backupFile.getParentFile().mkdirs();
                        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(backupFile))) {
                            writer.write(finalClipboard);
                            success = true;
                        }
                    } catch (Exception e) {
                        Logger.error("Ошибка записи бэкапа: " + regionName + " | " + e.getMessage());
                    }
                    final boolean finalSuccess = success;
                    if (onComplete != null) {
                        new BukkitRunnable() { @Override public void run() { onComplete.accept(finalSuccess); } }.runTask(plugin);
                    }
                }
            }.runTaskAsynchronously(plugin);
        };

        if (isFawe) {
            new BukkitRunnable() { @Override public void run() { doRead.run(); } }.runTaskAsynchronously(plugin);
        } else {
            new BukkitRunnable() { @Override public void run() { doRead.run(); } }.runTask(plugin);
        }
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

    public void clearVegetation(@NotNull Location center) {
        clearVegetation(center, null);
    }

    public void clearVegetation(@NotNull Location center, @Nullable Runnable onComplete) {
        if (!configManager.getSchem().getBoolean("clear-vegetation", true)) {
            runCallback(onComplete);
            return;
        }
        boolean isFawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
        CuboidRegion region = buildBackupRegion(center);

        Runnable task = () -> {
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(center.getWorld()))) {
                List<BlockType> vegTypes = new ArrayList<>();
                for (BlockType type : BlockType.REGISTRY.values()) {
                    if (type == null) continue;
                    String id = type.getId();
                    if (id.contains("_log") || id.contains("_leaves") || id.contains("_wood")
                            || id.contains("_sapling") || id.contains("_mushroom")
                            || id.equals("minecraft:grass") || id.equals("minecraft:tall_grass")
                            || id.equals("minecraft:fern") || id.equals("minecraft:large_fern")
                            || id.equals("minecraft:dead_bush") || id.equals("minecraft:vine")
                            || id.equals("minecraft:bamboo") || id.equals("minecraft:bamboo_sapling")
                            || id.equals("minecraft:sugar_cane") || id.equals("minecraft:cactus")
                            || id.contains("_flower") || id.contains("_tulip") || id.equals("minecraft:dandelion")
                            || id.equals("minecraft:poppy") || id.equals("minecraft:blue_orchid")
                            || id.equals("minecraft:allium") || id.equals("minecraft:oxeye_daisy")
                            || id.equals("minecraft:cornflower") || id.equals("minecraft:lily_of_the_valley")
                            || id.equals("minecraft:sunflower") || id.equals("minecraft:lilac")
                            || id.equals("minecraft:peony") || id.equals("minecraft:rose_bush")
                            || id.contains("_stem") || id.contains("_vine") || id.contains("fungus")
                            || id.equals("minecraft:hanging_roots") || id.equals("minecraft:glow_lichen")
                            || id.equals("minecraft:moss_block") || id.equals("minecraft:moss_carpet")
                            || id.equals("minecraft:spore_blossom") || id.equals("minecraft:azalea")
                            || id.equals("minecraft:flowering_azalea") || id.equals("minecraft:azalea_leaves")
                            || id.equals("minecraft:flowering_azalea_leaves")
                            || id.equals("minecraft:brown_mushroom") || id.equals("minecraft:red_mushroom")
                    ) {
                        vegTypes.add(type);
                    }
                }
                BlockTypeMask mask = new BlockTypeMask(editSession, vegTypes);
                editSession.replaceBlocks(region, mask, BlockTypes.AIR.getDefaultState());
                editSession.flushSession();
            } catch (Exception e) {
                Logger.error("Ошибка при очистке растительности: " + e.getMessage());
            }
            runCallback(onComplete);
        };

        if (isFawe) {
            new BukkitRunnable() { @Override public void run() { task.run(); } }.runTaskAsynchronously(plugin);
        } else {
            new BukkitRunnable() { @Override public void run() { task.run(); } }.runTask(plugin);
        }
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
