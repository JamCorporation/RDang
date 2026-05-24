package ru.truhot.rdang.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.truhot.rdang.util.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MigrateConfig {

    private static final String[] CONFIG_FILES = {
            "config.yml",
            "items.yml",
            "world.yml",
            "messages.yml",
            "shulker.yml",
            "region.yml",
            "schem.yml",
            "auto.yml"
    };

    public MigrateResult migrate(JavaPlugin plugin) {
        MigrateResult result = new MigrateResult();
        File dataFolder = plugin.getDataFolder();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        for (String fileName : CONFIG_FILES) {
            FileConfiguration defaults = loadDefault(plugin, fileName);
            if (defaults == null) {
                Logger.warn("Дефолтный " + fileName + " не найден в JAR, пропуск.");
                continue;
            }

            File file = new File(dataFolder, fileName);
            FileConfiguration current = file.exists()
                    ? YamlConfiguration.loadConfiguration(file)
                    : new YamlConfiguration();

            FileConfiguration merged = mergeInDefaultOrder(defaults, current);

            if (configsEqual(current, merged)) {
                result.unchanged.add(fileName);
                continue;
            }

            try {
                merged.save(file);
                result.updated.add(fileName);
            } catch (IOException e) {
                Logger.warn("Ошибка миграции " + fileName + ": " + e.getMessage());
                result.failed.add(fileName);
            }
        }

        return result;
    }

    private FileConfiguration loadDefault(JavaPlugin plugin, String fileName) {
        try (InputStream stream = plugin.getResource(fileName)) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return null;
        }
    }

    private FileConfiguration mergeInDefaultOrder(FileConfiguration defaults, FileConfiguration user) {
        YamlConfiguration merged = new YamlConfiguration();
        applySection(defaults, user, merged, "");
        return merged;
    }

    private void applySection(ConfigurationSection defaults, FileConfiguration user,
                                YamlConfiguration merged, String prefix) {
        for (String key : defaults.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;

            if (defaults.isConfigurationSection(key)) {
                ConfigurationSection child = defaults.getConfigurationSection(key);
                if (child != null) {
                    applySection(child, user, merged, path);
                }
            } else {
                Object value = user.contains(path) ? user.get(path) : defaults.get(key);
                merged.set(path, value);
            }
        }
    }

    private boolean configsEqual(FileConfiguration a, FileConfiguration b) {
        Set<String> pathsA = leafPaths(a);
        Set<String> pathsB = leafPaths(b);
        if (!pathsA.equals(pathsB)) {
            return false;
        }
        for (String path : pathsA) {
            if (!valuesEqual(a.get(path), b.get(path))) {
                return false;
            }
        }
        return true;
    }

    private Set<String> leafPaths(FileConfiguration config) {
        Set<String> paths = new LinkedHashSet<>();
        for (String path : config.getKeys(true)) {
            if (!config.isConfigurationSection(path)) {
                paths.add(path);
            }
        }
        return paths;
    }

    private boolean valuesEqual(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof List && b instanceof List) {
            List<?> listA = (List<?>) a;
            List<?> listB = (List<?>) b;
            if (listA.size() != listB.size()) {
                return false;
            }
            for (int i = 0; i < listA.size(); i++) {
                if (!Objects.equals(listA.get(i), listB.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return Objects.equals(a, b);
    }

    public static class MigrateResult {
        private final List<String> updated = new ArrayList<>();
        private final List<String> unchanged = new ArrayList<>();
        private final List<String> failed = new ArrayList<>();

        public boolean isAllUpToDate() {
            return updated.isEmpty() && failed.isEmpty();
        }

        public List<String> getUpdated() {
            return updated;
        }

        public List<String> getFailed() {
            return failed;
        }
    }
}
