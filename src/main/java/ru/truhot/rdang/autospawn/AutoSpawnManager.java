package ru.truhot.rdang.autospawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.dung.DungActions;
import ru.truhot.rdang.util.TimeUtil;
import ru.truhot.rdang.util.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class AutoSpawnManager {

    private final RDang plugin;
    private final ConfigManager configManager;
    private final DungActions dungActions;
    private final Random random = new Random();

    private BukkitTask task = null;
    private long nextSpawnTime = -1;

    public AutoSpawnManager(RDang plugin, ConfigManager configManager, DungActions dungActions) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dungActions = dungActions;
    }

    public void start() {
        FileConfiguration auto = configManager.getAuto();
        if (!auto.getBoolean("auto.spawn.enabled", false)) return;
        if (task != null) return;

        long intervalSeconds = TimeUtil.parse(auto.getString("auto.spawn.interval", "1h"));
        long intervalTicks = intervalSeconds * 20L;
        nextSpawnTime = System.currentTimeMillis() + intervalSeconds * 1000L;

        task = new BukkitRunnable() {
            @Override
            public void run() {
                nextSpawnTime = System.currentTimeMillis() + intervalSeconds * 1000L;
                doSpawn();
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);

        Logger.info("Авто-спавн данжей запущен, интервал: " + auto.getString("auto.spawn.interval", "1h"));
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
            nextSpawnTime = -1;
            Logger.info("Авто-спавн данжей остановлен.");
        }
    }

    public boolean isRunning() {
        return task != null;
    }

    public long getRemainingSeconds() {
        if (nextSpawnTime < 0) return -1;
        long remaining = (nextSpawnTime - System.currentTimeMillis()) / 1000L;
        return Math.max(0, remaining);
    }

    public void restart() {
        stop();
        start();
    }

    private void doSpawn() {
        FileConfiguration auto = configManager.getAuto();
        List<String> worldNames = auto.getStringList("auto.spawn.worlds");

        if (worldNames.isEmpty()) {
            for (World w : Bukkit.getWorlds()) {
                if (!configManager.getDangManager().getWorldDangs(w.getName()).isEmpty()) {
                    worldNames.add(w.getName());
                }
            }
        }

        for (String worldName : worldNames) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                Logger.warn("Авто-спавн: мир не найден — " + worldName);
                continue;
            }
            if (configManager.getDangManager().getWorldDangs(worldName).isEmpty()) continue;

            final String fWorldName = worldName;
            // Подбор точки размазан по тикам, чтобы не нагружать один тик сканированием мира.
            configManager.getSpawnManager().findDungLocationSpread(world, random, loc -> {
                if (loc != null) {
                    dungActions.spawn(loc);
                } else {
                    Logger.warn("Авто-спавн: не удалось найти точку спавна в мире " + fWorldName);
                }
            });
        }
    }

    public void setEnabled(boolean enabled) {
        FileConfiguration auto = configManager.getAuto();
        auto.set("auto.spawn.enabled", enabled);
        saveAutoConfig();
        if (enabled) {
            start();
        } else {
            stop();
        }
    }

    public void setInterval(String timeStr) {
        FileConfiguration auto = configManager.getAuto();
        auto.set("auto.spawn.interval", timeStr);
        saveAutoConfig();
        if (isRunning()) restart();
    }

    private void saveAutoConfig() {
        File file = new File(plugin.getDataFolder(), "auto.yml");
        try {
            configManager.getAuto().save(file);
        } catch (IOException e) {
            Logger.error("Не удалось сохранить auto.yml: " + e.getMessage());
        }
    }
}
