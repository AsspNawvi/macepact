package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import com.custommaces.managers.MaceManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class MaceHoldListener implements Listener {

    private final CustomMaces plugin;
    private final MaceManager manager;

    public MaceHoldListener(CustomMaces plugin) {
        this.plugin = plugin;
        this.manager = plugin.getMaceManager();
        startPassiveLoop();
    }

    private void startPassiveLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    // Check inventory (any slot) instead of only held item
                    MaceType type = manager.getInventoryMaceType(p);
                    if (type == null) {
                        // Remove passives if no mace in inventory
                        removePassives(p);
                        continue;
                    }
                    applyPassives(p, type);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void applyPassives(Player p, MaceType type) {
        int dur = 60;
        boolean god = type == MaceType.GOD;

        if (type == MaceType.MAGMA || god)
            apply(p, PotionEffectType.FIRE_RESISTANCE, dur, 0);

        if (type == MaceType.ECHO || god) {
            p.removePotionEffect(PotionEffectType.BLINDNESS);
            p.removePotionEffect(PotionEffectType.DARKNESS);
        }

        if (type == MaceType.SOUL || god)
            apply(p, PotionEffectType.HEALTH_BOOST, dur, 9); // +10 hearts = amplifier 9

        if (type == MaceType.VOID || god) {
            if (p.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END)
                apply(p, PotionEffectType.HEALTH_BOOST, dur, 19); // +20 hearts in End (on top of base)
        }

        if (type == MaceType.NATURE || god) {
            apply(p, PotionEffectType.REGENERATION, dur, 1);
            apply(p, PotionEffectType.SATURATION, dur, 0);
        }

        if (type == MaceType.STORM || god)
            apply(p, PotionEffectType.SPEED, dur, 1); // Speed II

        if (type == MaceType.BLOOD || god)
            apply(p, PotionEffectType.STRENGTH, dur, 0); // Strength I base

        if (type == MaceType.SPEED || god)
            apply(p, PotionEffectType.SPEED, dur, 2); // Speed III

        // Blood streak bonus
        if (type == MaceType.BLOOD || god) {
            int streak = manager.getBloodStreak(p.getUniqueId());
            if (streak >= 3)      apply(p, PotionEffectType.STRENGTH, dur, 2);
            else if (streak == 2) apply(p, PotionEffectType.STRENGTH, dur, 1);
        }

        // Berserk haste
        if (manager.isBloodBerserkActive(p.getUniqueId()))
            apply(p, PotionEffectType.HASTE, dur, 3);
    }

    private void removePassives(Player p) {
        p.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        p.removePotionEffect(PotionEffectType.HEALTH_BOOST);
        p.removePotionEffect(PotionEffectType.REGENERATION);
        p.removePotionEffect(PotionEffectType.SATURATION);
        p.removePotionEffect(PotionEffectType.SPEED);
        p.removePotionEffect(PotionEffectType.STRENGTH);
        p.removePotionEffect(PotionEffectType.HASTE);
    }

    // Frost Mace: powdered snow 3x3x3 on totem pop
    @EventHandler
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        MaceType type = manager.getInventoryMaceType(p);
        if (type != MaceType.FROST && type != MaceType.GOD) return;

        org.bukkit.Location loc = p.getLocation();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    org.bukkit.block.Block block = loc.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.POWDER_SNOW);
                        int fx = x; int fy = y; int fz = z;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            org.bukkit.block.Block b = loc.clone().add(fx, fy, fz).getBlock();
                            if (b.getType() == Material.POWDER_SNOW) b.setType(Material.AIR);
                        }, 60L);
                    }
                }
            }
        }
    }

    private void apply(Player p, PotionEffectType type, int duration, int amp) {
        if (duration == 0) return;
        p.addPotionEffect(new PotionEffect(type, duration, amp, true, false, false));
    }

    // Remove passives when switching away from mace slot (if no mace elsewhere in inventory)
    @EventHandler
    public void onSlotSwitch(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        // Schedule check after slot switch resolves
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (manager.getInventoryMaceType(p) == null) removePassives(p);
        }, 1L);
    }
}
