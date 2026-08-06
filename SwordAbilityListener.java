package com.customswords.listeners;

import com.customswords.CustomSwords;
import com.customswords.SwordType;
import com.customswords.managers.SwordManager;
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

public class SwordAbilityListener implements Listener {

    private final CustomSwords plugin;
    private final SwordManager manager;

    public SwordAbilityListener(CustomSwords plugin) {
        this.plugin = plugin;
        this.manager = plugin.getSwordManager();
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();

        // God sword uses shift+right-click for GUI — handled in GodSwordGUIListener
        if (p.isSneaking()) return;

        SwordType type = manager.getHeldSwordType(p);
        if (type == null || type == SwordType.GOD) return;

        switch (type) {
            case MAGMA  -> abilityMagma(p);
            case ECHO   -> abilityEcho(p);
            case SOUL   -> abilitySoul(p);
            case VOID   -> abilityVoid(p);
            case NATURE -> abilityNature(p);
            case STORM  -> abilityStorm(p);
            case FROST  -> abilityFrost(p);
            case BLOOD  -> abilityBlood(p);
        }
    }

    // ── MAGMA — 3 fireballs, 1s apart, 2 charges ─────────────────────────
    public void abilityMagma(Player p) {
        int charges = manager.getMagmaCharges(p.getUniqueId());
        if (charges <= 0) { p.sendActionBar("§cMagma: §7No charges! Recharging..."); return; }
        manager.useMagmaCharge(p.getUniqueId());
        p.sendActionBar("§c🔥 Firing 3 fireballs! §7Charges: §c" + (charges - 1) + "/2");

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline()) return;
                Fireball fb = p.getWorld().spawn(p.getEyeLocation(), Fireball.class);
                Vector dir = p.getLocation().getDirection().clone();
                if (idx == 1) dir.add(new Vector(0.05, 0, 0.05));
                if (idx == 2) dir.add(new Vector(-0.05, 0, -0.05));
                fb.setDirection(dir.multiply(2));
                fb.setShooter(p);
                fb.setYield(1.5f);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
            }, i * 20L);
        }
    }

    // ── ECHO — sonic beam, 4 hearts true damage, 60s CD ──────────────────
    public void abilityEcho(Player p) {
        if (!manager.isEchoBeamReady(p.getUniqueId())) {
            p.sendActionBar("§8Echo beam: §7" + manager.echoBeamCDRemaining(p.getUniqueId()) + "s");
            return;
        }
        manager.setEchoBeamCD(p.getUniqueId());
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        p.removePotionEffect(PotionEffectType.DARKNESS);

        RayTraceResult result = p.getWorld().rayTraceEntities(
            p.getEyeLocation(), p.getLocation().getDirection(), 30,
            e -> e instanceof LivingEntity && e != p
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (target.isValid()) target.damage(8.0, p); // 4 hearts true
            }, 1L);
            target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));

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

    // ── SOUL — Soul Drain AOE, 45s CD ─────────────────────────────────────
    public void abilitySoul(Player p) {
        if (!manager.isSoulDrainReady(p.getUniqueId())) {
            p.sendActionBar("§5Soul Drain: §7" + manager.soulDrainCDRemaining(p.getUniqueId()) + "s cooldown");
            return;
        }
        manager.setSoulDrainCD(p.getUniqueId());
        p.sendTitle("§5§l✦ SOUL DRAIN ✦", "§7Draining nearby enemies...", 5, 40, 10);

        new BukkitRunnable() {
            int ticks = 0;
            public void run() {
                if (ticks >= 5 || !p.isOnline()) { cancel(); return; }
                ticks++;
                double heal = 0;
                for (Entity e : p.getNearbyEntities(8, 4, 8)) {
                    if (!(e instanceof LivingEntity le) || e == p) continue;
                    le.damage(2.0, p);
                    heal += 2.0;
                    p.getWorld().spawnParticle(Particle.SOUL, le.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.05);
                }
                if (heal > 0) {
                    p.setHealth(Math.min(p.getHealth() + heal, p.getMaxHealth()));
                    p.sendActionBar("§5Soul Drain: §7+" + (int)(heal / 2) + " hearts!");
                }
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_HURT, 0.8f, 1.5f);
            }
        }.runTaskTimer(plugin, 0L, 20L);

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        p.getWorld().spawnParticle(Particle.SOUL, p.getLocation(), 50, 2, 1, 2, 0.05);
    }

    // ── VOID — dragon breath, 2 hearts true + 0.5/tick, 2 charges in overworld / 3 in End ──
    public void abilityVoid(Player p) {
        boolean inEnd = p.getWorld().getEnvironment() == World.Environment.THE_END;
        int maxCharges = inEnd ? 3 : 2;
        int charges = Math.min(manager.getVoidCharges(p.getUniqueId()), maxCharges);
        if (charges <= 0) {
            p.sendActionBar("§3Void: §7No charges!" + (inEnd ? "" : " §8(Max 2 in Overworld)"));
            return;
        }
        manager.useVoidCharge(p.getUniqueId());

        DragonFireball ball = p.getWorld().spawn(p.getEyeLocation(), DragonFireball.class);
        ball.setDirection(p.getLocation().getDirection().multiply(2));
        ball.setShooter(p);

        // Per-tick true damage from the lingering pool
        new BukkitRunnable() {
            int elapsed = 0;
            public void run() {
                if (elapsed >= 60 || (!ball.isValid() && elapsed > 5)) { cancel(); return; }
                elapsed++;
                Location poolLoc = ball.isValid() ? ball.getLocation() : p.getLocation();
                for (Entity e : p.getWorld().getNearbyEntities(poolLoc, 2.5, 1.5, 2.5)) {
                    if (!(e instanceof LivingEntity le) || e == p) continue;
                    le.damage(1.0, p); // 0.5 hearts per tick
                }
            }
        }.runTaskTimer(plugin, 5L, 1L);

        // Initial 2 hearts true on impact
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            RayTraceResult res = p.getWorld().rayTraceEntities(
                p.getEyeLocation(), p.getLocation().getDirection(), 30,
                e -> e instanceof LivingEntity && e != p
            );
            if (res != null && res.getHitEntity() instanceof LivingEntity hit)
                hit.damage(4.0, p);
        }, 3L);

        p.sendActionBar("§3Dragon breath: §b" + (charges - 1) + "/" + maxCharges + " charges");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);
    }

    // ── NATURE — grapple (pulls players/mobs toward you, or latches onto wind charges/pearls) ──
    public void abilityNature(Player p) {
        if (!manager.isNatureGrappleReady(p.getUniqueId())) {
            p.sendActionBar("§aGrapple: §7" + manager.natureGrappleCDRemaining(p.getUniqueId()) + "s cooldown");
            return;
        }

        // Check for wind charges / ender pearls in flight
        Entity projectileTarget = null;
        double closestDot = 0.3;
        Vector look = p.getLocation().getDirection();
        for (Entity e : p.getNearbyEntities(30, 30, 30)) {
            if (e instanceof WindCharge || e instanceof EnderPearl) {
                Vector toE = e.getLocation().toVector().subtract(p.getEyeLocation().toVector()).normalize();
                double dot = toE.dot(look);
                if (dot > closestDot) { closestDot = dot; projectileTarget = e; }
            }
        }

        if (projectileTarget != null) {
            Vector dir = projectileTarget.getLocation().toVector()
                .subtract(p.getLocation().toVector()).normalize().multiply(2.8);
            dir.setY(Math.min(dir.getY() + 0.4, 1.5));
            p.setVelocity(dir);
            manager.setNatureGrappleCD(p.getUniqueId());
            p.sendActionBar("§aGrappled to §f" + projectileTarget.getType().name().replace("_", " ") + "§a!");
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_VINE_PLACE, 1.0f, 1.2f);
            return;
        }

        // Pull living entity toward attacker
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
        RayTraceResult blockResult = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getLocation().getDirection(), 30);
        if (blockResult == null || blockResult.getHitBlock() == null) {
            p.sendActionBar("§aNo target in range!"); return;
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

    // ── STORM — launch upward, 25s CD ────────────────────────────────────
    public void abilityStorm(Player p) {
        if (!manager.isStormAbilityReady(p.getUniqueId())) {
            p.sendActionBar("§eStorm: §7" + manager.stormAbilityCDRemaining(p.getUniqueId()) + "s cooldown");
            return;
        }
        manager.setStormAbilityCD(p.getUniqueId());
        p.getWorld().strikeLightningEffect(p.getLocation());
        p.setVelocity(new Vector(0, 3.5, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.5f);
        p.sendActionBar("§e⚡ Storm Launch! §8(" + manager.stormAbilityCDRemaining(p.getUniqueId()) + "s CD)");
    }

    // ── FROST — ice beam freeze, 15s CD ──────────────────────────────────
    public void abilityFrost(Player p) {
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

    // ── BLOOD — activate berserk, 30s CD ─────────────────────────────────
    public void abilityBlood(Player p) {
        if (!manager.isBloodBerserkReady(p.getUniqueId())) {
            p.sendActionBar("§4Berserk: §7" + manager.bloodBerserkCDRemaining(p.getUniqueId()) + "s cooldown");
            return;
        }
        manager.activateBloodBerserk(p.getUniqueId());
        p.sendTitle("§4§l⚔ BERSERK ⚔", "§710s — 2x damage + autocrit", 5, 30, 10);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) p.sendActionBar("§7Berserk ended.");
        }, 200L);
    }
}
