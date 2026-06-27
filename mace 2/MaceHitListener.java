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

        // Shield check — if target is blocking with shield, skip abilities
        if (target instanceof Player targetPlayer) {
            ItemStack offhand = targetPlayer.getInventory().getItemInOffHand();
            ItemStack mainhand = targetPlayer.getInventory().getItemInMainHand();
            if (offhand.getType() == Material.SHIELD || mainhand.getType() == Material.SHIELD) return;
        }

        MaceType type = manager.getHeldMaceType(attacker);
        if (type == null) return;

        // Increment global hit combo counter
        int combo = manager.incrementHitCounter(attacker.getUniqueId());
        boolean thirdHit = (combo == 3);
        boolean god = (type == MaceType.GOD);

        if (type == MaceType.MAGMA || god) hitMagma(attacker, target);
        if (type == MaceType.ECHO  || god) { if (thirdHit) hitEcho(attacker, target); }
        if (type == MaceType.SOUL  || god) { if (thirdHit) hitSoul(attacker, target); }
        if (type == MaceType.VOID  || god) hitVoid(attacker, target);
        if (type == MaceType.NATURE || god) hitNature(attacker, target);
        if (type == MaceType.STORM || god) { if (thirdHit) hitStorm(attacker, target); }
        if (type == MaceType.FROST || god) { if (thirdHit) hitFrost(target); }
        if (type == MaceType.BLOOD || god) hitBlood(attacker, event);
        if (type == MaceType.SPEED || god) { if (thirdHit) hitSpeed(attacker); }
    }

    // MAGMA — explosion on every hit
    private void hitMagma(Player attacker, LivingEntity target) {
        target.getWorld().createExplosion(target.getLocation(), 1.5f, true, false, attacker);
    }

    // ECHO — blindness for 2s every 3 hits (every 20s... tracked via cooldown not implemented here for simplicity)
    private void hitEcho(Player attacker, LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
        attacker.sendActionBar("§8Echo: Blindness applied!");
    }

    // SOUL — remove 3 hearts for 10s every 3 hits (30s CD)
    private void hitSoul(Player attacker, LivingEntity target) {
        if (!manager.isSoulSpawnReady(attacker.getUniqueId())) {
            attacker.sendActionBar("§5Soul: §7" + manager.soulSpawnCDRemaining(attacker.getUniqueId()) + "s cooldown");
            return;
        }
        manager.setSoulSpawnCD(attacker.getUniqueId());
        target.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 200, -3)); // reduce max health
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0));
        attacker.sendActionBar("§5Soul: §73 hearts stolen!");
    }

    // VOID — vortex pull on every hit
    private void hitVoid(Player attacker, LivingEntity target) {
        Location center = attacker.getLocation();
        // Pull all nearby players toward attacker
        for (Entity e : attacker.getNearbyEntities(10, 10, 10)) {
            if (e instanceof Player nearby && e != attacker) {
                Vector pull = center.toVector().subtract(e.getLocation().toVector()).normalize().multiply(1.5);
                pull.setY(0.2);
                nearby.setVelocity(pull);
            }
        }
        // Explosion that doesn't harm attacker
        target.getWorld().createExplosion(target.getLocation(), 2.0f, false, false);
        target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 50, 1, 1, 1, 0.3);
    }

    // NATURE — +1 absorption heart on every hit
    private void hitNature(Player attacker, LivingEntity target) {
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200,
            Math.min((attacker.getPotionEffect(PotionEffectType.ABSORPTION) != null
                ? attacker.getPotionEffect(PotionEffectType.ABSORPTION).getAmplifier() + 1 : 0), 9), false, false, false));
    }

    // STORM — lightning strikes target every 3 hits
    private void hitStorm(Player attacker, LivingEntity target) {
        target.getWorld().strikeLightning(target.getLocation());
    }

    // FROST — slowness 2 every 3 hits
    private void hitFrost(LivingEntity target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
    }

    // BLOOD — autocrit during berserk
    private void hitBlood(Player attacker, EntityDamageByEntityEvent event) {
        if (manager.isBloodBerserkActive(attacker.getUniqueId())) {
            event.setDamage(event.getDamage() * 2.0);
        }
        // Check if berserk should activate (3-hit combo)
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
            attacker.sendActionBar("§bSpeed: §7" + "CD active");
            return;
        }
        manager.setSpeedHitCD(attacker.getUniqueId());
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 4)); // Speed V
        attacker.sendActionBar("§b§lSPEED SURGE!");
    }
}
