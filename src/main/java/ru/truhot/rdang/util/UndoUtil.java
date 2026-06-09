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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class UndoUtil {
    private final ConfigManager configManager;
    private final Storage shulkers;
    private final Storage blockStorage;
    private final RDang plugin;
    private final SchemAction schemAction;
    private final Set<BukkitRunnable> activeTimers = new HashSet<>();

    public static class UndoResult {
        public final int shulkerCount;
        public final String worldName;
        public final boolean found;

        public UndoResult(int shulkerCount, String worldName, boolean found) {
            this.shulkerCount = shulkerCount;
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
        String path = "history." + regionName;
        ConfigurationSection section = blockStorage.getConfig().createSection(path);
        section.set("world", world.getName());
        section.set("x", minPoint.getX());
        section.set("y", minPoint.getY());
        section.set("z", minPoint.getZ());
        new BukkitRunnable() {
            @Override
            public void run() {
                blockStorage.save();
            }
        }.runTaskAsynchronously(plugin);
    }

    public UndoResult performUndo(String regionName) {
        String path = "history." + regionName;
        ConfigurationSection data = blockStorage.getConfig().getConfigurationSection(path);
        if (data == null) return new UndoResult(0, "Неизвестно", false);
        String worldName = data.getString("world");
        World world = Bukkit.getWorld(Objects.requireNonNull(worldName));
        if (world == null) return new UndoResult(0, worldName, false);
        BlockVector3 minPoint = BlockVector3.at(data.getInt("x"), data.getInt("y"), data.getInt("z"));
        blockStorage.getConfig().set(path, null);
        blockStorage.save();
        int removedCount = 0;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager != null && manager.hasRegion(regionName)) {
            ProtectedRegion region = manager.getRegion(regionName);
            removedCount = removeShulkers(getRegionCenter(region, world), world);
            manager.removeRegion(regionName);
            if (removedCount > 0) shulkers.save();
        }
        restoreLandscape(regionName, world, minPoint);
        return new UndoResult(removedCount, worldName, true);
    }

    private void restoreLandscape(String regionName, World world, BlockVector3 minPoint) {
        File backupFile = schemAction.backupFile(regionName);
        if (!backupFile.exists()) {
            Logger.warn("Бэкап не найден, ландшафт не восстановлен: " + regionName);
            return;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(backupFile);
        if (format == null) {
            Logger.error("Неизвестный формат бэкапа: " + regionName);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try (FileInputStream fis = new FileInputStream(backupFile);
                     ClipboardReader reader = format.getReader(fis)) {
                    Clipboard clipboard = reader.read();
                    BlockVector3 offset = minPoint.subtract(clipboard.getMinimumPoint());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            boolean restored = false;
                            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                                ForwardExtentCopy copy = new ForwardExtentCopy(
                                        clipboard, clipboard.getRegion(), editSession, clipboard.getMinimumPoint().add(offset)
                                );
                                copy.setCopyingEntities(false);
                                Operations.complete(copy);
                                restored = true;
                            } catch (Exception e) {
                                Logger.error("Ошибка восстановления ландшафта: " + regionName);
                            }
                            if (restored && backupFile.exists() && !backupFile.delete()) {
                                Logger.warn("Не удалось удалить бэкап после восстановления: " + regionName);
                            }
                        }
                    }.runTask(plugin);
                } catch (Exception e) {
                    Logger.error("Ошибка чтения бэкапа: " + regionName);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void scheduleAutoUndo(String regionName, World world, ProtectedRegion region) {
        String timeStr = configManager.getAuto().getString("auto.time");
        long seconds = TimeUtil.parse(timeStr);
        String rawMsg = configManager.getMessages().getString("messages.actionbar_timer");

        BukkitRunnable task = new BukkitRunnable() {
            private long timeLeft = seconds;
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    performUndo(regionName);
                    activeTimers.remove(this);
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
        activeTimers.add(task);
        task.runTaskTimer(plugin, 0L, 20L);
    }

    public void shutdown() {
        for (BukkitRunnable task : activeTimers) {
            try {
                task.cancel();
            } catch (IllegalStateException ignored) {}
        }
        activeTimers.clear();
    }

    private int removeShulkers(Location center, World world) {
        ConfigurationSection locs = shulkers.getConfig().getConfigurationSection("locs");
        if (locs == null) return 0;
        int radiusX = configManager.getRegion().getInt("region.size.x", 12);
        int radiusZ = configManager.getRegion().getInt("region.size.z", 12);
        List<String> toRemove = locs.getKeys(false).stream()
                .filter(key -> {
                    Location loc = locs.getLocation(key + ".location");
                    return loc != null &&
                            loc.getWorld().getName().equals(world.getName()) &&
                            Math.abs(loc.getBlockX() - center.getBlockX()) <= radiusX &&
                            Math.abs(loc.getBlockZ() - center.getBlockZ()) <= radiusZ;
                })
                .toList();
        toRemove.forEach(key -> locs.set(key, null));
        return toRemove.size();
    }

    private Location getRegionCenter(ProtectedRegion region, World world) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        return new Location(world, (min.getX() + max.getX()) / 2.0, (min.getY() + max.getY()) / 2.0, (min.getZ() + max.getZ()) / 2.0);
    }
}
