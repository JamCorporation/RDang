package ru.truhot.rdang.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Storage {
    private File file;
    private FileConfiguration config;

    public Storage(String name, JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "data/" + name);
        try {
            if (!this.file.exists() && !this.file.createNewFile()) throw new IOException();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create file: ", e);
        }

        this.config = YamlConfiguration.loadConfiguration(this.file);
    }

    public synchronized void save() {
        try {
            this.config.save(this.file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file:", e);
        }
    }

    public void reloadConfig() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}