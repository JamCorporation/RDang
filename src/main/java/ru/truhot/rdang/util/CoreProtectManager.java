package ru.truhot.rdang.util;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import ru.truhot.rdang.util.logger.Logger;

public class CoreProtectManager {
    private CoreProtectAPI api;

    public void init() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("CoreProtect");
        if (plugin instanceof CoreProtect) {
            this.api = ((CoreProtect) plugin).getAPI();
            if (api != null && api.isEnabled()) {
                Logger.info("CoreProtect интегрирован — сканирование построек активно.");
            } else {
                Logger.warn("CoreProtect найден, но API недоступен или отключен.");
                this.api = null;
            }
        } else {
            Logger.warn("CoreProtect не найден — сканирование построек отключено.");
        }
    }

    public boolean isAvailable() {
        return api != null;
    }

    /**
     * Собирает блоки поверхности для проверки. ОБЯЗАТЕЛЬНО вызывать в главном потоке —
     * здесь идёт доступ к миру (getHighestBlockYAt/getBlockAt/getType).
     * Сканирует поверхность с шагом для оптимизации.
     */
    public java.util.List<Block> collectSurfaceBlocks(Location center, int radiusX, int radiusZ) {
        java.util.List<Block> blocks = new java.util.ArrayList<>();
        if (api == null) return blocks;
        World world = center.getWorld();
        if (world == null) return blocks;

        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        int step = 5;
        int maxChecks = 64;
        int checks = 0;

        for (int x = centerX - radiusX; x <= centerX + radiusX; x += step) {
            for (int z = centerZ - radiusZ; z <= centerZ + radiusZ; z += step) {
                if (checks >= maxChecks) break;
                int y = world.getHighestBlockYAt(x, z);
                Block block = world.getBlockAt(x, y, z);
                if (block.getType().isAir()) continue;
                blocks.add(block);
                checks++;
            }
            if (checks >= maxChecks) break;
        }
        return blocks;
    }

    /**
     * Проверяет, есть ли среди собранных блоков постройки игроков.
     * Безопасно вызывать асинхронно — здесь только обращения к БД CoreProtect
     * (api.blockLookup), без доступа к живому миру.
     */
    public boolean hasPlayerBuilds(java.util.List<Block> blocks) {
        if (api == null || blocks == null) return false;
        for (Block block : blocks) {
            if (isPlayerPlaced(block)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPlayerPlaced(Block block) {
        try {
            for (String[] result : api.blockLookup(block, 0)) {
                CoreProtectAPI.ParseResult parse = api.parseResult(result);
                int action = parse.getActionId();
                if (action == 1) { // 1 = block placed
                    return true;
                }
            }
        } catch (Exception e) {
            Logger.warn("Ошибка CoreProtect при проверке " + block.getLocation() + ": " + e.getMessage());
        }
        return false;
    }
}
