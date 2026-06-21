package ru.truhot.rdang.сore.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldBorder;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import ru.truhot.rdang.config.ConfigManager;
import java.util.Random;

public class SpawnManager {
    private int minX = -2000, maxX = 2000, minZ = -2000, maxZ = 2000;
    private ConfigManager configManager;

    public void load(ConfigurationSection section, ConfigManager configManager) {
        this.configManager = configManager;
        if (section == null) return;
        minX = section.getInt("minx", -2000);
        maxX = section.getInt("maxx", 2000);
        minZ = section.getInt("minz", -2000);
        maxZ = section.getInt("maxz", 2000);
    }

    private int[] getBorderClampedRange(World world) {
        WorldBorder border = world.getWorldBorder();
        double cx = border.getCenter().getX();
        double cz = border.getCenter().getZ();
        double half = border.getSize() / 2.0;
        int bMinX = (int) Math.ceil(cx - half);
        int bMaxX = (int) Math.floor(cx + half);
        int bMinZ = (int) Math.ceil(cz - half);
        int bMaxZ = (int) Math.floor(cz + half);
        return new int[]{
            Math.max(minX, bMinX),
            Math.min(maxX, bMaxX),
            Math.max(minZ, bMinZ),
            Math.min(maxZ, bMaxZ)
        };
    }

    public Location findDungLocation(World world, Random random) {
        int[] range = getBorderClampedRange(world);
        int rMinX = range[0], rMaxX = range[1], rMinZ = range[2], rMaxZ = range[3];
        if (rMinX >= rMaxX || rMinZ >= rMaxZ) return null;
        for (int attempt = 0; attempt < 100; attempt++) {
            int x = random.nextInt(rMaxX - rMinX + 1) + rMinX;
            int z = random.nextInt(rMaxZ - rMinZ + 1) + rMinZ;
            int y = getSuitableHeight(world, x, z, random);
            if (y == Integer.MIN_VALUE) continue;
            Location loc = new Location(world, x, y, z);
            if (isLocationSafe(loc) && hasSuitableBiome(loc)) {
                return loc;
            }
        }
        return null;
    }

    /**
     * Тот же подбор точки, но размазанный по тикам: одна попытка за тик. Доступ к миру
     * остаётся в главном потоке (так и должно быть), но больше не нагружает один тик
     * десятками тысяч обращений к блокам. Результат отдаётся в callback (loc или null).
     */
    public void findDungLocationSpread(World world, Random random, java.util.function.Consumer<Location> callback) {
        if (configManager == null || configManager.getPlugin() == null) {
            callback.accept(findDungLocation(world, random));
            return;
        }
        int[] range = getBorderClampedRange(world);
        int rMinX = range[0], rMaxX = range[1], rMinZ = range[2], rMaxZ = range[3];
        if (rMinX >= rMaxX || rMinZ >= rMaxZ) {
            callback.accept(null);
            return;
        }
        new org.bukkit.scheduler.BukkitRunnable() {
            int attempt = 0;
            @Override
            public void run() {
                if (attempt >= 100) {
                    cancel();
                    callback.accept(null);
                    return;
                }
                attempt++;
                int x = random.nextInt(rMaxX - rMinX + 1) + rMinX;
                int z = random.nextInt(rMaxZ - rMinZ + 1) + rMinZ;
                int y = getSuitableHeight(world, x, z, random);
                if (y == Integer.MIN_VALUE) return;
                Location loc = new Location(world, x, y, z);
                if (isLocationSafe(loc) && hasSuitableBiome(loc)) {
                    cancel();
                    callback.accept(loc);
                }
            }
        }.runTaskTimer(configManager.getPlugin(), 1L, 1L);
    }

    public Location findNearPlayerLocation(Player player, Random random) {
        World world = player.getWorld();
        WorldBorder border = world.getWorldBorder();
        double cx = border.getCenter().getX();
        double cz = border.getCenter().getZ();
        double half = border.getSize() / 2.0;
        int bMinX = (int) Math.ceil(cx - half);
        int bMaxX = (int) Math.floor(cx + half);
        int bMinZ = (int) Math.ceil(cz - half);
        int bMaxZ = (int) Math.floor(cz + half);
        int centerX = player.getLocation().getBlockX();
        int centerZ = player.getLocation().getBlockZ();
        int minR = 30, maxR = 150;
        for (int attempt = 0; attempt < 100; attempt++) {
            int angle = random.nextInt(360);
            int distance = minR + random.nextInt(maxR - minR + 1);
            int x = centerX + (int) (Math.cos(Math.toRadians(angle)) * distance);
            int z = centerZ + (int) (Math.sin(Math.toRadians(angle)) * distance);
            if (x < bMinX || x > bMaxX || z < bMinZ || z > bMaxZ) continue;
            int y = getSuitableHeight(world, x, z, random);
            if (y == Integer.MIN_VALUE) continue;
            Location loc = new Location(world, x, y, z);
            if (isLocationSafe(loc) && hasSuitableBiome(loc)) {
                return loc;
            }
        }
        return null;
    }

