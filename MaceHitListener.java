package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import com.custommaces.managers.MaceManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;

public class MaceHitListener implements Listener {

    private final CustomMaces plugin;
    private final MaceManager manager;
    private final Random random = new Random();

    public MaceHitListener(CustomMaces plugin) {
        this.plugin = plugin;
        this.manager = plugin.getMaceManager();
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if (target instanceof Player tp) {
            ItemStack off = tp.getInventory().getItemInOffHand();
            ItemStack main = tp.getInventory().getItemInMainHand();
            if (off.getType() == Material.SHIELD || main.getType() == Material.SHIELD) return;
        }

        MaceType type = manager.getHeldMaceType(attacker);
        if (type == null) return;

        int combo = manager.incrementHitCounter(attacker.getUniqueId());
        boolean thirdHit = (combo == 3);
        boolean god = (type == MaceType.GOD);

        if (type == MaceType.MAGMA  || god) hitMagma(attacker, target, event);
        if (type == MaceType.ECHO   || god) hitEcho(attacker, target);          // every hit, 4 true damage
        if (type == MaceType.SOUL   || god) { if (thirdHit) hitSoul(attacker, target); }
        if (type == MaceType.VOID   || god) hitVoid(attacker, target);
        if (type == MaceType.NATURE || god) hitNature(attacker);
        if (type == MaceType.STORM  || god) { if (thirdHit) hitStorm(attacker, target); }
        if (type == MaceType.FROST  || god) { if (thirdHit) hitFrost(target); }
        if (type == MaceType.BLOOD  || god) hitBlood(attacker, event);
        if (type == MaceType.SPEED  || god) { if (thirdHit) hitSpeed(attacker); }
    }

    // MAGMA — 1.5 hearts true damage every hit (no explosion on hit, explosion only on smash)
    private void hitMagma(Player attacker, LivingEntity target, EntityDamageByEntityEvent event) {
        // Schedule true damage after event resolves (bypasses armor)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (target.isValid()) {
                target.damage(3.0, attacker); // 1.5 hearts = 3 HP, scheduled so it's separate from swing damage
            }
        }, 1L);
        target.setFireTicks(60);
    }

    // ECHO — 4 true damage every hit (bypasses armor)
    private void hitEcho(Player attacker, LivingEntity target) {
        // Remove blindness/darkness from the Echo holder immediately (warden immunity)
        attacker.removePotionEffect(PotionEffectType.BLINDNESS);
        attacker.removePotionEffect(PotionEffectType.DARKNESS);

        // True damage: schedule after this event so it's separate from normal damage
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (target.isValid()) {
                target.damage(8.0, attacker); // 4 hearts = 8 HP true damage
            }
        }, 1L);

        // Brief darkness on target
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));
        target.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
    }

    // SOUL — every 3 hits: give target 3 hearts
    private void hitSoul(Player attacker, LivingEntity target) {
        if (target instanceof Player tp) {
            double newHealth = Math.min(tp.getHealth() + 6.0, tp.getMaxHealth());
            tp.setHealth(newHealth);
            attacker.sendActionBar("§5Soul: §7Opponent healed 3 hearts!");
        }
        // Spawn mob on every 3rd hit
        EntityType[] mobs = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                             EntityType.SPIDER, EntityType.WITCH};
        EntityType mob = mobs[(int)(Math.random() * mobs.length)];
        target.getWorld().spawnEntity(target.getLocation(), mob);
        target.getWorld().spawnParticle(Particle.SOUL, target.getLocation(), 10, 0.5, 0.5, 0.5, 0.05);
    }

    // VOID — dragon breath pools on hit
    private void hitVoid(Player attacker, LivingEntity target) {
        boolean inEnd = attacker.getWorld().getEnvironment() == World.Environment.THE_END;
        if (inEnd) {
            Location center = attacker.getLocation();
            for (Entity e : attacker.getNearbyEntities(10, 10, 10)) {
                if (e instanceof Player nearby && e != attacker) {
                    Vector pull = center.toVector().subtract(e.getLocation().toVector()).normalize().multiply(1.5);
                    pull.setY(0.2);
                    nearby.setVelocity(pull);
                }
            }
        }
        target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 20, 0.5, 0.5, 0.5, 0.2);
    }

    // NATURE — absorption on every hit
    private void hitNature(Player attacker) {
        int currentAmp = attacker.getPotionEffect(PotionEffectType.ABSORPTION) != null
            ? attacker.getPotionEffect(PotionEffectType.ABSORPTION).getAmplifier() : -1;
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200,
            Math.min(currentAmp + 1, 9), false, false, false));
    }

    // STORM — lightning every 3 hits (25s CD on ability, not on hit proc)
    private void hitStorm(Player attacker, LivingEntity target) {
        target.getWorld().strikeLightning(target.getLocation());
        attacker.sendActionBar("§e§lSTORM STRIKE!");
    }

    // FROST — slowness every 3 hits
    private void hitFrost(LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
    }

    // BLOOD — always autocrit (1.5x), berserk adds another 2x
    private void hitBlood(Player attacker, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 1.5);
        if (manager.isBloodBerserkActive(attacker.getUniqueId())) {
            event.setDamage(event.getDamage() * 2.0);
        }
        int combo = manager.getHitCounter(attacker.getUniqueId());
        if (combo == 3 && manager.isBloodBerserkReady(attacker.getUniqueId())) {
            manager.activateBloodBerserk(attacker.getUniqueId());
            attacker.sendActionBar("§4§l⚔ BERSERK ACTIVATED ⚔");
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (attacker.isOnline()) attacker.sendActionBar("§7Berserk ended.");
            }, 200L);
        }
    }

    // SPEED — Speed V for 5s every 3 hits
    private void hitSpeed(Player attacker) {
        if (!manager.isSpeedHitReady(attacker.getUniqueId())) return;
        manager.setSpeedHitCD(attacker.getUniqueId());
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 4));
        attacker.sendActionBar("§b§lSPEED SURGE!");
    }
}
