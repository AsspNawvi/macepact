package com.customswords.listeners;

import com.customswords.CustomSwords;
import com.customswords.SwordType;
import com.customswords.managers.SwordManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SwordHoldListener implements Listener {

    private final CustomSwords plugin;
    private final SwordManager manager;

    public SwordHoldListener(CustomSwords plugin) {
        this.plugin = plugin;
        this.manager = plugin.getSwordManager();
        startPassiveLoop();
    }

    private void startPassiveLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    SwordType type = manager.getInventorySwordType(p);
                    if (type == null) { removePassives(p); continue; }
                    applyPassives(p, type);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void applyPassives(Player p, SwordType type) {
        int dur = 60;
        boolean god = type == SwordType.GOD;

        if (type == SwordType.MAGMA || god)
            apply(p, PotionEffectType.FIRE_RESISTANCE, dur, 0);

        if (type == SwordType.ECHO || god) {
            p.removePotionEffect(PotionEffectType.BLINDNESS);
            p.removePotionEffect(PotionEffectType.DARKNESS);
        }

        if (type == SwordType.SOUL || god)
            apply(p, PotionEffectType.HEALTH_BOOST, dur, 4); // +5 hearts

        if (type == SwordType.NATURE || god) {
            apply(p, PotionEffectType.REGENERATION, dur, 1);
            apply(p, PotionEffectType.SATURATION, dur, 0);
        }

        if (type == SwordType.STORM || god)
            apply(p, PotionEffectType.SPEED, dur, 1); // Speed II

        if (type == SwordType.BLOOD || god) {
            int streak = manager.getBloodStreak(p.getUniqueId());
            int amp = streak >= 3 ? 2 : streak == 2 ? 1 : 0;
            apply(p, PotionEffectType.STRENGTH, dur, amp);
        }

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

    // FROST — powder snow sphere on totem pop (radius 5, 10s, pushes away)
    @EventHandler
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        SwordType type = manager.getInventorySwordType(p);
        if (type != SwordType.FROST && type != SwordType.GOD) return;

        org.bukkit.Location loc = p.getLocation();
        int radius = 5;
        for (int x = -radius; x <= radius; x++)
            for (int y = -radius; y <= radius; y++)
                for (int z = -radius; z <= radius; z++) {
                    if (x*x + y*y + z*z > radius * radius) continue;
                    org.bukkit.block.Block b = loc.clone().add(x, y, z).getBlock();
                    if (b.getType() == Material.AIR) {
                        b.setType(Material.POWDER_SNOW);
                        int fx = x, fy = y, fz = z;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            org.bukkit.block.Block bl = loc.clone().add(fx, fy, fz).getBlock();
                            if (bl.getType() == Material.POWDER_SNOW) bl.setType(Material.AIR);
                        }, 200L);
                    }
                }

        for (org.bukkit.entity.Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof org.bukkit.entity.LivingEntity le) || e == p) continue;
            org.bukkit.util.Vector push = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.5);
            push.setY(0.6);
            le.setVelocity(push);
        }
    }

    @EventHandler
    public void onSlotSwitch(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (manager.getInventorySwordType(p) == null) removePassives(p);
        }, 1L);
    }

    private void apply(Player p, PotionEffectType type, int dur, int amp) {
        p.addPotionEffect(new PotionEffect(type, dur, amp, true, false, false));
    }
}
