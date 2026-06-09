package ru.truhot.rdang.util.logger;

import ru.truhot.rdang.util.logger.impl.LegacyLogger;
import org.bukkit.plugin.java.JavaPlugin;

public final class Logger {
    private Logger() {}
    private static final String INFO_PREFIX = "&7(&#FEF06ARDang&7) &aINFO &f";
    private static final String WARN_PREFIX = "&7(&#FEF06ARDang&7) &6WARN &e";
    private static final String ERROR_PREFIX = "&7(&#FEF06ARDang&7) &4ERROR &c";

    private static ILogger logger;

    public static void setup(JavaPlugin plugin) {
        logger = new LegacyLogger();
    }

    public static void warn(String message) {
        if (logger != null) logger.warn(WARN_PREFIX + message);
    }

    public static void info(String message) {
        if (logger != null) logger.info(INFO_PREFIX + message);
    }

    public static void error(String message) {
        if (logger != null) logger.error(ERROR_PREFIX + message);
    }
}