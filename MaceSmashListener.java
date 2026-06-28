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

        // God Mace: use the selected smash ability (or all if none chosen)
        if (god) {
            MaceType godSmash = manager.getGodSmashAbility(attacker.getUniqueId());
            if (godSmash != null) {
                executeSmash(godSmash, attacker, target, loc);
                attacker.sendTitle("§6§l✦ " + godSmash.getDisplayName().toUpperCase() + " SMASH ✦", "", 5, 30, 10);
            } else {
                // No ability chosen: use all
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
            default -> {}
        }
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

    // ECHO smash — does NOT apply blindness to the attacker (warden immunity)
    private void smashEcho(Player attacker, LivingEntity target, Location loc) {
        World w = loc.getWorld();
        for (Entity e : attacker.getNearbyEntities(6, 4, 6))
            if (e instanceof LivingEntity le && e != attacker) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            }
        // Strip any blindness the attacker got (e.g. warden retaliation)
        attacker.removePotionEffect(PotionEffectType.BLINDNESS);
        attacker.removePotionEffect(PotionEffectType.DARKNESS);
        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.0f);
        w.spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
    }

    // SOUL smash — give target 3 hearts, spawn mob
    private void smashSoul(Player attacker, LivingEntity target, Location loc) {
        World w = loc.getWorld();
        EntityType[] mobs = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                             EntityType.SPIDER, EntityType.WITCH, EntityType.BLAZE,
                             EntityType.CAVE_SPIDER, EntityType.ENDERMAN};
        EntityType mob = mobs[(int)(Math.random() * mobs.length)];
        w.spawnEntity(loc, mob);

        // Give target 3 hearts
        if (target instanceof Player tp) {
            double newHealth = Math.min(tp.getHealth() + 6.0, tp.getMaxHealth());
            tp.setHealth(newHealth);
            attacker.sendActionBar("§5Soul Smash: §7Opponent healed 3 hearts!");
        }

        // Pull nearby entities
        for (Entity e : attacker.getNearbyEntities(6, 4, 6))
            if (e instanceof LivingEntity le && e != attacker) {
                Vector pull = loc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(2.0);
                pull.setY(0.3);
                le.setVelocity(pull);
            }
        w.playSound(loc, Sound.ENTITY_WITHER_HURT, 1.0f, 1.5f);
        w.spawnParticle(Particle.SOUL, loc, 50, 2, 1, 2, 0.05);
    }

    // VOID smash — only works in the End; +20 hearts to attacker instead of capping at 20
    private void smashVoid(Player attacker, LivingEntity target, Location loc) {
        World w = loc.getWorld();
        boolean inEnd = w.getEnvironment() == World.Environment.THE_END;

        if (!inEnd) {
            attacker.sendActionBar("§3Void Smash: §7Only available in The End!");
            return;
        }

        // Give attacker +20 hearts (additive to current max)
        double newMaxHealth = attacker.getMaxHealth() + 40.0; // +20 hearts = +40 HP
        attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(newMaxHealth);
        attacker.setHealth(Math.min(attacker.getHealth() + 40.0, newMaxHealth));
        attacker.sendActionBar("§3Void Smash: §b+20 hearts!");

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
        if (count != 3) return;
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
