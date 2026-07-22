package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import com.custommaces.managers.MaceManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class MaceAbilityListener implements Listener {

    private final CustomMaces plugin;
    private final MaceManager manager;

    public MaceAbilityListener(CustomMaces plugin) {
        this.plugin = plugin;
        this.manager = plugin.getMaceManager();
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        MaceType type = manager.getHeldMaceType(p);
        if (type == null) return;
        if (type == MaceType.GOD) return;

        switch (type) {
            case MAGMA  -> abilityMagma(p);
            case ECHO   -> abilityEcho(p);
            case SOUL   -> abilitySoul(p);
            case VOID   -> abilityVoid(p);
            case NATURE -> abilityNature(p);
            case STORM  -> abilityStorm(p);
            case FROST  -> abilityFrost(p);
            case BLOOD  -> {}
            case SPEED  -> abilitySpeed(p);
            default -> {}
        }
    }

    // MAGMA — shoot 3 fireballs, 1 second apart, 2 charges (each charge = all 3 balls)
    private void abilityMagma(Player p) {
        int charges = manager.getMagmaCharges(p.getUniqueId());
        if (charges <= 0) {
            p.sendActionBar("§cMagma: §7No charges! Recharging...");
            return;
        }
        manager.useMagmaCharge(p.getUniqueId());
        p.sendActionBar("§c🔥 Firing 3 fireballs! §7Charges: §c" + (charges - 1) + "/2");

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline()) return;
                Fireball fb = p.getWorld().spawn(p.getEyeLocation(), Fireball.class);
                // Slight spread between the 3 fireballs
                Vector dir = p.getLocation().getDirection().clone();
                if (idx == 1) dir.add(new Vector(0.05, 0, 0.05));
                if (idx == 2) dir.add(new Vector(-0.05, 0, -0.05));
                fb.setDirection(dir.multiply(2));
                fb.setShooter(p);
                fb.setYield(1.5f); // smaller explosion so it doesn't crash
                // 1.5 hearts true damage on hit — handled via EntityDamageByEntityEvent in MaceHitListener
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
            }, i * 20L); // 0s, 1s, 2s
        }
    }

    // ECHO — sonic beam, 60s CD
    private void abilityEcho(Player p) {
        if (!manager.isEchoBeamReady(p.getUniqueId())) {
            p.sendActionBar("§8Echo beam: §7" + manager.echoBeamCDRemaining(p.getUniqueId()) + "s");
            return;
        }
        manager.setEchoBeamCD(p.getUniqueId());

        // Warden immunity: strip blindness on use
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        p.removePotionEffect(PotionEffectType.DARKNESS);

        RayTraceResult result = p.getWorld().rayTraceEntities(
            p.getEyeLocation(), p.getLocation().getDirection(), 30,
            e -> e instanceof LivingEntity && e != p
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            // 4 hearts true damage
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (target.isValid()) target.damage(8.0, p);
            }, 1L);
            target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
            // Beam particles
            Location start = p.getEyeLocation();
            Location end = target.getLocation().add(0, 1, 0);
            Vector step = end.toVector().subtract(start.toVector()).normalize().multiply(0.5);
            Location cur = start.clone();
            for (int i = 0; i < 60; i++) {
                cur.add(step);
                p.getWorld().spawnParticle(Particle.SONIC_BOOM, cur, 1, 0, 0, 0, 0);
                if (cur.distance(end) < 1) break;
            }
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.0f);
    }

    // SOUL — spawn hostile mob, 30s CD
    private void abilitySoul(Player p) {
        if (!manager.isSoulSpawnReady(p.getUniqueId())) {
            p.sendActionBar("§5Soul spawn: §7" + manager.soulSpawnCDRemaining(p.getUniqueId()) + "s");
            return;
        }
        manager.setSoulSpawnCD(p.getUniqueId());

        EntityType[] mobs = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                             EntityType.SPIDER, EntityType.WITCH, EntityType.BLAZE, EntityType.ENDERMAN};
        EntityType mob = mobs[(int)(Math.random() * mobs.length)];
        Entity spawned = p.getWorld().spawnEntity(p.getLocation().add(1, 0, 1), mob);

        if (spawned instanceof Mob mobEntity) {
            LivingEntity nearest = null;
            double dist = Double.MAX_VALUE;
            for (Entity e : p.getNearbyEntities(20, 20, 20)) {
                if (e instanceof LivingEntity le && e != p && e != spawned) {
                    double d = e.getLocation().distance(p.getLocation());
                    if (d < dist) { dist = d; nearest = le; }
                }
            }
            if (nearest != null) mobEntity.setTarget(nearest);
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
    }

    // VOID — dragon breath: 2 hearts true on hit + 0.5 hearts true every tick for 3s on the area
    private void abilityVoid(Player p) {
        boolean inEnd = p.getWorld().getEnvironment() == World.Environment.THE_END;
        int maxCharges = inEnd ? 3 : 2;
        int charges = Math.min(manager.getVoidCharges(p.getUniqueId()), maxCharges);
        if (charges <= 0) {
            p.sendActionBar("§3Void: §7No charges! Recharging..." + (inEnd ? "" : " §8(Max 2 in Overworld)"));
            return;
        }
        manager.useVoidCharge(p.getUniqueId());

        // Spawn a custom dragon fireball that we track
        DragonFireball ball = p.getWorld().spawn(p.getEyeLocation(), DragonFireball.class);
        ball.setDirection(p.getLocation().getDirection().multiply(2));
        ball.setShooter(p);

        // When the dragon fireball lands it creates a lingering pool — we intercept via a
        // separate listener on EntityExplodeEvent in MaceHitListener, but for the per-tick true
        // damage we track the area where it lands using a delayed task after 40 ticks (estimated travel)
        Location fireLoc = ball.getLocation().clone();

        // Every tick of the pool (3s = 60 ticks), deal 0.5 hearts true damage to anyone in the cloud
        new BukkitRunnable() {
            int elapsed = 0;
            public void run() {
                if (elapsed >= 60 || !ball.isValid() && elapsed > 5) {
                    cancel();
                    return;
                }
                elapsed++;
                Location poolLoc = ball.isValid() ? ball.getLocation() : fireLoc;
                for (Entity e : p.getWorld().getNearbyEntities(poolLoc, 2.5, 1.5, 2.5)) {
                    if (!(e instanceof LivingEntity le) || e == p) continue;
                    // 0.5 hearts = 1.0 HP per tick true damage
                    le.damage(1.0, p);
                }
            }
        }.runTaskTimer(plugin, 5L, 1L);

        // Initial 2 hearts true damage when the ball first hits — schedule a raytrace check
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            RayTraceResult result = p.getWorld().rayTraceEntities(
                p.getEyeLocation(), p.getLocation().getDirection(), 30,
                e -> e instanceof LivingEntity && e != p
            );
            if (result != null && result.getHitEntity() instanceof LivingEntity hit) {
                hit.damage(4.0, p); // 2 hearts = 4 HP true
            }
        }, 3L);

        p.sendActionBar("§3Dragon breath: §b" + (charges - 1) + "/" + maxCharges + " charges");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);
    }

    // NATURE — grapple: pulls players/mobs toward you; also latches onto windcharges and pearls
    private void abilityNature(Player p) {
        if (!manager.isNatureGrappleReady(p.getUniqueId())) {
            p.sendActionBar("§aGrapple: §7" + manager.natureGrappleCDRemaining(p.getUniqueId()) + "s cooldown");
            return;
        }

        // Check for wind charges and ender pearls in flight first
        Entity projectileTarget = null;
        double closestAngle = 0.3; // within ~17 degrees of crosshair
        Vector look = p.getLocation().getDirection();
        for (Entity e : p.getNearbyEntities(30, 30, 30)) {
            if (e instanceof WindCharge || e instanceof EnderPearl || e instanceof Enderpearl) {
                Vector toEntity = e.getLocation().toVector().subtract(p.getEyeLocation().toVector()).normalize();
                double dot = toEntity.dot(look);
                if (dot > closestAngle) { closestAngle = dot; projectileTarget = e; }
            }
        }

        if (projectileTarget != null) {
            // Grapple to the wind charge / pearl: launch toward it
            Vector dir = projectileTarget.getLocation().toVector()
                .subtract(p.getLocation().toVector()).normalize().multiply(2.8);
            dir.setY(Math.min(dir.getY() + 0.4, 1.5));
            p.setVelocity(dir);
            manager.setNatureGrappleCD(p.getUniqueId());
            p.sendActionBar("§aGrappled to §f" + projectileTarget.getType().name().replace("_", " ") + "§a!");
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_VINE_PLACE, 1.0f, 1.2f);
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation(), 10, 0.5, 0.5, 0.5, 0);
            return;
        }

        // Check for living entity (player/mob) to pull toward attacker
        RayTraceResult entityResult = p.getWorld().rayTraceEntities(
            p.getEyeLocation(), p.getLocation().getDirection(), 30,
            e -> e instanceof LivingEntity && e != p
        );

        if (entityResult != null && entityResult.getHitEntity() instanceof LivingEntity hitEntity) {
            Vector pull = p.getLocation().toVector()
                .subtract(hitEntity.getLocation().toVector()).normalize().multiply(2.5);
            pull.setY(0.5);
            hitEntity.setVelocity(pull);
            manager.setNatureGrappleCD(p.getUniqueId());
            p.sendActionBar("§aGrapple: §7Pulled target!");
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_VINE_PLACE, 1.0f, 1.0f);
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, hitEntity.getLocation(), 10, 0.5, 0.5, 0.5, 0);
            return;
        }

        // Fallback: grapple to block
        RayTraceResult blockResult = p.getWorld().rayTraceBlocks(
            p.getEyeLocation(), p.getLocation().getDirection(), 30
        );
        if (blockResult == null || blockResult.getHitBlock() == null) {
            p.sendActionBar("§aNo target in range!");
            return;
        }
        Block target = blockResult.getHitBlock();
        Vector dir = target.getLocation().add(0.5, 0.5, 0.5).toVector()
            .subtract(p.getLocation().toVector()).normalize().multiply(2.5);
        dir.setY(Math.min(dir.getY() + 0.5, 1.5));
        p.setVelocity(dir);
        manager.setNatureGrappleCD(p.getUniqueId());
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_VINE_PLACE, 1.0f, 1.0f);
        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation(), 10, 0.5, 0.5, 0.5, 0);
    }

    // STORM — launch upward, 25s CD
    private void abilityStorm(Player p) {
        if (!manager.isStormAbilityReady(p.getUniqueId())) {
            p.sendActionBar("§eStorm: §7" + manager.stormAbilityCDRemaining(p.getUniqueId()) + "s cooldown");
            return;
        }
        manager.setStormAbilityCD(p.getUniqueId());
        p.getWorld().strikeLightningEffect(p.getLocation());
        p.setVelocity(new Vector(0, 3.5, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.5f);
        p.sendActionBar("§e⚡ Storm Launch! §7" + manager.stormAbilityCDRemaining(p.getUniqueId()) + "s CD");
    }

    // FROST — ice beam, 15s CD
    private void abilityFrost(Player p) {
        if (!manager.isFrostBeamReady(p.getUniqueId())) {
            p.sendActionBar("§fFrost beam: §7" + manager.frostBeamCDRemaining(p.getUniqueId()) + "s cooldown");
            return;
        }
        manager.setFrostBeamCD(p.getUniqueId());

        RayTraceResult result = p.getWorld().rayTraceEntities(
            p.getEyeLocation(), p.getLocation().getDirection(), 30,
            e -> e instanceof LivingEntity && e != p
        );

        Vector dir = p.getLocation().getDirection().normalize().multiply(0.5);
        Location cur = p.getEyeLocation().clone();
        for (int i = 0; i < 60; i++) {
            cur.add(dir);
            p.getWorld().spawnParticle(Particle.SNOWFLAKE, cur, 2, 0.1, 0.1, 0.1, 0);
            if (result != null && result.getHitEntity() != null
                && cur.distance(result.getHitEntity().getLocation()) < 1.5) break;
        }

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 127));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
            plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
                if (!target.isValid()) { task.cancel(); return; }
                target.setVelocity(new Vector(0, target.getVelocity().getY(), 0));
            }, 0L, 1L);
            plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> target.removePotionEffect(PotionEffectType.SLOWNESS), 60L);
        }
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_POWDER_SNOW_PLACE, 1.5f, 0.5f);
    }

    // SPEED — dash, 3 charges
    private void abilitySpeed(Player p) {
        int charges = manager.getSpeedDashes(p.getUniqueId());
        if (charges <= 0) { p.sendActionBar("§bNo dash charges! Recharging..."); return; }
        manager.useSpeedDash(p.getUniqueId());
        Vector dir = p.getLocation().getDirection().normalize().multiply(2.5);
        dir.setY(0.4);
        p.setVelocity(dir);
        p.sendActionBar("§bDash! §7Charges: §3" + (charges - 1) + "/3");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.5f);
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
    }
}
