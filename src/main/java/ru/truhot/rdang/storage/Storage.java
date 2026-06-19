package ru.truhot.rdang.storage;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.truhot.rdang.util.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Storage {
    private final File file;
    private FileConfiguration config;
    // Отдельный лок только на запись в файл, чтобы синхронные и асинхронные
    // сохранения не перетирали друг друга на диске.
    private final Object ioLock = new Object();

    public Storage(String name, JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "data/" + name);
        try {
            if (!this.file.exists() && !this.file.createNewFile()) throw new IOException();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create file: ", e);
        }

        this.config = YamlConfiguration.loadConfiguration(this.file);
    }

    /**
     * Синхронное сохранение. Снимок конфига делается на текущем потоке (где и
     * происходят все мутации config.set(...)), поэтому гонки с этими мутациями нет.
     * Запись на диск выполняется под ioLock.
     */
    public void save() {
        writeToDisk(snapshot());
    }

    /**
     * Асинхронное сохранение без гонки данных: снимок (saveToString) снимается
     * СЕЙЧАС, на вызывающем (главном) потоке, пока никто не мутирует config,
     * а уже готовая строка пишется на диск в асинхронном потоке.
     */
    public void saveAsync(Plugin plugin) {
        final String data = snapshot();
        if (plugin == null) {
            writeToDisk(data);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeToDisk(data));
    }

    private synchronized String snapshot() {
        return config.saveToString();
    }

    private void writeToDisk(String data) {
        synchronized (ioLock) {
            try {
                Files.write(file.toPath(), data.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                Logger.error("Не удалось сохранить файл " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public synchronized void reloadConfig() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
