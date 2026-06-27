package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class MaceHitListener implements Listener {
    private final CustomMaces plugin;
    private final Map<UUID, Integer> frostStacks = new HashMap<>();
    private final Map<UUID, Integer> voidHitCounters = new HashMap<>();

    public MaceHitListener(CustomMaces plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (plugin.getMaceManager().isHoldingShield(attacker)) return;

        MaceType mace = MaceType.fromItemStack(attacker.getInventory().getItemInMainHand(), plugin);
        if (mace == null) return;

        int combo = plugin.getMaceManager().incrementHitCombo(attacker);
        boolean isThirdHit = combo >= 3;

        if (isThirdHit) {
            plugin.getMaceManager().resetHitCombo(attacker);
            // Trigger smash via MaceSmashListener
            MaceSmashListener.triggerSmash(attacker, target, mace, plugin);
        }

        // On-hit effects
        applyOnHit(attacker, target, event, mace, isThirdHit);

        // God Mace on-hit: all on-hit effects
        if (mace == MaceType.GOD) {
            for (MaceType base : MaceType.getBaseMaces()) {
                applyOnHit(attacker, target, event, base, isThirdHit);
            }
        }
    }

    private void applyOnHit(Player attacker, LivingEntity target, EntityDamageByEntityEvent event, MaceType mace, boolean isThirdHit) {
        UUID tid = target.getUniqueId();

        switch (mace) {
            case MAGMA -> {
                // Explosion on every hit
                target.getWorld().createExplosion(target.getLocation(), 1.0f, false, false);
                target.setFireTicks(100);
                target.getWorld().spawnParticle(Particle.FLAME, target.getLocation(), 10, 0.5, 0.5, 0.5, 0.05);
            }
            case ECHO -> {
                if (isThirdHit && !plugin.getMaceManager().isEchoBlindnessOnCooldown(attacker)) {
                    plugin.getMaceManager().setEchoBlindnessCooldown(attacker);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, true, true));
                    target.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation(), 5, 0.3, 0.5, 0.3, 0);
                }
            }
            case SOUL -> {
                if (isThirdHit && !plugin.getMaceManager().isSoulHeartOnCooldown(attacker)) {
                    plugin.getMaceManager().setSoulHeartCooldown(attacker);
                    target.damage(6.0, attacker); // 3 hearts
                    target.getWorld().spawnParticle(Particle.SOUL, target.getLocation(), 20, 0.5, 0.5, 0.5, 0.05);
                    target.playSound(target.getLocation(), Sound.ENTITY_WITCH_DRINK, 1.0f, 0.8f);
                }
            }
            case ENDER_VOID -> {
                // Vortex pull on every hit
                Vector pull = attacker.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.4);
                target.setVelocity(target.getVelocity().add(pull));
                target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 10, 0.3, 0.5, 0.3, 0.05);
            }
            case NATURE -> {
                // +1 Absorption heart every hit
                target.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 0, false, true, true));
                target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation(), 8, 0.3, 0.5, 0.3, 0.1);
            }
            case STORM -> {
                if (isThirdHit) {
                    target.getWorld().strikeLightning(target.getLocation());
                    target.damage(4.0, attacker);
                    target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation(), 20, 0.5, 0.5, 0.5, 0.2);
                }
            }
            case FROST -> {
                if (isThirdHit) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1, false, true, true));
                    target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation(), 15, 0.3, 0.5, 0.3, 0.01);
                }
            }
            case BLOOD -> {
                if (isThirdHit && !plugin.getMaceManager().isBloodBerserkOnCooldown(attacker)) {
                    plugin.getMaceManager().setBloodBerserkCooldown(attacker);
                    plugin.getMaceManager().setBerserk(attacker, true);
                    attacker.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 2, false, true, true));
                    attacker.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 200, 2, false, true, true));
                    attacker.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, attacker.getLocation(), 30, 1, 1, 1, 0.2);
                    // Berserk expires after 10s
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        plugin.getMaceManager().setBerserk(attacker, false);
                    }, 200L);
                }
            }
            case SPEED -> {
                if (isThirdHit && !plugin.getMaceManager().isSpeedBoostOnCooldown(attacker)) {
                    plugin.getMaceManager().setSpeedBoostCooldown(attacker);
                    attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 4, false, true, true)); // Speed V
                    attacker.getWorld().spawnParticle(Particle.CLOUD, attacker.getLocation(), 30, 0.5, 0.5, 0.5, 0.2);
                }
            }
            case GOD -> {
                // Handled by looping base maces above
            }
        }

        // Autocrit during berserk (Blood Mace / God Mace)
        if (plugin.getMaceManager().isBerserk(attacker)) {
            event.setDamage(event.getDamage() * 1.5);
        }
    }
}
