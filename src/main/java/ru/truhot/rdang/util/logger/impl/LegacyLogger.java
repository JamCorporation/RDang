package ru.truhot.rdang.util.logger.impl;

import org.bukkit.Bukkit;
import ru.truhot.rdang.util.logger.ILogger;
import ru.truhot.rdang.util.MessageUtil;

public class LegacyLogger implements ILogger {

    @Override
    public void info(String s) {
        Bukkit.getConsoleSender().sendMessage(MessageUtil.colorize(s));
    }

    @Override
    public void warn(String s) {
        Bukkit.getConsoleSender().sendMessage(MessageUtil.colorize(s));
    }

    @Override
    public void error(String s) {
        Bukkit.getConsoleSender().sendMessage(MessageUtil.colorize(s));
    }
}