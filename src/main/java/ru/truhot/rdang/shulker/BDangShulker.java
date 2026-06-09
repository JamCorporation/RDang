package ru.truhot.rdang.shulker;

import org.bukkit.Location;
import ru.truhot.rdang.util.logger.Logger;
import ru.truhot.rdang.сore.MainCore;

public class BDangShulker implements ShulkerActions {

    private final MainCore bDang;

    public BDangShulker(MainCore bDang) {
        this.bDang = bDang;
    }

    @Override
    public void addShulker(Location location) {
        Logger.info("Добавление шалкера в " + location);
        bDang.addShulker(location);
    }
}
