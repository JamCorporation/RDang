package ru.truhot.rdang.dung;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import ru.truhot.rdang.addchests.AddChests;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.data.DangData;
import ru.truhot.rdang.schem.SchemAction;
import ru.truhot.rdang.util.CoreProtectManager;
import ru.truhot.rdang.util.UndoUtil;
import ru.truhot.rdang.util.logger.Logger;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

public class DungActions {
    private final SchemAction schemAction;
    private final AddChests addChests;
    private final ConfigManager configManager;
    private final UndoUtil undoUtil;
    private final CoreProtectManager coreProtectManager;

    public DungActions(SchemAction schemAction, AddChests addChests, ConfigManager configManager, UndoUtil undoUtil, CoreProtectManager coreProtectManager) {
        this.schemAction = schemAction;
        this.addChests = addChests;
        this.configManager = configManager;
        this.undoUtil = undoUtil;
        this.coreProtectManager = coreProtectManager;
    }

    public SchemAction getSchemAction() {
        return schemAction;
    }

    public AddChests getAddChests() {
        return addChests;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public UndoUtil getUndoUtil() {
        return undoUtil;
    }

    public CoreProtectManager getCoreProtectManager() {
        return coreProtectManager;
    }

    public void spawn(@NotNull Location loc) {
        final World world = loc.getWorld();
        final List<DangData> dangDataList = configManager.getDangManager().getDangs();
        if (dangDataList.isEmpty()) return;

        final Biome currentBiome = world.getBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        final int freeId = getFreeId();

        new BukkitRunnable() {
            @Override
            public void run() {
                int minDist = configManager.getRegion().getInt("check.distance-dangs");
                boolean checkOtherRegions = configManager.getRegion().getBoolean("check.check_other_regions");
                if (checkDistance(loc, minDist)) return;
                if (checkOtherRegions && checkInside(loc)) return;

                int radiusX = configManager.getRegion().getInt("region.size.x");
                int radiusZ = configManager.getRegion().getInt("region.size.z");
                if (coreProtectManager != null && coreProtectManager.isAvailable() && coreProtectManager.hasPlayerBuilds(loc, radiusX, radiusZ)) {
                    Logger.info("Данж не заспавнен: в радиусе найдены постройки игроков (" + loc.getBlockX() + ", " + loc.getBlockZ() + ").");
                    return;
                }

                String nameFormat = configManager.getRegion().getString("region.name_format");
                String regionName = nameFormat.replace("{id}", String.valueOf(freeId));

                for (int i = 0; i < 20; i++) {
                    DangData dangData = dangDataList.get(new Random().nextInt(dangDataList.size()));
                    if (dangData.getWorld().equals(world.getName()) && dangData.getBiome().contains(currentBiome)) {
                        int minY = configManager.getRegion().getInt("region.height.min");
                        int maxY = configManager.getRegion().getInt("region.height.max");
                        BlockVector3 minPoint = BlockVector3.at(loc.getBlockX() - radiusX, minY, loc.getBlockZ() - radiusZ);
                        DangData selected = dangData;

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                schemAction.createBackup(loc, regionName, (success) -> {
                                    if (!success) {
                                        Logger.error("Спавн данжа отменен: не удалось создать бэкап ландшафта.");
                                        return;
                                    }
                                    undoUtil.saveDungeonData(regionName, world, minPoint);
                                    schemAction.spawnSchem(loc, selected.getFileName(), () -> {
                                        addChests.addChests(loc, radiusX, radiusZ, minY, maxY);
                                        buildRegion(loc.getBlockX(), loc.getBlockZ(), world, freeId);
                                    });
                                });
                            }
                        }.runTask(configManager.getPlugin());
                        return;
                    }
                }
            }
        }.runTaskAsynchronously(configManager.getPlugin());
    }

    public String buildRegion(int x, int z, World worldBukkit, int id) {
        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        int radiusX = configManager.getRegion().getInt("region.size.x");
        int radiusZ = configManager.getRegion().getInt("region.size.z");
        int minY = configManager.getRegion().getInt("region.height.min");
        int maxY = configManager.getRegion().getInt("region.height.max");
        String nameFormat = configManager.getRegion().getString("region.name_format");
        String regionName = nameFormat.replace("{id}", String.valueOf(id));
        if (container != null) {
            final RegionManager regionManager = container.get(BukkitAdapter.adapt(worldBukkit));
            if (regionManager != null) {
                final BlockVector3 minPoint = BlockVector3.at(x - radiusX, minY, z - radiusZ);
                final BlockVector3 maxPoint = BlockVector3.at(x + radiusX, maxY, z + radiusZ);
                final ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, minPoint, maxPoint);
                synchronized (this) {
                    applyFlags(region);
                }
                regionManager.addRegion(region);
            }
        }
        return regionName;
    }

    private final java.util.concurrent.atomic.AtomicInteger lastId = new java.util.concurrent.atomic.AtomicInteger(-1);

    public synchronized int getFreeId() {
        if (lastId.get() == -1) {
            lastId.set(findMaxIdInWorldGuard());
        }
        return lastId.incrementAndGet();
    }

    private int findMaxIdInWorldGuard() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) return 0;
        String nameFormat = configManager.getRegion().getString("region.name_format", "dang_{id}");
        String prefix = nameFormat.split("\\{id\\}")[0].toLowerCase();
        int max = 0;
        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = container.get(BukkitAdapter.adapt(world));
            if (manager == null) continue;
            for (String id : manager.getRegions().keySet()) {
                if (id.toLowerCase().startsWith(prefix)) {
                    try {
                        String numPart = id.substring(prefix.length()).replaceAll("[^0-9]", "");
                        if (!numPart.isEmpty()) {
                            max = Math.max(max, Integer.parseInt(numPart));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return max;
    }

    private void applyFlags(ProtectedCuboidRegion region) {
        var flagsSection = configManager.getRegion().getConfigurationSection("region.flags");
        if (flagsSection == null) return;
        for (String flagName : flagsSection.getKeys(false)) {
            String flagValue = flagsSection.getString(flagName, "").toLowerCase();
            if (flagValue.isEmpty()) continue;
            StateFlag.State state = flagValue.equals("allow") ? StateFlag.State.ALLOW : StateFlag.State.DENY;
            try {
                String normalizedFlagName = flagName.toUpperCase().replace("-", "_").replace(" ", "_");
                Field flagField = Flags.class.getField(normalizedFlagName);
                Flag<?> flag = (Flag<?>) flagField.get(null);
                if (flag instanceof StateFlag) {
                    region.setFlag((StateFlag) flag, state);
                }
            } catch (Exception ignored) {}
        }
    }

    private boolean checkDistance(Location loc, int minDist) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) return false;
        RegionManager manager = container.get(BukkitAdapter.adapt(loc.getWorld()));
        if (manager == null) return false;
        String rawFormat = configManager.getRegion().getString("region.name_format");
        String prefix = rawFormat.split("\\{")[0];
        return manager.getRegions().values().stream()
                .filter(r -> r.getId().startsWith(prefix))
                .anyMatch(r -> {
                    BlockVector3 min = r.getMinimumPoint();
                    BlockVector3 max = r.getMaximumPoint();
                    double centerX = (min.getX() + max.getX()) / 2.0;
                    double centerZ = (min.getZ() + max.getZ()) / 2.0;
                    return Math.hypot(loc.getX() - centerX, loc.getZ() - centerZ) < minDist;
                });
    }

    private boolean checkInside(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) return false;
        RegionManager manager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (manager == null) return false;
        String rawFormat = configManager.getRegion().getString("region.name_format");
        String prefix = rawFormat.split("\\{")[0];
        BlockVector3 vector = BukkitAdapter.asBlockVector(location);
        return manager.getApplicableRegions(vector).getRegions().stream()
                .anyMatch(r -> !r.getId().startsWith(prefix));
    }
}