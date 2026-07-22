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

        if (target instanceof Player tp) {
            if (tp.getInventory().getItemInOffHand().getType() == Material.SHIELD ||
                tp.getInventory().getItemInMainHand().getType() == Material.SHIELD) return;
        }

        MaceType type = manager.getHeldMaceType(attacker);
        if (type == null) return;

        Location loc = target.getLocation();
        boolean god = (type == MaceType.GOD);

        if (god) {
            MaceType godSmash = manager.getGodSmashAbility(attacker.getUniqueId());
            if (godSmash != null) {
                executeSmash(godSmash, attacker, target, loc);
                attacker.sendTitle("§6§l✦ " + godSmash.getDisplayName().toUpperCase() + " SMASH ✦", "", 5, 30, 10);
            } else {
                for (MaceType t : MaceType.values()) {
                    if (t != MaceType.GOD) executeSmash(t, attacker, target, loc);
                }
                attacker.sendTitle("§6§l✦ GOD SMASH ✦", "", 5, 30, 10);
            }
            return;
        }

        executeSmash(type, attacker, target, loc);
    }

    private void executeSmash(MaceType type, Player attacker, LivingEntity target, Location loc) {
        switch (type) {
            case MAGMA  -> smashMagma(attacker, loc);
            case ECHO   -> smashEcho(attacker, target, loc);
            case SOUL   -> smashSoul(attacker, target, loc);
            case VOID   -> smashVoid(attacker, target, loc);
            case NATURE -> smashNature(attacker, loc);
            case STORM  -> smashStorm(attacker, loc);
            case FROST  -> smashFrost(attacker, loc);
            case BLOOD  -> smashBlood(attacker);
            case SPEED  -> smashSpeed(attacker, loc);
        }
    }

    // MAGMA — fire ring that only spreads fire, no block destruction
    private void smashMagma(Player attacker, Location loc) {
        World w = loc.getWorld();
        int radius = 3;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue; // circular
                Block ground = loc.clone().add(x, -1, z).getBlock();
                Block air    = loc.clone().add(x,  0, z).getBlock();
                // Only place fire on top of solid blocks; never break/replace solid blocks
                if (ground.getType().isSolid() && air.getType() == Material.AIR) {
                    air.setType(Material.FIRE);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (air.getType() == Material.FIRE) air.setType(Material.AIR);
                    }, 80L);
                }
            }
        }
        // Set nearby entities on fire — no explosion (that was the crash cause)
        for (Entity e : attacker.getNearbyEntities(4, 4, 4)) {
            if (e instanceof LivingEntity le && e != attacker) {
                le.setFireTicks(100);
                le.setVelocity(le.getVelocity().add(new Vector(0, 1.0, 0)));
            }
        }
        w.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.8f);
        w.spawnParticle(Particle.FLAME, loc, 60, 2, 0.5, 2, 0.1);
    }

    // ECHO SMASH — Sonic Shockwave: 3 hearts true damage to all nearby, darkness+blindness, launch backward
    // 60s cooldown
    private void smashEcho(Player attacker, LivingEntity target, Location loc) {
        if (!manager.isEchoSmashReady(attacker.getUniqueId())) {
            attacker.sendActionBar("§8Echo Smash: §7" + manager.echoSmashCDRemaining(attacker.getUniqueId()) + "s cooldown");
            return;
        }
        manager.setEchoSmashCD(attacker.getUniqueId());

        World w = loc.getWorld();
        // Strip blindness from attacker first (warden immunity)
        attacker.removePotionEffect(PotionEffectType.BLINDNESS);
        attacker.removePotionEffect(PotionEffectType.DARKNESS);

        for (Entity e : attacker.getNearbyEntities(8, 4, 8)) {
            if (!(e instanceof LivingEntity le) || e == attacker) continue;

            // Launch backward (away from attacker)
            Vector knockback = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.5);
            knockback.setY(0.8);
            le.setVelocity(knockback);

            // Darkness + blindness (5s)
            le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
            le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));

            // 3 hearts true damage (6 HP) — scheduled to bypass armor
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (le.isValid()) le.damage(6.0, attacker);
            }, 1L);
        }

        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.8f);
        w.spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.EXPLOSION, loc, 8, 2, 0.5, 2, 0.1);
        attacker.sendTitle("§8§l✦ SONIC SHOCKWAVE ✦", "", 5, 25, 10);
    }

    // SOUL SMASH — Soul Drain: drain 1 heart/s from all nearby for 5s, heal self for each
    // 45s cooldown
    private void smashSoul(Player attacker, LivingEntity target, Location loc) {
        if (!manager.isSoulDrainReady(attacker.getUniqueId())) {
            attacker.sendActionBar("§5Soul Drain: §7" + manager.soulDrainCDRemaining(attacker.getUniqueId()) + "s cooldown");
            return;
        }
        manager.setSoulDrainCD(attacker.getUniqueId());

        World w = loc.getWorld();
        attacker.sendTitle("§5§l✦ SOUL DRAIN ✦", "§7Draining nearby enemies...", 5, 40, 10);

        new BukkitRunnable() {
            int ticks = 0;
            public void run() {
                if (ticks >= 5 || !attacker.isOnline()) { cancel(); return; }
                ticks++;

                double healPerTarget = 0;
                for (Entity e : attacker.getNearbyEntities(8, 4, 8)) {
                    if (!(e instanceof LivingEntity le) || e == attacker) continue;
                    // Drain 1 heart (2 HP) true damage
                    le.damage(2.0, attacker);
                    healPerTarget += 2.0;
                    w.spawnParticle(Particle.SOUL, le.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.05);
                }
                // Heal attacker for every target drained
                if (healPerTarget > 0) {
                    double newHp = Math.min(attacker.getHealth() + healPerTarget, attacker.getMaxHealth());
                    attacker.setHealth(newHp);
                    attacker.sendActionBar("§5Soul Drain: §7+" + (int)(healPerTarget / 2) + " hearts!");
                }
                w.playSound(loc, Sound.ENTITY_WITHER_HURT, 0.8f, 1.5f);
            }
        }.runTaskTimer(plugin, 0L, 20L); // every second for 5s

        w.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        w.spawnParticle(Particle.SOUL, loc, 50, 2, 1, 2, 0.05);
    }

    // VOID SMASH — Singularity: pull everything in for 4s, explode for 4 hearts true, attacker NOT pulled
    // 60s cooldown
    private void smashVoid(Player attacker, LivingEntity target, Location loc) {
        if (!manager.isVoidSingularityReady(attacker.getUniqueId())) {
            attacker.sendActionBar("§3Singularity: §7" + manager.voidSingularityCDRemaining(attacker.getUniqueId()) + "s cooldown");
            return;
        }
        manager.setVoidSingularityCD(attacker.getUniqueId());

        World w = loc.getWorld();
        attacker.sendTitle("§3§l✦ SINGULARITY ✦", "", 5, 30, 10);

        // Pull phase: 4 seconds
        new BukkitRunnable() {
            int ticks = 0;
            public void run() {
                if (ticks >= 80) { // 4 seconds = 80 ticks
                    cancel();
                    // Explosion phase: 4 hearts true damage to all nearby (not attacker)
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3, 0.5, 0.5, 0.5, 0);
                    w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                    for (Entity e : attacker.getNearbyEntities(8, 8, 8)) {
                        if (!(e instanceof LivingEntity le) || e == attacker) continue;
                        // 4 hearts = 8 HP true damage
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (le.isValid()) le.damage(8.0, attacker);
                        }, 1L);
                        // Launch outward after implosion
                        Vector blast = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3.0);
                        blast.setY(1.2);
                        le.setVelocity(blast);
                    }
                    return;
                }
                ticks++;
                // Pull nearby entities in (but NOT the attacker)
                for (Entity e : attacker.getNearbyEntities(12, 8, 12)) {
                    if (!(e instanceof LivingEntity le) || e == attacker) continue;
                    Vector pull = loc.toVector().subtract(e.getLocation().toVector());
                    if (pull.length() > 0.5) {
                        pull = pull.normalize().multiply(0.4);
                        pull.setY(pull.getY() + 0.05);
                        le.setVelocity(le.getVelocity().add(pull));
                    }
                }
                // Spiral particle effect
                if (ticks % 2 == 0)
                    w.spawnParticle(Particle.PORTAL, loc, 10, 1.5, 1.5, 1.5, 0.3);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        w.playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 0.3f);
    }

    private void smashNature(Player attacker, Location loc) {
        World w = loc.getWorld();
        for (Entity e : attacker.getNearbyEntities(5, 3, 5)) {
            if (!(e instanceof LivingEntity le) || e == attacker) continue;
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

    // FROST SMASH — bigger (7 radius), longer (10s), pushes players away
    private void smashFrost(Player attacker, Location loc) {
        int count = manager.incrementFrostSmash(attacker.getUniqueId());
        if (count != 3) return;

        World w = loc.getWorld();
        int radius = 7;

        // Push all nearby players away first
        for (Entity e : attacker.getNearbyEntities(radius, 5, radius)) {
            if (!(e instanceof LivingEntity le) || e == attacker) continue;
            Vector push = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.5);
            push.setY(0.6);
            le.setVelocity(push);
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 1)); // 10s slowness
        }

        // Powder snow sphere (bigger, 7 radius)
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x*x + y*y + z*z > radius * radius) continue;
                    Block b = loc.clone().add(x, y, z).getBlock();
                    if (b.getType() == Material.AIR) {
                        b.setType(Material.POWDER_SNOW);
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (b.getType() == Material.POWDER_SNOW) b.setType(Material.AIR);
                        }, 200L); // 10 seconds
                    }
                }
            }
        }

        w.playSound(loc, Sound.BLOCK_POWDER_SNOW_PLACE, 1.5f, 0.5f);
        w.spawnParticle(Particle.SNOWFLAKE, loc, 100, 3, 1.5, 3, 0.1);
    }

    private void smashBlood(Player attacker) {
        attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
        attacker.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, attacker.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
    }

    private void smashSpeed(Player attacker, Location loc) {
        World w = loc.getWorld();
        for (Entity e : attacker.getNearbyEntities(5, 3, 5)) {
            if (!(e instanceof LivingEntity le) || e == attacker) continue;
            Vector dir = e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(3.0);
            dir.setY(0.8);
            le.setVelocity(dir);
        }
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 2));
        w.playSound(loc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.5f, 1.2f);
        w.spawnParticle(Particle.CLOUD, loc, 40, 1, 0.5, 1, 0.2);
    }
}
