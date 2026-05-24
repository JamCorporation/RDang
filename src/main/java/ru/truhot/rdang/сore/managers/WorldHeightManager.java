package ru.truhot.rdang.сore.managers;

import lombok.Getter;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.Map;

@Getter
public class WorldHeightManager {
    private final Map<String, WorldHeightConfig> worldHeights = new HashMap<>();
    private WorldHeightConfig defaultNormal;
    private WorldHeightConfig defaultNether;
    private WorldHeightConfig defaultEnd;

    public void load(ConfigurationSection section) {
        worldHeights.clear();
        if (section == null) {
            loadDefaults();
            return;
        }
        defaultNormal = loadConfig(section.getConfigurationSection("normal"), "normal");
        defaultNether = loadConfig(section.getConfigurationSection("nether"), "nether");
        defaultEnd = loadConfig(section.getConfigurationSection("end"), "the_end");
        ConfigurationSection customWorldsSection = section.getConfigurationSection("custom-worlds");
        if (customWorldsSection != null) {
            for (String worldName : customWorldsSection.getKeys(false)) {
                ConfigurationSection worldSection = customWorldsSection.getConfigurationSection(worldName);
                if (worldSection != null) {
                    WorldHeightConfig config = loadHeightConfig(worldSection);
                    config.setWorldName(worldName);
                    worldHeights.put(worldName.toLowerCase(), config);
                }
            }
        }
    }

    private void loadDefaults() {
        defaultNormal = new WorldHeightConfig("normal", defaultMin("normal"), defaultMax("normal"));
        defaultNether = new WorldHeightConfig("nether", defaultMin("nether"), defaultMax("nether"));
        defaultEnd = new WorldHeightConfig("the_end", defaultMin("the_end"), defaultMax("the_end"));
    }

    private WorldHeightConfig loadConfig(ConfigurationSection section, String type) {
        if (section != null) return loadHeightConfig(section);
        return new WorldHeightConfig(type, defaultMin(type), defaultMax(type));
    }

    private WorldHeightConfig loadHeightConfig(ConfigurationSection section) {
        String type = section.getString("type", "normal");
        int minY = section.getInt("min", defaultMin(type));
        int maxY = section.getInt("max", defaultMax(type));
        boolean useDefaultAlgorithm = section.getBoolean("use-default-algorithm", true);
        return new WorldHeightConfig(type, minY, maxY, useDefaultAlgorithm);
    }

    private int defaultMin(String type) {
        return switch (type.toLowerCase()) {
            case "nether" -> 32;
            case "the_end", "end" -> 40;
            default -> 60;
        };
    }

    private int defaultMax(String type) {
        return switch (type.toLowerCase()) {
            case "nether" -> 100;
            case "the_end", "end" -> 80;
            default -> 256;
        };
    }

    public WorldHeightConfig getWorldHeightConfig(World world) {
        String worldName = world.getName().toLowerCase();
        if (worldHeights.containsKey(worldName)) return worldHeights.get(worldName);
        return switch (world.getEnvironment()) {
            case NETHER -> defaultNether;
            case THE_END -> defaultEnd;
            default -> defaultNormal;
        };
    }

    @Getter
    public static class WorldHeightConfig {
        private final String worldType;
        private String worldName;
        private final int minY;
        private final int maxY;
        private final boolean useDefaultAlgorithm;

        public WorldHeightConfig(String worldType, int minY, int maxY) {
            this(worldType, minY, maxY, true);
        }

        public WorldHeightConfig(String worldType, int minY, int maxY, boolean useDefaultAlgorithm) {
            this.worldType = worldType;
            this.minY = minY;
            this.maxY = maxY;
            this.useDefaultAlgorithm = useDefaultAlgorithm;
        }

        public void setWorldName(String worldName) {
            this.worldName = worldName;
        }
    }
}