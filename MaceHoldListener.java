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
                    MaceType type = manager.getInventoryMaceType(p);
                    if (type == null) {
                        removePassives(p);
                        continue;
                    }
                    applyPassives(p, type);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // every 2 seconds
    }

    private void applyPassives(Player p, MaceType type) {
        int dur = 60; // 3 seconds buffer
        boolean god = type == MaceType.GOD;

        // MAGMA — fire resistance
        if (type == MaceType.MAGMA || god)
            apply(p, PotionEffectType.FIRE_RESISTANCE, dur, 0);

        // ECHO — warden immunity: strip blindness and darkness every loop
        if (type == MaceType.ECHO || god) {
            p.removePotionEffect(PotionEffectType.BLINDNESS);
            p.removePotionEffect(PotionEffectType.DARKNESS);
        }

        // SOUL — +5 hearts (amplifier 4 = 5 extra hearts on top of base 10)
        if (type == MaceType.SOUL || god)
            apply(p, PotionEffectType.HEALTH_BOOST, dur, 4); // amplifier 4 = +5 hearts

        // NATURE — regen + saturation
        if (type == MaceType.NATURE || god) {
            apply(p, PotionEffectType.REGENERATION, dur, 1);
            apply(p, PotionEffectType.SATURATION, dur, 0);
        }

        // STORM — speed II
        if (type == MaceType.STORM || god)
            apply(p, PotionEffectType.SPEED, dur, 1);

        // BLOOD — strength I baseline; strength scales with streak
        if (type == MaceType.BLOOD || god) {
            int streak = manager.getBloodStreak(p.getUniqueId());
            int amp = streak >= 3 ? 2 : streak == 2 ? 1 : 0;
            apply(p, PotionEffectType.STRENGTH, dur, amp);
        }

        // BLOOD berserk — haste IV
        if (manager.isBloodBerserkActive(p.getUniqueId()))
            apply(p, PotionEffectType.HASTE, dur, 3);

        // SPEED mace — speed III
        if (type == MaceType.SPEED || god)
            apply(p, PotionEffectType.SPEED, dur, 2);
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

    // FROST — powder snow on totem pop, bigger sphere, pushes away
    @EventHandler
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        MaceType type = manager.getInventoryMaceType(p);
        if (type != MaceType.FROST && type != MaceType.GOD) return;

        org.bukkit.Location loc = p.getLocation();
        int radius = 5;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x*x + y*y + z*z > radius * radius) continue;
                    org.bukkit.block.Block block = loc.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.POWDER_SNOW);
                        int fx = x; int fy = y; int fz = z;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            org.bukkit.block.Block b = loc.clone().add(fx, fy, fz).getBlock();
                            if (b.getType() == Material.POWDER_SNOW) b.setType(Material.AIR);
                        }, 200L); // 10 seconds
                    }
                }
            }
        }
        // Push nearby entities away
        for (org.bukkit.entity.Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof org.bukkit.entity.LivingEntity le) || e == p) continue;
            org.bukkit.util.Vector push = e.getLocation().toVector()
                .subtract(loc.toVector()).normalize().multiply(2.5);
            push.setY(0.6);
            le.setVelocity(push);
        }
    }

    @EventHandler
    public void onSlotSwitch(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (manager.getInventoryMaceType(p) == null) removePassives(p);
        }, 1L);
    }

    private void apply(Player p, PotionEffectType type, int duration, int amp) {
        p.addPotionEffect(new PotionEffect(type, duration, amp, true, false, false));
    }
}
