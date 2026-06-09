package ru.truhot.rdang;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.truhot.rdang.addshulkers.AddShulkers;
import ru.truhot.rdang.comands.Command;
import ru.truhot.rdang.comands.RTabCompleter;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.dung.DungActions;
import ru.truhot.rdang.menu.MenuManager;
import ru.truhot.rdang.schem.SchemAction;
import ru.truhot.rdang.shulker.BDangShulker;
import ru.truhot.rdang.shulker.ShulkerActions;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.CoreProtectManager;
import ru.truhot.rdang.util.Metrics;
import ru.truhot.rdang.util.UndoUtil;
import ru.truhot.rdang.util.UpdateUtil;
import ru.truhot.rdang.util.logger.Logger;
import ru.truhot.rdang.сore.MainCore;
import ru.truhot.rdang.сore.CoreFactory;

import java.io.File;

public final class RDang extends JavaPlugin {
    private UndoUtil undoUtil;

    @Override
    public void onEnable() {
        Logger.setup(this);
        new File(getDataFolder(), "schem").mkdirs();
        new File(getDataFolder(), "data").mkdirs();
        new File(getDataFolder(), "backups").mkdirs();
        ConfigManager configManager = new ConfigManager(this);
        Storage shulkers = new Storage("shulkers.yml", this);
        Storage items = new Storage("items.yml", this);
        Storage blockStorage = new Storage("block.yml", this);
        SchemAction schemAction = new SchemAction(this, configManager);
        this.undoUtil = new UndoUtil(configManager, shulkers, blockStorage, this, schemAction);
        MainCore mainCore = CoreFactory.createDang(items, shulkers, configManager, undoUtil);
        ShulkerActions shulkerActions = new BDangShulker(mainCore);
        AddShulkers addShulkers = new AddShulkers(this, shulkerActions);
        CoreProtectManager coreProtectManager = new CoreProtectManager();
        coreProtectManager.init();
        DungActions dungActions = new DungActions(schemAction, addShulkers, configManager, undoUtil, coreProtectManager);
        MenuManager menuManager = new MenuManager(configManager, items, shulkers, blockStorage, this, mainCore.getLootManager());
        UpdateUtil updateUtil = new UpdateUtil(this);
        if (getConfig().getBoolean("settings.update-check")) {updateUtil.check();}
        Command command = new Command(mainCore, dungActions, this, items, shulkers, blockStorage, configManager, menuManager, undoUtil, mainCore.getShulkerManager(), updateUtil);
        getServer().getPluginManager().registerEvents(menuManager, this);
        getCommand("rdang").setExecutor(command);
        getCommand("rdang").setTabCompleter(new RTabCompleter(this));
        getServer().getPluginManager().registerEvents(mainCore, this);
        getServer().getPluginManager().registerEvents(mainCore.getEventHandler(), this);
        Logger.info("успешно запущен!");
        if (getConfig().getBoolean("settings.metrics")) {
            int pluginId = 28720;
            new Metrics(this, pluginId);
            Logger.info("bStats успешно инициализирован!");
        }
    }

    @Override
    public void onDisable() {
        Logger.info("отключен!");
        if (undoUtil != null) undoUtil.shutdown();
        Bukkit.getScheduler().cancelTasks(this);
    }
}