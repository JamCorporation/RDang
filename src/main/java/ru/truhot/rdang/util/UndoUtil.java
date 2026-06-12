package ru.truhot.rdang.util;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.schem.SchemAction;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class UndoUtil {
    private final ConfigManager configManager;
    private final Storage shulkers;
    private final Storage blockStorage;
    private final RDang plugin;
    private final SchemAction schemAction;
    private final Map<String, BukkitRunnable> activeTimers = new HashMap<>();

    public static class UndoResult {
        public final int chestCount;
        public final String worldName;
        public final boolean found;

        public UndoResult(int chestCount, String worldName, boolean found) {
            this.chestCount = chestCount;
            this.worldName = worldName;
            this.found = found;
        }
    }

    public UndoUtil(ConfigManager configManager, Storage shulkers, Storage blockStorage, RDang plugin, SchemAction schemAction) {
        this.configManager = configManager;
        this.shulkers = shulkers;
        this.blockStorage = blockStorage;
        this.plugin = plugin;
        this.schemAction = schemAction;
    }

    public UndoUtil(ConfigManager configManager, Storage shulkers, Storage blockStorage, RDang plugin) {
        this(configManager, shulkers, blockStorage, plugin, new SchemAction(plugin, configManager));
    }

    public void saveDungeonData(String regionName, World world, BlockVector3 minPoint) {
        synchronized (blockStorage) {
            String path = "history." + regionName;
            ConfigurationSection section = blockStorage.getConfig().createSection(path);
            section.set("world", world.getName());
            section.set("x", minPoint.getX());
            section.set("y", minPoint.getY());
            section.set("z", minPoint.getZ());
            blockStorage.save();
        }
    }

    public void performUndo(String regionName, Consumer<UndoResult> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String path = "history." + regionName;
                ConfigurationSection data = blockStorage.getConfig().getConfigurationSection(path);
                if (data == null) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            callback.accept(new UndoResult(0, "Неизвестно", false));
                        }
                    }.runTask(plugin);
                    return;
                }

                String worldName = data.getString("world");
                World world = Bukkit.getWorld(Objects.requireNonNull(worldName));
                if (world == null) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            callback.accept(new UndoResult(0, worldName, false));
                        }
                    }.runTask(plugin);
                    return;
                }

                BlockVector3 minPoint = BlockVector3.at(data.getInt("x"), data.getInt("y"), data.getInt("z"));

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        BukkitRunnable existingTimer = activeTimers.remove(regionName);
                        if (existingTimer != null) {
                            try { existingTimer.cancel(); } catch (IllegalStateException ignored) {}
                        }

                        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                        RegionManager manager = container.get(BukkitAdapter.adapt(world));
                        ProtectedRegion region = (manager != null) ? manager.getRegion(regionName) : null;

                        final ProtectedRegion finalRegion = region;
                        final RegionManager finalManager = manager;

                        restoreLandscape(regionName, world, minPoint, () -> {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    int removedCount = 0;
                                    if (finalRegion != null) {
                                        removedCount = removeChests(finalRegion, world);
                                        if (finalManager != null) {
                                            finalManager.removeRegion(regionName);
                                        }
                                    }

                                    // Удаляем запись из истории ТОЛЬКО после попытки восстановления
                                    blockStorage.getConfig().set(path, null);
                                    blockStorage.save();
                                    
                                    if (removedCount > 0) {
                                        shulkers.save();
                                    }

                                    final int finalRemovedCount = removedCount;
                                    callback.accept(new UndoResult(finalRemovedCount, worldName, true));
                                }
                            }.runTask(plugin);
                        });
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    private void restoreLandscape(String regionName, World world, BlockVector3 minPoint, Runnable onComplete) {
        File backupFile = schemAction.backupFile(regionName);
        if (!backupFile.exists()) {
            Logger.warn("Бэкап не найден для данжа: " + regionName + ". Пропускаем восстановление блоков.");
            if (onComplete != null) onComplete.run();
            return;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(backupFile);
        if (format == null) {
            Logger.error("Неизвестный формат бэкапа для: " + regionName);
            if (onComplete != null) onComplete.run();
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try (FileInputStream fis = new FileInputStream(backupFile);
                     ClipboardReader reader = format.getReader(fis)) {
                    Clipboard clipboard = reader.read();
                    BlockVector3 offset = minPoint.subtract(clipboard.getMinimumPoint());
                    boolean isFawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;

                    BukkitRunnable pasteTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                                ForwardExtentCopy copy = new ForwardExtentCopy(
                                        clipboard, clipboard.getRegion(), editSession, clipboard.getMinimumPoint().add(offset)
                                );
                                copy.setCopyingEntities(false);
                                Operations.complete(copy);
                                editSession.flushSession();
                            } catch (Exception e) {
                                Logger.error("Ошибка при восстановлении блоков для данжа " + regionName + ": " + e.getMessage());
                            } finally {
                                // Удаляем файл бэкапа только после успешной или завершенной попытки вставки
                                if (backupFile.exists() && !backupFile.delete()) {
                                    Logger.warn("Не удалось удалить файл бэкапа: " + backupFile.getName());
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
                        }
                    };

                    if (isFawe) {
                        pasteTask.runTaskAsynchronously(plugin);
                    } else {
                        pasteTask.runTask(plugin);
                    }
                } catch (Exception e) {
                    Logger.error("Ошибка чтения файла бэкапа для " + regionName + ": " + e.getMessage());
                    if (onComplete != null) onComplete.run();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void scheduleAutoUndo(String regionName, World world, ProtectedRegion region) {
        String timeStr = configManager.getAuto().getString("auto.time");
        long seconds = TimeUtil.parse(timeStr);
        String rawMsg = configManager.getMessages().getString("messages.actionbar_timer");

        BukkitRunnable oldTask = activeTimers.remove(regionName);
        if (oldTask != null) {
            try { oldTask.cancel(); } catch (IllegalStateException ignored) {}
        }
        BukkitRunnable task = new BukkitRunnable() {
            private long timeLeft = seconds;
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    performUndo(regionName, result -> {});
                    activeTimers.remove(regionName);
                    this.cancel();
                    return;
                }
                String formattedTime = TimeUtil.format(timeLeft);
                String finalMsg = MessageUtil.colorize(rawMsg.replace("{time}", formattedTime));
                for (Player player : world.getPlayers()) {
                    if (region.contains(BukkitAdapter.asBlockVector(player.getLocation()))) {
                        player.spigot().sendMessage(
                                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent(finalMsg)
                        );
                    }
                }
                timeLeft--;
            }
        };
        activeTimers.put(regionName, task);
        task.runTaskTimer(plugin, 0L, 20L);
    }

    public void shutdown() {
        for (BukkitRunnable task : activeTimers.values()) {
            try {
                task.cancel();
            } catch (IllegalStateException ignored) {}
        }
        activeTimers.clear();
    }

    private int removeChests(ProtectedRegion region, World world) {
        ConfigurationSection locs = shulkers.getConfig().getConfigurationSection("locs");
        if (locs == null) return 0;
        
        List<String> toRemove = new java.util.ArrayList<>();
        for (String key : locs.getKeys(false)) {
            Location loc = locs.getLocation(key + ".location");
            if (loc != null && loc.getWorld().getName().equals(world.getName())) {
                if (region.contains(BukkitAdapter.asBlockVector(loc))) {
                    toRemove.add(key);
                }
            }
        }
        
        for (String key : toRemove) {
            locs.set(key, null);
        }
        
        return toRemove.size();
    }

    private Location getRegionCenter(ProtectedRegion region, World world) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        return new Location(world, (min.getX() + max.getX()) / 2.0, (min.getY() + max.getY()) / 2.0, (min.getZ() + max.getZ()) / 2.0);
    }
}
