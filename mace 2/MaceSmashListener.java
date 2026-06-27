package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import com.custommaces.managers.MaceManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MaceSmashListener implements Listener {

    private final CustomMaces plugin;
    private final MaceManager manager;

    public MaceSmashListener(CustomMaces plugin) {
        this.plugin = plugin;
        this.manager = plugin.getMaceManager();
    }

    @EventHandler
    public void onSmash(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (attacker.getFallDistance() < 1.5f) return;

        // Shield check
        if (target instanceof Player tp) {
            if (tp.getInventory().getItemInOffHand().getType() == Material.SHIELD ||
                tp.getInventory().getItemInMainHand().getType() == Material.SHIELD) return;
        }

        MaceType type = manager.getHeldMaceType(attacker);
        if (type == null) return;

        Location loc = target.getLocation();
        boolean god = (type == MaceType.GOD);

        if (type == MaceType.MAGMA  || god) smashMagma(attacker, loc);
        if (type == MaceType.ECHO   || god) smashEcho(attacker, loc);
        if (type == MaceType.SOUL   || god) smashSoul(attacker, loc);
        if (type == MaceType.VOID   || god) smashVoid(attacker, loc);
        if (type == MaceType.NATURE || god) smashNature(attacker, loc);
        if (type == MaceType.STORM  || god) smashStorm(attacker, loc);
        if (type == MaceType.FROST  || god) smashFrost(attacker, loc);
        if (type == MaceType.BLOOD  || god) smashBlood(attacker);
        if (type == MaceType.SPEED  || god) smashSpeed(attacker, loc);

        if (god) attacker.sendTitle("§6§l✦ GOD SMASH ✦", "", 5, 30, 10);
    }

    private void smashMagma(Player attacker, Location loc) {
        World w = loc.getWorld();
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++) {
                Block b = loc.clone().add(x, 0, z).getBlock();
                if (b.getType() == Material.AIR) {
                    b.setType(Material.FIRE);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (b.getType() == Material.FIRE) b.setType(Material.AIR);
                    }, 60L);
                }
            }
        for (Entity e : attacker.getNearbyEntities(4, 4, 4))
            if (e instanceof LivingEntity le && e != attacker) {
                le.setFireTicks(100);
                le.setVelocity(le.getVelocity().add(new Vector(0, 1.2, 0)));
            }
        w.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.8f);
        w.spawnParticle(Particle.FLAME, loc, 60, 2, 0.5, 2, 0.1);
    }

    private void smashEcho(Player attacker, Location loc) {
        World w = loc.getWorld();
        for (Entity e : attacker.getNearbyEntities(6, 4, 6))
            if (e instanceof LivingEntity le && e != attacker) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            }
        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.0f);
        w.spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
    }

    private void smashSoul(Player attacker, Location loc) {
        World w = loc.getWorld();
        // Spawn random hostile mob on target location
        EntityType[] mobs = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                             EntityType.SPIDER, EntityType.WITCH, EntityType.BLAZE,
                             EntityType.CAVE_SPIDER, EntityType.ENDERMAN};
        EntityType mob = mobs[(int)(Math.random() * mobs.length)];
        w.spawnEntity(loc, mob);
        // Pull nearby entities inward
        for (Entity e : attacker.getNearbyEntities(6, 4, 6))
            if (e instanceof LivingEntity le && e != attacker) {
                Vector pull = loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(2.0);
                pull.setY(0.3);
                le.setVelocity(pull);
            }
        w.playSound(loc, Sound.ENTITY_WITHER_HURT, 1.0f, 1.5f);
        w.spawnParticle(Particle.SOUL, loc, 50, 2, 1, 2, 0.05);
    }

    private void smashVoid(Player attacker, Location loc) {
        World w = loc.getWorld();
        for (Entity e : attacker.getNearbyEntities(5, 4, 5))
            if (e instanceof LivingEntity le && e != attacker) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 1));
                Vector pull = loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(2.0);
                le.setVelocity(pull);
            }
        w.createExplosion(loc, 3.0f, false, false);
        w.playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 0.5f);
        w.spawnParticle(Particle.PORTAL, loc, 80, 1.5, 1, 1.5, 0.5);
    }

    private void smashNature(Player attacker, Location loc) {
        World w = loc.getWorld();
        for (Entity e : attacker.getNearbyEntities(5, 3, 5))
            if (e instanceof LivingEntity le && e != attacker) {
                le.setVelocity(new Vector(0, 0, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10));
                le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                new BukkitRunnable() {
                    int t = 0;
                    public void run() {
                        if (t++ >= 60 || !le.isValid()) { cancel(); return; }
                        le.setVelocity(new Vector(0, le.getVelocity().getY(), 0));
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        w.playSound(loc, Sound.BLOCK_GRASS_BREAK, 1.5f, 0.6f);
        w.spawnParticle(Particle.HAPPY_VILLAGER, loc, 40, 2, 0.5, 2, 0.1);
    }

    private void smashStorm(Player attacker, Location loc) {
        World w = loc.getWorld();
        double[][] offsets = {{2,2},{-2,2},{2,-2},{-2,-2},{0,0}};
        for (double[] off : offsets) w.strikeLightning(loc.clone().add(off[0], 0, off[1]));
        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.0f);
    }

    private void smashFrost(Player attacker, Location loc) {
        int count = manager.incrementFrostSmash(attacker.getUniqueId());
        if (count != 3) return; // only on every 3rd smash
        World w = loc.getWorld();
        for (Entity e : attacker.getNearbyEntities(5, 3, 5))
            if (e instanceof LivingEntity le && e != attacker) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
                le.setVelocity(new Vector(0, 0, 0));
                new BukkitRunnable() {
                    int t = 0;
                    public void run() {
                        if (t++ >= 40 || !le.isValid()) { cancel(); return; }
                        le.setVelocity(new Vector(0, le.getVelocity().getY(), 0));
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        w.playSound(loc, Sound.BLOCK_POWDER_SNOW_PLACE, 1.5f, 0.5f);
        w.spawnParticle(Particle.SNOWFLAKE, loc, 60, 2, 0.5, 2, 0.1);
    }

    private void smashBlood(Player attacker) {
        attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
        attacker.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, attacker.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
    }

    private void smashSpeed(Player attacker, Location loc) {
        World w = loc.getWorld();
        for (Entity e : attacker.getNearbyEntities(5, 3, 5))
            if (e instanceof LivingEntity le && e != attacker) {
                Vector dir = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3.0);
                dir.setY(0.8);
                le.setVelocity(dir);
            }
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 2));
        w.playSound(loc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.5f, 1.2f);
        w.spawnParticle(Particle.CLOUD, loc, 40, 1, 0.5, 1, 0.2);
    }
}
