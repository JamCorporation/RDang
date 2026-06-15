package ru.truhot.rdang.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.truhot.rdang.util.logger.Logger;
import ru.truhot.rdang.сore.managers.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration items;
    private FileConfiguration world;
    private FileConfiguration messages;
    private FileConfiguration shulker;
    private FileConfiguration region;
    private FileConfiguration schem;
    private FileConfiguration config;
    private FileConfiguration auto;
    private boolean needKey = true;

    private final ItemManager itemManager;
    private final SpawnManager spawnManager;
    private final MessageManager messageManager;
    private final DangManager dangManager;
    private final WorldHeightManager worldHeightManager;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemManager = new ItemManager();
        this.spawnManager = new SpawnManager();
        this.messageManager = new MessageManager();
        this.dangManager = new DangManager();
        this.worldHeightManager = new WorldHeightManager();

        loadAllConfigs();
    }

    public void loadAllConfigs() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        config = loadConfig("config.yml", true);
        items = loadConfig("items.yml", true);
        world = loadConfig("world.yml", true);
        messages = loadConfig("messages.yml", true);
        shulker = loadConfig("shulker.yml", true);
        region = loadConfig("region.yml", true);
        schem = loadConfig("schem.yml", true);
        auto = loadConfig("auto.yml", true);
        needKey = config.getBoolean("settings.need-key", true);

        itemManager.load(items.getConfigurationSection("items"));
        spawnManager.load(world.getConfigurationSection("spawn"), this);
        messageManager.load(messages.getConfigurationSection("messages"));
        dangManager.load(world.getConfigurationSection("dang"));
        worldHeightManager.load(schem.getConfigurationSection("world-heights"));
    }

    public void reloadAll() {
        loadAllConfigs();
        Logger.info("Все конфигурации перезагружены!");
    }


    private FileConfiguration loadConfig(String fileName, boolean saveDefault) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            if (saveDefault) {
                plugin.saveResource(fileName, false);
                Logger.info(fileName + " успешно загружен");
            }
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (saveDefault) {
            mergeDefaults(cfg, file, fileName);
        }
        return cfg;
    }

    private void mergeDefaults(FileConfiguration cfg, File file, String resourceName) {
        InputStream resource = plugin.getResource(resourceName);
        if (resource == null) return;
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(resource, StandardCharsets.UTF_8));
        boolean changed = false;
        for (String key : defaults.getKeys(true)) {
            if (!cfg.contains(key)) {
                cfg.set(key, defaults.get(key));
                changed = true;
            }
        }
        if (changed) {
            try {
                cfg.save(file);
                Logger.info("[Config] Добавлены новые поля в " + resourceName);
            } catch (IOException e) {
                Logger.error("[Config] Не удалось сохранить " + resourceName + ": " + e.getMessage());
            }
        }
    }

    public boolean isNeedKey() {
        return needKey;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public FileConfiguration getItems() {
        return items;
    }

    public FileConfiguration getWorld() {
        return world;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getShulker() {
        return shulker;
    }

    public FileConfiguration getRegion() {
        return region;
    }

    public FileConfiguration getSchem() {
        return schem;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getAuto() {
        return auto;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DangManager getDangManager() {
        return dangManager;
    }

    public WorldHeightManager getHeightManager() {
        return worldHeightManager;
    }
}