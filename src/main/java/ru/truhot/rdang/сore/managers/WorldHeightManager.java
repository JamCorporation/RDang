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
        defaultNormal = loadConfigOrDefault(section.getConfigurationSection("normal"), "normal");
        defaultNether = loadConfigOrDefault(section.getConfigurationSection("nether"), "nether");
        defaultEnd = loadConfigOrDefault(section.getConfigurationSection("end"), "the_end");
        ConfigurationSection customWorldsSection = section.getConfigurationSection("custom-worlds");
        if (customWorldsSection != null) {
            for (String worldName : customWorldsSection.getKeys(false)) {
                ConfigurationSection worldSection = customWorldsSection.getConfigurationSection(worldName);
                if (worldSection != null) {
                    WorldHeightConfig config = loadWorldHeightConfig(worldSection);
                    config.setWorldName(worldName);
                    worldHeights.put(worldName.toLowerCase(), config);
                }
            }
        }
    }

    private void loadDefaults() {
        defaultNormal = new WorldHeightConfig("normal", getDefaultMinForType("normal"), getDefaultMaxForType("normal"));
        defaultNether = new WorldHeightConfig("nether", getDefaultMinForType("nether"), getDefaultMaxForType("nether"));
        defaultEnd = new WorldHeightConfig("the_end", getDefaultMinForType("the_end"), getDefaultMaxForType("the_end"));
    }

    private WorldHeightConfig loadConfigOrDefault(ConfigurationSection section, String type) {
        if (section != null) return loadWorldHeightConfig(section);
        return new WorldHeightConfig(type, getDefaultMinForType(type), getDefaultMaxForType(type));
    }

    private WorldHeightConfig loadWorldHeightConfig(ConfigurationSection section) {
        String type = section.getString("type", "normal");
        int minY = section.getInt("min", getDefaultMinForType(type));
        int maxY = section.getInt("max", getDefaultMaxForType(type));
        boolean useDefaultAlgorithm = section.getBoolean("use-default-algorithm", true);
        return new WorldHeightConfig(type, minY, maxY, useDefaultAlgorithm);
    }

    private int getDefaultMinForType(String type) {
        return switch (type.toLowerCase()) {
            case "nether" -> 32;
            case "the_end", "end" -> 40;
            default -> 60;
        };
    }

    private int getDefaultMaxForType(String type) {
        return switch (type.toLowerCase()) {
            case "nether" -> 100;
            case "the_end", "end" -> 80;
            default -> 256;
        };
    }

    public WorldHeightConfig getHeightConfigForWorld(World world) {
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