    public int getSuitableHeight(World world, int x, int z, Random random) {
        if (configManager == null || configManager.getHeightManager() == null)
            return world.getHighestBlockYAt(x, z);
        WorldHeightManager.WorldHeightConfig hConfig = configManager.getHeightManager().getWorldHeightConfig(world);
        if (!hConfig.isUseDefaultAlgorithm()) {
            int minY = Math.max(hConfig.getMinY(), world.getMinHeight());
            int maxY = Math.min(hConfig.getMaxY(), world.getMaxHeight());
            return minY >= maxY ? minY : random.nextInt(maxY - minY + 1) + minY;
        }
        Environment env = world.getEnvironment();
        if (env == Environment.NETHER) return getNetherHeight(world, x, z, hConfig);
        if (env == Environment.THE_END) return getEndHeight(world, x, z, hConfig);
        return getSurfaceHeight(world, x, z, hConfig);
    }

    private int getSurfaceHeight(World world, int x, int z, WorldHeightManager.WorldHeightConfig config) {
        int minY = Math.max(config.getMinY(), world.getMinHeight());
        int maxY = Math.min(config.getMaxY(), world.getMaxHeight());
        for (int y = maxY; y >= minY; y--) {
            if (isSolidGround(world.getBlockAt(x, y - 1, z).getType()) && isReplaceable(world.getBlockAt(x, y, z).getType())) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private int getNetherHeight(World world, int x, int z, WorldHeightManager.WorldHeightConfig config) {
        int minY = Math.max(config.getMinY(), world.getMinHeight());
        int maxY = Math.min(config.getMaxY(), world.getMaxHeight());
        for (int y = minY; y <= maxY; y++) {
            if (isSolidGround(world.getBlockAt(x, y - 1, z).getType()) && isReplaceable(world.getBlockAt(x, y, z).getType()) && isReplaceable(world.getBlockAt(x, y + 1, z).getType())) return y;
        }
        return (minY + maxY) / 2;
    }

    private int getEndHeight(World world, int x, int z, WorldHeightManager.WorldHeightConfig config) {
        int minY = Math.max(config.getMinY(), world.getMinHeight());
        int maxY = Math.min(config.getMaxY(), world.getMaxHeight());
        for (int y = maxY; y >= minY; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            if ((type == Material.END_STONE || type == Material.OBSIDIAN) && isReplaceable(world.getBlockAt(x, y + 1, z).getType())) return y + 1;
        }
        return (minY + maxY) / 2;
    }

    private boolean hasSuitableBiome(Location location) {
        if (configManager == null || configManager.getDangManager() == null) return true;
        var dangs = configManager.getDangManager().getDangs();
        if (dangs.isEmpty()) return true;
        Biome locationBiome = location.getWorld().getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        for (var dangData : dangs) {
            if (!dangData.getWorld().equalsIgnoreCase(location.getWorld().getName())) continue;
            for (Biome allowedBiome : dangData.getBiome()) {
                if (allowedBiome == locationBiome) return true;
            }
        }
        return false;
    }

    private boolean isSolidGround(Material m) {
        if (m.isAir() || m == Material.WATER || m == Material.LAVA || m == Material.CAVE_AIR) return false;
        String n = m.name();
        if (n.endsWith("_LEAVES") || n.contains("LOG") || n.contains("WOOD") || n.contains("FUNGUS") || n.contains("MUSHROOM") || n.contains("GRASS") || n.contains("VINE") || n.contains("CORAL")) return false;
        return m.isSolid() && m.isBlock();
    }

    private boolean isReplaceable(Material m) {
        if (m.isAir()) return true;
        String n = m.name();
        return n.endsWith("_LEAVES") || n.contains("GRASS") || n.contains("FERN") || m == Material.SNOW || !m.isSolid();
    }

    private boolean isLocationSafe(Location loc) {
        if (loc.getY() <= loc.getWorld().getMinHeight() || loc.getY() >= loc.getWorld().getMaxHeight()) return false;
        if (!isSolidGround(loc.clone().subtract(0, 1, 0).getBlock().getType())) return false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Material m = loc.clone().add(dx, dy, dz).getBlock().getType();
                    if (m == Material.LAVA || m == Material.WATER) return false;
                }
            }
        }
        return isReplaceable(loc.getBlock().getType());
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}