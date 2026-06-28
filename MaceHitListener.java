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

import java.util.List;
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

        // Shield check
        if (target instanceof Player targetPlayer) {
            ItemStack offhand = targetPlayer.getInventory().getItemInOffHand();
            ItemStack mainhand = targetPlayer.getInventory().getItemInMainHand();
            if (offhand.getType() == Material.SHIELD || mainhand.getType() == Material.SHIELD) return;
        }

        MaceType type = manager.getHeldMaceType(attacker);
        if (type == null) return;

        int combo = manager.incrementHitCounter(attacker.getUniqueId());
        boolean thirdHit = (combo == 3);
        boolean god = (type == MaceType.GOD);

        if (type == MaceType.MAGMA  || god) hitMagma(attacker, target);
        // Echo: blindness on every 3 hits (not every hit)
        if (type == MaceType.ECHO   || god) { if (thirdHit) hitEcho(attacker, target); }
        // Soul: spawn hostile on every hit; give target 3 hearts every 3 hits
        if (type == MaceType.SOUL   || god) hitSoul(attacker, target, thirdHit);
        // Void: vortex only in the End
        if (type == MaceType.VOID   || god) hitVoid(attacker, target);
        if (type == MaceType.NATURE || god) hitNature(attacker, target);
        // Storm: lightning every 3 hits (fixed — was triggering every hit)
        if (type == MaceType.STORM  || god) { if (thirdHit) hitStorm(attacker, target); }
        if (type == MaceType.FROST  || god) { if (thirdHit) hitFrost(target); }
        // Blood: always autocrit, regardless of berserk state
        if (type == MaceType.BLOOD  || god) hitBlood(attacker, event);
        if (type == MaceType.SPEED  || god) { if (thirdHit) hitSpeed(attacker); }
    }

    // MAGMA — explosion on every hit
    private void hitMagma(Player attacker, LivingEntity target) {
        target.getWorld().createExplosion(target.getLocation(), 1.5f, true, false, attacker);
    }

    // ECHO — blindness every 3 hits (NOT every hit; Echo passive already strips warden blindness)
    private void hitEcho(Player attacker, LivingEntity target) {
        // Don't apply blindness to the Echo/God mace holder
        if (target instanceof Player tp) {
            MaceType targetType = manager.getInventoryMaceType(tp);
            if (targetType == MaceType.ECHO || targetType == MaceType.GOD) return;
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
        attacker.sendActionBar("§8Echo: Blindness applied!");
    }

    // SOUL — spawn hostile mob every hit; every 3 hits give target 3 hearts instead of removing
    private void hitSoul(Player attacker, LivingEntity target, boolean thirdHit) {
        // Spawn hostile mob on every hit
        EntityType[] mobs = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                             EntityType.SPIDER, EntityType.WITCH, EntityType.BLAZE,
                             EntityType.CAVE_SPIDER, EntityType.ENDERMAN};
        EntityType mob = mobs[(int)(Math.random() * mobs.length)];
        Entity spawned = target.getWorld().spawnEntity(target.getLocation(), mob);
        if (spawned instanceof Mob mobEntity) mobEntity.setTarget(target instanceof Player ? target : null);

        // Every 3 hits: give opponent 3 hearts (not remove)
        if (thirdHit && target instanceof Player tp) {
            double newHealth = Math.min(tp.getHealth() + 6.0, tp.getMaxHealth());
            tp.setHealth(newHealth);
            attacker.sendActionBar("§5Soul: §7Opponent healed 3 hearts!");
        }

        target.getWorld().spawnParticle(Particle.SOUL, target.getLocation(), 10, 0.5, 0.5, 0.5, 0.05);
    }

    // VOID — vortex pull only in the End; explosion everywhere
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
        target.getWorld().createExplosion(target.getLocation(), 2.0f, false, false);
        target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 50, 1, 1, 1, 0.3);
    }

    // NATURE — +1 absorption heart on every hit
    private void hitNature(Player attacker, LivingEntity target) {
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200,
            Math.min((attacker.getPotionEffect(PotionEffectType.ABSORPTION) != null
                ? attacker.getPotionEffect(PotionEffectType.ABSORPTION).getAmplifier() + 1 : 0), 9), false, false, false));
    }

    // STORM — lightning strikes target every 3 hits (was broken, firing every hit)
    private void hitStorm(Player attacker, LivingEntity target) {
        target.getWorld().strikeLightning(target.getLocation());
        attacker.sendActionBar("§e§lSTORM STRIKE!");
    }

    // FROST — slowness 2 every 3 hits
    private void hitFrost(LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
    }

    // BLOOD — always autocrit (not just during berserk); berserk doubles damage on top
    private void hitBlood(Player attacker, EntityDamageByEntityEvent event) {
        // Always autocrit: multiply by 1.5 (crit multiplier)
        event.setDamage(event.getDamage() * 1.5);

        // Berserk doubles damage on top of autocrit
        if (manager.isBloodBerserkActive(attacker.getUniqueId())) {
            event.setDamage(event.getDamage() * 2.0);
        }

        // Activate berserk on 3-hit combo
        int combo = manager.getHitCounter(attacker.getUniqueId());
        if (combo == 3 && manager.isBloodBerserkReady(attacker.getUniqueId())) {
            manager.activateBloodBerserk(attacker.getUniqueId());
            attacker.sendActionBar("§4§l⚔ BERSERK ACTIVATED ⚔");
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (attacker.isOnline()) attacker.sendActionBar("§7Berserk ended.");
            }, 200L);
        }
    }

    // SPEED — Speed V for 5s every 3 hits (30s CD)
    private void hitSpeed(Player attacker) {
        if (!manager.isSpeedHitReady(attacker.getUniqueId())) {
            attacker.sendActionBar("§bSpeed: §7CD active");
            return;
        }
        manager.setSpeedHitCD(attacker.getUniqueId());
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 4));
        attacker.sendActionBar("§b§lSPEED SURGE!");
    }
}
