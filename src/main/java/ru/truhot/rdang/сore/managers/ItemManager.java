package ru.truhot.rdang.сore.managers;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.truhot.rdang.util.HeadUtil;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.TimeUtil;
import ru.truhot.rdang.util.logger.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class ItemManager {

    private ItemStack key;
    private ItemStack compass;
    private int spawnChance;
    private int saveChance;
    private boolean hideEnchantments;
    private long compassCooldown;
    private String compassSound;
    private boolean keyGlow = true;
    private boolean keyDrop = true;
    private boolean compassGlow = true;
    private boolean compassDrop = true;

    public void load(ConfigurationSection section) {
        if (section == null) return;

        ConfigurationSection keySection = section.getConfigurationSection("key");
        if (keySection != null) {
            this.key = loadItem(keySection, "key");
            if (!keySection.contains("chanceSpawn")) Logger.warn("нету chanceSpawn в секции key");
            this.spawnChance = keySection.getInt("chanceSpawn");
            if (!keySection.contains("saveChance")) Logger.warn("нету saveChance в секции key");
            this.saveChance = keySection.getInt("saveChance");
            this.keyGlow = keySection.getBoolean("glow", true);
            this.keyDrop = keySection.getBoolean("drop", true);
        }

        ConfigurationSection compassSection = section.getConfigurationSection("compass");
        if (compassSection != null) {
            this.compass = loadItem(compassSection, "compass");
            this.compassSound = compassSection.getString("sounds");
            if (this.compassSound == null) {
                Logger.warn("нету sounds в секции compass");
            } else {
                try {
                    Sound.valueOf(this.compassSound.toUpperCase());
                } catch (IllegalArgumentException e) {
                    Logger.error("Неверный звук '" + this.compassSound + "' в секции compass");
                }
            }

            String cooldownStr = compassSection.getString("сooldown");
            if (cooldownStr == null) {
                Logger.warn("нету сooldown в секции compass");
            } else {
                this.compassCooldown = TimeUtil.parse(cooldownStr);
            }
            this.compassGlow = compassSection.getBoolean("glow", true);
            this.compassDrop = compassSection.getBoolean("drop", true);
        }
    }

    public boolean isKeyGlow() {
        return keyGlow;
    }

    public boolean isKeyDrop() {
        return keyDrop;
    }

    public boolean isCompassGlow() {
        return compassGlow;
    }

    public boolean isCompassDrop() {
        return compassDrop;
    }

    public Sound getCompassSound() {
        if (this.compassSound == null) return null;
        try {
            return Sound.valueOf(this.compassSound.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack loadItem(ConfigurationSection section, String type) {
        String materialName = section.getString("material");
        ItemStack item = null;
        if (materialName == null) {
            Logger.error("нету material в секции " + section.getName());
        } else if (HeadUtil.isBase64Head(materialName)) {
            item = HeadUtil.createSkull(materialName, section.getName());
        } else {
            Material material = materialFrom(materialName);
            if (material == null) {
                Logger.error("Неверный material '" + materialName + "' в секции " + section.getName());
            } else {
                item = new ItemStack(material);
            }
        }

        ItemMeta meta = (item != null) ? item.getItemMeta() : null;
        applyItemMeta(meta, section);
        applyNBT(meta, type);
        applyEnchants(meta, section);
        if (item != null && meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }

    private void applyNBT(ItemMeta meta, String type) {
        if (meta == null) return;
        if (type.equals("key")) {
            meta.getPersistentDataContainer().set(new NamespacedKey("rdang", "key"), PersistentDataType.STRING, "holyworld");
        } else if (type.equals("compass")) {
            meta.getPersistentDataContainer().set(new NamespacedKey("rdang", "compass"), PersistentDataType.STRING, "true");
        }
    }

    private void applyEnchants(ItemMeta meta, ConfigurationSection section) {
        if (!section.contains("hideEnchantments")) {
            Logger.warn("нету hideEnchantments в секции " + section.getName());
        }
        if (meta == null) return;
        if (!section.getBoolean("hideEnchantments")) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        } else {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
    }

    private void applyItemMeta(ItemMeta meta, ConfigurationSection section) {
        String name = section.getString("name");
        if (name == null) {
            Logger.warn("нету name в секции " + section.getName());
        } else if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
        }

        List<String> lore = section.getStringList("lore");
        if (lore.isEmpty()) {
            Logger.warn("нету lore в секции " + section.getName());
        } else if (meta != null) {
            meta.setLore(lore.stream()
                    .map(MessageUtil::colorize)
                    .collect(Collectors.toList()));
            if (section.contains("custom_model_data")) {
                int modelData = section.getInt("custom_model_data");
                meta.setCustomModelData(modelData);
            }
        }
    }

    private Material materialFrom(String materialName) {
        if (materialName == null || materialName.isEmpty()) return null;
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material != null) return material;
        return Material.getMaterial(materialName.toUpperCase().replace(' ', '_'));
    }

    public ItemStack getKey() {
        return key;
    }

    public ItemStack getCompass() {
        return compass;
    }

    public int getSpawnChance() {
        return spawnChance;
    }

    public int getSaveChance() {
        return saveChance;
    }

    public boolean isHideEnchantments() {
        return hideEnchantments;
    }

    public long getCompassCooldown() {
        return compassCooldown;
    }

}