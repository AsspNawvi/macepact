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

        MaceType type = manager.getHeldMaceType(attacker);
        if (type == null) return;

        Location impactLoc = target.getLocation();

        if (type == MaceType.GOD) {
            // Fire ALL smash abilities at once with a slight delay stagger for effect
            smashMagma(attacker, impactLoc);
            smashTide(attacker, impactLoc);
            smashSoul(attacker, impactLoc);
            smashVoid(attacker, impactLoc);
            smashNature(attacker, impactLoc);
            smashStorm(attacker, impactLoc);
            smashFrost(attacker, impactLoc);
            smashBlood(attacker);
            smashSpeed(attacker);
            attacker.sendTitle("§6§l✦ GOD SMASH ✦", "§eAll powers unleashed!", 5, 30, 10);
            attacker.getWorld().playSound(impactLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 0.8f);
        } else {
            switch (type) {
                case MAGMA -> smashMagma(attacker, impactLoc);
                case TIDE  -> smashTide(attacker, impactLoc);
                case SOUL  -> smashSoul(attacker, impactLoc);
                case VOID  -> smashVoid(attacker, impactLoc);
                case NATURE -> smashNature(attacker, impactLoc);
                case STORM -> smashStorm(attacker, impactLoc);
                case FROST -> smashFrost(attacker, impactLoc);
                case BLOOD -> smashBlood(attacker);
                case SPEED -> smashSpeed(attacker);
            }
        }
    }

    private void smashMagma(Player attacker, Location loc) {
        World world = loc.getWorld();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Block block = loc.clone().add(x, 0, z).getBlock();
                if (block.getType() == Material.AIR) block.setType(Material.FIRE);
            }
        }
        for (Entity entity : attacker.getNearbyEntities(4, 4, 4)) {
            if (entity instanceof LivingEntity le && entity != attacker) {
                le.setFireTicks(100);
                le.setVelocity(le.getVelocity().add(new Vector(0, 1.2, 0)));
            }
        }
        world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.8f);
        world.spawnParticle(Particle.FLAME, loc, 60, 2, 0.5, 2, 0.1);
        if (attacker.getInventory().getItemInMainHand() != null)
            attacker.sendActionBar("§c§lMAGMA SMASH!");
    }

    private void smashTide(Player attacker, Location loc) {
        World world = loc.getWorld();
        for (Entity entity : attacker.getNearbyEntities(5, 3, 5)) {
            if (entity instanceof LivingEntity le && entity != attacker) {
                Vector dir = entity.getLocation().toVector().subtract(loc.toVector()).normalize();
                dir.multiply(2.5).setY(0.5);
                le.setVelocity(dir);
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
            }
        }
        world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, 1.2f, 0.7f);
        world.spawnParticle(Particle.SPLASH, loc, 100, 2, 0.5, 2, 0.3);
    }

    private void smashSoul(Player attacker, Location loc) {
        World world = loc.getWorld();
        for (Entity entity : attacker.getNearbyEntities(6, 4, 6)) {
            if (entity instanceof LivingEntity le && entity != attacker) {
                Vector dir = loc.toVector().subtract(entity.getLocation().toVector()).normalize();
                dir.multiply(2.0).setY(0.3);
                le.setVelocity(dir);
                le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));
            }
        }
        double newHealth = Math.min(attacker.getHealth() + 6, attacker.getMaxHealth());
        attacker.setHealth(newHealth);
        world.playSound(loc, Sound.ENTITY_WITHER_HURT, 1.0f, 1.5f);
        world.spawnParticle(Particle.SOUL, loc, 50, 2, 1, 2, 0.05);
    }

    private void smashVoid(Player attacker, Location loc) {
        World world = loc.getWorld();
        for (Entity entity : attacker.getNearbyEntities(5, 4, 5)) {
            if (entity instanceof LivingEntity le && entity != attacker) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 1));
            }
        }
        world.playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 0.5f);
        world.spawnParticle(Particle.PORTAL, loc, 80, 1.5, 1, 1.5, 0.5);
    }

    private void smashNature(Player attacker, Location loc) {
        World world = loc.getWorld();
        for (Entity entity : attacker.getNearbyEntities(5, 3, 5)) {
            if (entity instanceof LivingEntity le && entity != attacker) {
                le.setVelocity(new Vector(0, 0, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10));
                le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override public void run() {
                        if (ticks >= 60 || !le.isValid()) { cancel(); return; }
                        le.setVelocity(new Vector(0, le.getVelocity().getY(), 0));
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }
        world.playSound(loc, Sound.BLOCK_GRASS_BREAK, 1.5f, 0.6f);
        world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 40, 2, 0.5, 2, 0.1);
    }

    private void smashStorm(Player attacker, Location loc) {
        World world = loc.getWorld();
        double[][] offsets = {{2,2},{-2,2},{2,-2},{-2,-2},{0,0}};
        for (double[] off : offsets) world.strikeLightning(loc.clone().add(off[0], 0, off[1]));
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.0f);
    }

    private void smashFrost(Player attacker, Location loc) {
        World world = loc.getWorld();
        for (Entity entity : attacker.getNearbyEntities(5, 3, 5)) {
            if (entity instanceof LivingEntity le && entity != attacker) {
                le.setVelocity(new Vector(0, 0, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 127));
                le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1));
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override public void run() {
                        if (ticks >= 40 || !le.isValid()) { cancel(); return; }
                        le.setVelocity(new Vector(0, le.getVelocity().getY(), 0));
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                le.damage(4.0, attacker);
            }
        }
        world.playSound(loc, Sound.BLOCK_POWDER_SNOW_PLACE, 1.5f, 0.5f);
        world.spawnParticle(Particle.SNOWFLAKE, loc, 60, 2, 0.5, 2, 0.1);
    }

    private void smashBlood(Player attacker) {
        manager.startBerserk(attacker.getUniqueId(), 10);
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 200, 3));
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 2));
        attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
        attacker.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, attacker.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (attacker.isOnline()) attacker.sendActionBar("§7Berserk faded.");
        }, 200L);
    }

    private void smashSpeed(Player attacker) {
        Location loc = attacker.getLocation();
        World world = loc.getWorld();
        for (Entity entity : attacker.getNearbyEntities(5, 3, 5)) {
            if (entity instanceof LivingEntity le && entity != attacker) {
                Vector dir = entity.getLocation().toVector().subtract(loc.toVector()).normalize();
                dir.multiply(3.0).setY(0.8);
                le.setVelocity(dir);
            }
        }
        Vector backward = attacker.getLocation().getDirection().multiply(-2.5);
        backward.setY(0.5);
        attacker.setVelocity(backward);
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 2));
        world.playSound(loc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.5f, 1.2f);
        world.spawnParticle(Particle.CLOUD, loc, 40, 1, 0.5, 1, 0.2);
    }
}
