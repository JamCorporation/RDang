package ru.truhot.rdang.сore.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.UndoUtil;

public class EventManager implements Listener {
    private final Storage shulkers;
    private final ConfigManager configManager;
    private final ShulkerManager shulkerManager;
    private final ItemChecker itemChecker;
    private final Random random = new Random();
    private final UndoUtil undoUtil;

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {return;}
        ConfigurationSection locsSection = this.shulkers.getConfig().getConfigurationSection("locs");
        if (locsSection != null && event.getClickedBlock() != null) {
            if (this.shulkerManager.isShulker(event.getClickedBlock())) {
                for(String itemId : locsSection.getKeys(false)) {
                    ConfigurationSection shulker = locsSection.getConfigurationSection(itemId);
                    Location shulkerLocation = shulker.getLocation("location");
                    if (event.getClickedBlock().getLocation().getBlockX() == shulkerLocation.getBlockX() &&
                            event.getClickedBlock().getLocation().getBlockY() == shulkerLocation.getBlockY() &&
                            event.getClickedBlock().getLocation().getBlockZ() == shulkerLocation.getBlockZ() &&
                            event.getClickedBlock().getLocation().getWorld().getName().equals(shulkerLocation.getWorld().getName()) &&
                            !shulker.getBoolean("opened")) {
                        ItemStack itemInHand = event.getPlayer().getItemInHand();
                        if (itemInHand != null && itemInHand.getType() != Material.AIR && itemInHand.getItemMeta() != null && this.itemChecker.isValidKey(itemInHand)) {
                            String pTypeStr = this.configManager.getShulker().getString("particles.open.type", "TOTEM");
                            Particle pType;
                            try { pType = Particle.valueOf(pTypeStr.toUpperCase()); } catch (Exception e) { pType = Particle.TOTEM; }
                            int pCount = this.configManager.getShulker().getInt("particles.open.count", 20);
                            double pOx = this.configManager.getShulker().getDouble("particles.open.offsetX", 1.5);
                            double pOy = this.configManager.getShulker().getDouble("particles.open.offsetY", 1.5);
                            double pOz = this.configManager.getShulker().getDouble("particles.open.offsetZ", 1.5);
                            double pExtra = this.configManager.getShulker().getDouble("particles.open.extra", 0.1);
                            shulkerLocation.getWorld().spawnParticle(pType, shulkerLocation.clone().add(0.5, 1.0, 0.5), pCount, pOx, pOy, pOz, pExtra);
                            String sTypeStr = this.configManager.getShulker().getString("sounds.open.type", "UI_TOAST_CHALLENGE_COMPLETE");
                            try {
                                Sound sType = Sound.valueOf(sTypeStr.toUpperCase());
                                float sVol = (float)this.configManager.getShulker().getInt("sounds.open.volume", 50) / 100.0F;
                                float sPitch = (float)this.configManager.getShulker().getDouble("sounds.open.pitch", 1.0);
                                shulkerLocation.getWorld().playSound(shulkerLocation, sType, sVol, sPitch);
                            } catch (Exception e) {
                                shulkerLocation.getWorld().playSound(shulkerLocation, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5F, 1.0F);
                            }
                            shulker.set("opened", true);
                            this.shulkers.save();
                            this.checkAndScheduleCleanup(shulkerLocation);
                            for(Player player : Bukkit.getOnlinePlayers()) {
                                for(String s : this.configManager.getMessageManager().getFormattedOpenDungMessages(event.getPlayer().getName())) {
                                    player.sendMessage(s);
                                }
                            }
                            int saveChance = this.configManager.getItemManager().getSaveChance();
                            if (Math.random() * 100.0 < (double)saveChance) {
                                event.getPlayer().sendMessage(this.configManager.getMessageManager().getSaveKeyMessage());
                            } else {
                                if (itemInHand.getAmount() > 1) {
                                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                                } else {
                                    event.getPlayer().setItemInHand(new ItemStack(Material.AIR));
                                }
                            }
                            return;
                        }
                        event.setCancelled(true);
                        String pTypeStrL = this.configManager.getShulker().getString("particles.locked.type", "TOTEM");
                        Particle pTypeL;
                        try { pTypeL = Particle.valueOf(pTypeStrL.toUpperCase()); } catch (Exception e) { pTypeL = Particle.TOTEM; }
                        int pCountL = this.configManager.getShulker().getInt("particles.locked.count", 20);
                        double pOxL = this.configManager.getShulker().getDouble("particles.locked.offsetX", 1.5);
                        double pOyL = this.configManager.getShulker().getDouble("particles.locked.offsetY", 1.5);
                        double pOzL = this.configManager.getShulker().getDouble("particles.locked.offsetZ", 1.5);
                        double pExtraL = this.configManager.getShulker().getDouble("particles.locked.extra", 0.1);
                        shulkerLocation.getWorld().spawnParticle(pTypeL, shulkerLocation.clone().add(0.5, 1.0, 0.5), pCountL, pOxL, pOyL, pOzL, pExtraL);
                        String sTypeStrL = this.configManager.getShulker().getString("sounds.locked.type", "BLOCK_BARREL_CLOSE");
                        try {
                            Sound sTypeL = Sound.valueOf(sTypeStrL.toUpperCase());
                            float sVolL = (float)this.configManager.getShulker().getInt("sounds.locked.volume", 50) / 100.0F;
                            float sPitchL = (float)this.configManager.getShulker().getDouble("sounds.locked.pitch", 1.0);
                            event.getPlayer().playSound(event.getPlayer().getLocation(), sTypeL, sVolL, sPitchL);
                            shulkerLocation.getWorld().playSound(shulkerLocation, sTypeL, sVolL * 0.5F, sPitchL * 0.8F);
                        } catch (Exception e) {
                            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_BARREL_CLOSE, 0.5F, 1.0F);
                        }
                        for(String s : this.configManager.getMessageManager().getClosedDungMessages()) {
                            event.getPlayer().sendMessage(s);
                        }
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onLoot(LootGenerateEvent e) {
        int result = this.random.nextInt(100);
        if (result < this.configManager.getItemManager().getSpawnChance()) {
            e.getLoot().add(this.configManager.getItemManager().getKey());

            if (e.getEntity() instanceof Player player) {
                String rawMsg = this.configManager.getMessages().getString("messages.key-found");
                String finalMsg = MessageUtil.colorize(rawMsg.replace("{player}", player.getName()));
                Bukkit.broadcastMessage(finalMsg);
            }
        }

    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onPistonExtend(BlockPistonExtendEvent e) {
        this.pistonUtil(e.getBlocks(), e);
    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onPistonRetract(BlockPistonRetractEvent e) {
        this.pistonUtil(e.getBlocks(), e);
    }

    private void pistonUtil(List<Block> pushedBlocks, BlockPistonEvent e) {
        for(Block b : pushedBlocks) {
            if (this.shulkerManager.isShulker(b)) {
                e.setCancelled(true);
            }
        }

    }

    @EventHandler
    public void onPrepareCraft(org.bukkit.event.inventory.PrepareItemCraftEvent event) {
        if (event.getInventory().getMatrix() == null) return;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (this.itemChecker.isKeyItem(item) || this.itemChecker.isCompassItem(item)) {
                event.getInventory().setResult(new ItemStack(Material.AIR));
                break;
            }
        }
    }


    @EventHandler
    public void on(BlockBreakEvent e) {
        if (this.shulkerManager.isShulker(e.getBlock())) {
            ConfigurationSection locsSection = this.shulkers.getConfig().getConfigurationSection("locs");
            if (locsSection == null) {
                return;
            }

            for(String itemId : locsSection.getKeys(false)) {
                ConfigurationSection shulker = locsSection.getConfigurationSection(itemId);
                Location shulkerLocation = shulker.getLocation("location");
                if (e.getBlock().getLocation().getBlockX() == shulkerLocation.getBlockX() && e.getBlock().getLocation().getBlockY() == shulkerLocation.getBlockY() && e.getBlock().getLocation().getBlockZ() == shulkerLocation.getBlockZ() && e.getBlock().getLocation().getWorld().getName().equals(shulkerLocation.getWorld().getName())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

    }

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onKeyUsage(PlayerInteractEvent event) {
        ItemStack itemInHand = event.getItem();
        if (itemInHand != null && itemInHand.getType() != Material.AIR) {
            if (this.itemChecker.isKeyItem(itemInHand)) {
                ;
            }
        }
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )

    public void onCompassUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;
        if (!this.itemChecker.isCompassItem(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (player.hasCooldown(item.getType())) {
            String cooldownMsg = this.configManager.getMessages().getString("messages.givecompass.cooldown");
            if (cooldownMsg != null) {
                int remainingTicks = player.getCooldown(item.getType());
                long remainingSeconds = remainingTicks / 20;
                String formattedTime = ru.truhot.rdang.util.TimeUtil.format(remainingSeconds);
                player.sendMessage(MessageUtil.colorize(cooldownMsg.replace("{time}", formattedTime)));
            }
            return;
        }

        Location randomDangLocation = this.getRandomDangLocation();
        if (randomDangLocation == null) {
            String noDangsMsg = this.configManager.getMessages().getString("messages.givecompass.no_dangs");
            if (noDangsMsg != null) player.sendMessage(MessageUtil.colorize(noDangsMsg));
            return;
        }

        String showingMsg = this.configManager.getMessages().getString("messages.givecompass.showing_location");
        if (showingMsg != null) {
            showingMsg = showingMsg.replace("{x}", String.valueOf(randomDangLocation.getBlockX()))
                    .replace("{y}", String.valueOf(randomDangLocation.getBlockY()))
                    .replace("{z}", String.valueOf(randomDangLocation.getBlockZ()));
            player.sendMessage(MessageUtil.colorize(showingMsg));
        }

        Sound sound = this.configManager.getItemManager().getCompassSoundEnum();
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
        }

        long cooldownTicks = this.configManager.getItemManager().getCompassCooldown() * 20L;
        if (cooldownTicks > 0) {
            player.setCooldown(item.getType(), (int) cooldownTicks);
        }

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }
    }

    private void checkAndScheduleCleanup(Location loc) {
        if (this.configManager.getAuto().getBoolean("auto.enabled")) {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager manager = container.get(BukkitAdapter.adapt(loc.getWorld()));
            if (manager != null) {
                ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(loc));
                String prefix = this.configManager.getRegion().getString("region.name_format", "dang_").replace("{id}", "");
                set.getRegions().stream().filter((r) -> r.getId().startsWith(prefix)).findFirst().ifPresent((region) -> {
                    ConfigurationSection locs = this.shulkers.getConfig().getConfigurationSection("locs");
                    if (locs != null) {
                        boolean anyLeft = locs.getKeys(false).stream().anyMatch((key) -> {
                            ConfigurationSection s = locs.getConfigurationSection(key);
                            Location sLoc = s.getLocation("location");
                            return sLoc != null && sLoc.getWorld().getName().equals(loc.getWorld().getName()) && region.contains(BukkitAdapter.asBlockVector(sLoc)) && !s.getBoolean("opened");
                        });
                        if (!anyLeft) {
                            this.undoUtil.scheduleAutoUndoWithActionBar(region.getId(), loc.getWorld(), region);
                        }

                    }
                });
            }
        }
    }

    private Location getRandomDangLocation() {
        ConfigurationSection locsSection = this.shulkers.getConfig().getConfigurationSection("locs");
        if (locsSection != null && !locsSection.getKeys(false).isEmpty()) {
            List<String> dangIds = new ArrayList(locsSection.getKeys(false));
            if (dangIds.isEmpty()) {
                return null;
            } else {
                String randomDangId = (String)dangIds.get(this.random.nextInt(dangIds.size()));
                ConfigurationSection dangSection = locsSection.getConfigurationSection(randomDangId);
                return dangSection != null ? dangSection.getLocation("location") : null;
            }
        } else {
            return null;
        }
    }


    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (this.itemChecker.isKeyItem(item)) {
            event.setCancelled(true);
        }

    }

    public EventManager(Storage shulkers, ConfigManager configManager, ShulkerManager shulkerManager, ItemChecker itemChecker, UndoUtil undoUtil) {
        this.shulkers = shulkers;
        this.configManager = configManager;
        this.shulkerManager = shulkerManager;
        this.itemChecker = itemChecker;
        this.undoUtil = undoUtil;
    }
}
