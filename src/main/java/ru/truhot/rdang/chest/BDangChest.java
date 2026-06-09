package ru.truhot.rdang.chest;

import org.bukkit.Location;
import ru.truhot.rdang.util.logger.Logger;
import ru.truhot.rdang.сore.MainCore;

public class BDangChest implements ChestActions {

    private final MainCore bDang;

    public BDangChest(MainCore bDang) {
        this.bDang = bDang;
    }

    @Override
    public void addChest(Location location) {
        Logger.info("Добавление сундука в " + location);
        bDang.addChest(location);
    }
}
