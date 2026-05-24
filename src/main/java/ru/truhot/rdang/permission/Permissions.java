package ru.truhot.rdang.permission;

import org.bukkit.permissions.Permissible;

public final class Permissions {

    public static final String ADMIN = "rdang.admin";
    public static final String USE = "rdang.use";

    public static final String ADD_ITEM = "rdang.additem";
    public static final String SPAWN = "rdang.spawn";
    public static final String SCHEM = "rdang.schem";
    public static final String GIVE_KEY = "rdang.give.key";
    public static final String GIVE_COMPASS = "rdang.give.compass";
    public static final String RELOAD = "rdang.reload";
    public static final String UNDO = "rdang.undo";
    public static final String LIST = "rdang.list";
    public static final String MENU = "rdang.menu";
    public static final String ADMINS = "rdang.admins";
    public static final String UPDATE = "rdang.update";
    public static final String MIGRATE = "rdang.migrate";

    private Permissions() {}

    public static boolean has(Permissible permissible, String permission) {
        return permissible.hasPermission(ADMIN) || permissible.hasPermission(permission);
    }
}
