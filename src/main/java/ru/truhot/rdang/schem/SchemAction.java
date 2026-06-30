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
        spawnSchem(location, fileName, (java.util.function.Consumer<BlockVector3[]>) null);
    }

    public void spawnSchem(@NotNull Location location, @NotNull String fileName, @Nullable Runnable onComplete) {
        spawnSchem(location, fileName, onComplete == null ? null : (java.util.function.Consumer<BlockVector3[]>) bounds -> onComplete.run());
    }

    public void spawnSchem(@NotNull Location location, @NotNull String fileName, @Nullable java.util.function.Consumer<BlockVector3[]> onComplete) {
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
            if (onComplete != null) onComplete.accept(null);
            return;
        }
        final File finalFile = schemFile;
        ClipboardFormat format = ClipboardFormats.findByFile(finalFile);
        if (format == null) {
            Logger.error("[Schem] Формат не распознан для: " + finalFile.getAbsolutePath());
            if (onComplete != null) onComplete.accept(null);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try (FileInputStream fis = new FileInputStream(finalFile);
                     ClipboardReader reader = format.getReader(fis)) {
                    Clipboard clipboard = reader.read();
                    boolean ignoreAir = getSchemBoolean(fileName, "ignore-air-blocks", true);
                    ConfigurationSection offsetSection = getSchemOffsetSection(fileName);
                    double ox = offsetSection != null ? offsetSection.getDouble("x") : 0;
                    double oy = offsetSection != null ? offsetSection.getDouble("y") : 0;
                    double oz = offsetSection != null ? offsetSection.getDouble("z") : 0;
                    // Ищем самый нижний не-воздушный Y в схемате, чтобы убрать воздушный зазор снизу.
                    // Если нижние слои клипборда — сплошной воздух, данж будет висеть в воздухе
                    // ровно на их количество. Компенсируем сдвигом targetLoc вниз.
                    int clipMinY = clipboard.getMinimumPoint().getY();
                    int clipMaxY = clipboard.getMaximumPoint().getY();
                    int clipMinX = clipboard.getMinimumPoint().getX();
                    int clipMaxX = clipboard.getMaximumPoint().getX();
                    int clipMinZ = clipboard.getMinimumPoint().getZ();
                    int clipMaxZ = clipboard.getMaximumPoint().getZ();
                    int lowestNonAirY = Integer.MAX_VALUE;
                    ySearch:
                    for (int scanY = clipMinY; scanY <= clipMaxY; scanY++) {
                        for (int scanX = clipMinX; scanX <= clipMaxX; scanX++) {
                            for (int scanZ = clipMinZ; scanZ <= clipMaxZ; scanZ++) {
                                BlockType bt = clipboard.getBlock(BlockVector3.at(scanX, scanY, scanZ)).getBlockType();
                                if (bt != null && !bt.getMaterial().isAir()) {
                                    lowestNonAirY = scanY;
                                    break ySearch;
                                }
                            }
                        }
                    }
                    int yShift = (lowestNonAirY != Integer.MAX_VALUE)
                            ? (lowestNonAirY - clipboard.getOrigin().getY()) : 0;
                    Location targetLoc = location.clone().add(ox, oy - yShift, oz);
                    BlockVector3 targetOrigin = BlockVector3.at(targetLoc.getX(), targetLoc.getY(), targetLoc.getZ());
                    BlockVector3 offset = targetOrigin.subtract(clipboard.getOrigin());

                    boolean isFawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;

                    boolean clearArea = getSchemBoolean(fileName, "clear-area-before-paste", true);

                    BukkitRunnable pasteTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            final BlockVector3 pasteMin = clipboard.getMinimumPoint().add(offset);
                            final BlockVector3 pasteMax = clipboard.getMaximumPoint().add(offset);
                            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(targetLoc.getWorld()))) {
                                // Очищаем объём будущей схематики в AIR, чтобы блоки террейна
                                // (земля/камень при спавне в земле) не замуровывали сундуки и не
                                // оставались внутри данжа. Сама схематика вставляется поверх.
                                if (clearArea) {
                                    int clearBottomY = Math.min(pasteMin.getY(), location.getBlockY());
                                    BlockVector3 clearBottom = BlockVector3.at(pasteMin.getX(), clearBottomY, pasteMin.getZ());
                                    CuboidRegion schemRegion = new CuboidRegion(
                                            BukkitAdapter.adapt(targetLoc.getWorld()), clearBottom, pasteMax);
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
                                        onComplete.accept(new BlockVector3[]{pasteMin, pasteMax});
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
                                onComplete.accept(null);
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

    private boolean getSchemBoolean(@Nullable String fileName, String key, boolean defaultValue) {
        if (fileName != null) {
            ConfigurationSection perSchem = configManager.getSchem().getConfigurationSection("schematics." + fileName);
            if (perSchem != null && perSchem.contains(key)) {
                return perSchem.getBoolean(key);
            }
        }
        return configManager.getSchem().getBoolean(key, defaultValue);
    }

    private ConfigurationSection getSchemOffsetSection(@Nullable String fileName) {
        if (fileName != null) {
            ConfigurationSection perSchem = configManager.getSchem().getConfigurationSection("schematics." + fileName);
            if (perSchem != null && perSchem.contains("schem-offset")) {
                return perSchem.getConfigurationSection("schem-offset");
            }
        }
        return configManager.getSchem().getConfigurationSection("schem-offset");
    }

    public void clearVegetation(@NotNull Location center) {
        clearVegetation(center, null, null);
    }

    public void clearVegetation(@NotNull Location center, @Nullable Runnable onComplete) {
        clearVegetation(center, null, onComplete);
    }

    public void clearVegetation(@NotNull Location center, @Nullable String fileName, @Nullable Runnable onComplete) {
        if (!getSchemBoolean(fileName, "clear-vegetation", true)) {
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
