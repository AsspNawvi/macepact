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

        // God mace: shift + right click opens GUI (handled in GodMaceGUIListener)
        if (type == MaceType.GOD) return;

        switch (type) {
            case MAGMA  -> abilityMagma(p);
            case ECHO   -> abilityEcho(p);
            case SOUL   -> abilitySoul(p);
            case VOID   -> abilityVoid(p);
            case NATURE -> abilityNature(p);
            case STORM  -> abilityStorm(p);
            case FROST  -> abilityFrost(p);
            case BLOOD  -> {} // Blood berserk triggers on 3-hit combo in hit listener
            case SPEED  -> abilitySpeed(p);
            default -> {}
        }
    }

    // MAGMA — launch 2 fireballs
    private void abilityMagma(Player p) {
        for (int i = 0; i < 2; i++) {
            Fireball fb = p.getWorld().spawn(p.getEyeLocation(), Fireball.class);
            fb.setDirection(p.getLocation().getDirection().multiply(2));
            fb.setShooter(p);
            fb.setYield(2.0f);
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
    }

    // ECHO — warden sonic beam (60s CD)
    private void abilityEcho(Player p) {
        if (!manager.isEchoBeamReady(p.getUniqueId())) {
            p.sendActionBar("§8Echo beam: §7" + manager.echoBeamCDRemaining(p.getUniqueId()) + "s");
            return;
        }
        manager.setEchoBeamCD(p.getUniqueId());

        RayTraceResult result = p.getWorld().rayTraceEntities(
            p.getEyeLocation(), p.getLocation().getDirection(), 30,
            e -> e instanceof LivingEntity && e != p
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            double damage = Math.min(target.getHealth(), 8.0); // max 4 hearts
            target.damage(damage, p);
            target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
            // Draw beam particles
            Location start = p.getEyeLocation();
            Location end = target.getLocation().add(0, 1, 0);
            Vector step = end.toVector().subtract(start.toVector()).normalize().multiply(0.5);
            Location current = start.clone();
            for (int i = 0; i < 60; i++) {
                current.add(step);
                p.getWorld().spawnParticle(Particle.SONIC_BOOM, current, 1, 0, 0, 0, 0);
                if (current.distance(end) < 1) break;
            }
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.0f);
    }

    // SOUL — spawn random hostile mob targeting nearest enemy (30s CD)
    private void abilitySoul(Player p) {
        if (!manager.isSoulSpawnReady(p.getUniqueId())) {
            p.sendActionBar("§5Soul spawn: §7" + manager.soulSpawnCDRemaining(p.getUniqueId()) + "s");
            return;
        }
        manager.setSoulSpawnCD(p.getUniqueId());

        EntityType[] mobs = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                             EntityType.SPIDER, EntityType.WITCH, EntityType.BLAZE,
                             EntityType.CAVE_SPIDER, EntityType.ENDERMAN};
        EntityType mob = mobs[(int)(Math.random() * mobs.length)];
        Entity spawned = p.getWorld().spawnEntity(p.getLocation().add(1, 0, 1), mob);

        // Lock onto nearest enemy
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

    // VOID — dragon breath projectile (3 charges, 90s recharge)
    private void abilityVoid(Player p) {
        int charges = manager.getVoidCharges(p.getUniqueId());
        if (charges <= 0) {
            p.sendActionBar("§3Void: §7No charges! Recharging...");
            return;
        }
        manager.useVoidCharge(p.getUniqueId());
        DragonFireball ball = p.getWorld().spawn(p.getEyeLocation(), DragonFireball.class);
        ball.setDirection(p.getLocation().getDirection().multiply(2));
        ball.setShooter(p);
        p.sendActionBar("§3Dragon breath: §b" + (charges - 1) + "/3 charges");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);
    }

    // NATURE — vine grapple (30 block range)
    private void abilityNature(Player p) {
        RayTraceResult result = p.getWorld().rayTraceBlocks(
            p.getEyeLocation(), p.getLocation().getDirection(), 30
        );
        if (result == null || result.getHitBlock() == null) {
            p.sendActionBar("§aNo block in range!");
            return;
        }
        Block target = result.getHitBlock();
        Vector dir = target.getLocation().add(0.5, 0.5, 0.5).toVector()
            .subtract(p.getLocation().toVector()).normalize().multiply(2.5);
        dir.setY(Math.min(dir.getY() + 0.5, 1.5));
        p.setVelocity(dir);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_VINE_PLACE, 1.0f, 1.0f);
        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation(), 10, 0.5, 0.5, 0.5, 0);
    }

    // STORM — launch player 20 blocks upward
    private void abilityStorm(Player p) {
        p.getWorld().strikeLightningEffect(p.getLocation());
        p.setVelocity(new Vector(0, 3.5, 0)); // roughly 20 blocks up
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0)); // safe landing
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.5f);
    }

    // FROST — instant ice ray (freezes mobs/players for 3s)
    private void abilityFrost(Player p) {
        RayTraceResult result = p.getWorld().rayTraceEntities(
            p.getEyeLocation(), p.getLocation().getDirection(), 30,
            e -> e instanceof LivingEntity && e != p
        );

        // Draw ice beam particles
        Vector dir = p.getLocation().getDirection().normalize().multiply(0.5);
        Location cur = p.getEyeLocation().clone();
        for (int i = 0; i < 60; i++) {
            cur.add(dir);
            p.getWorld().spawnParticle(Particle.SNOWFLAKE, cur, 2, 0.1, 0.1, 0.1, 0);
            if (result != null && result.getHitEntity() != null
                && cur.distance(result.getHitEntity().getLocation()) < 1.5) break;
        }

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 127)); // freeze
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
            // Freeze velocity for 3s
            plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
                if (!target.isValid()) { task.cancel(); return; }
                target.setVelocity(new Vector(0, target.getVelocity().getY(), 0));
            }, 0L, 1L);
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                target.removePotionEffect(PotionEffectType.SLOWNESS), 60L);
        }
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_POWDER_SNOW_PLACE, 1.5f, 0.5f);
    }

    // SPEED — 3 dashes (equal to Lunge III)
    private void abilitySpeed(Player p) {
        int charges = manager.getSpeedDashes(p.getUniqueId());
        if (charges <= 0) {
            p.sendActionBar("§bNo dash charges! Recharging...");
            return;
        }
        manager.useSpeedDash(p.getUniqueId());
        Vector dir = p.getLocation().getDirection().normalize().multiply(2.5);
        dir.setY(0.4);
        p.setVelocity(dir);
        p.sendActionBar("§bDash! §7Charges: §3" + (charges - 1) + "/3");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.5f);
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
    }
}
