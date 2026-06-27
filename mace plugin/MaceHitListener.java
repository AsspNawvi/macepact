package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import com.custommaces.managers.MaceManager;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

        MaceType type = manager.getHeldMaceType(attacker);
        if (type == null) return;

        boolean isGod = (type == MaceType.GOD);

        if (type == MaceType.MAGMA || isGod) handleMagmaHit(attacker, target);
        if (type == MaceType.SOUL || isGod) handleSoulHit(attacker, event);
        if (type == MaceType.VOID || isGod) handleVoidHit(attacker, target);
        if (type == MaceType.STORM || isGod) handleStormHit(attacker, target, event);
        if (type == MaceType.FROST || isGod) handleFrostHit(target);
        if (type == MaceType.BLOOD || isGod) handleBloodHit(attacker, event);
    }

    private void handleMagmaHit(Player attacker, LivingEntity target) {
        target.setFireTicks(60);
        for (Entity nearby : attacker.getNearbyEntities(3, 3, 3)) {
            if (nearby instanceof LivingEntity le && nearby != attacker) le.setFireTicks(40);
        }
    }

    private void handleSoulHit(Player attacker, EntityDamageByEntityEvent event) {
        double heal = event.getFinalDamage() * 0.3;
        double newHealth = Math.min(attacker.getHealth() + heal, attacker.getMaxHealth());
        attacker.setHealth(newHealth);
        attacker.sendActionBar("§5+§d" + String.format("%.1f", heal) + " §5soul steal");
    }

    private void handleVoidHit(Player attacker, LivingEntity target) {
        int hits = manager.getAndIncrementHitCounter(attacker.getUniqueId());
        if (hits >= 4) {
            manager.resetHitCounter(attacker.getUniqueId());
            if (target instanceof Player targetPlayer) {
                Location loc = target.getLocation();
                double offsetX = (random.nextDouble() - 0.5) * 10;
                double offsetZ = (random.nextDouble() - 0.5) * 10;
                Location newLoc = loc.clone().add(offsetX, 0, offsetZ);
                newLoc.setY(Math.max(newLoc.getY(), -60));
                targetPlayer.teleport(newLoc);
                targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                attacker.sendActionBar("§8Void displaced your target!");
            }
        }
    }

    private void handleStormHit(Player attacker, LivingEntity target, EntityDamageByEntityEvent event) {
        if (random.nextFloat() < 0.30f) {
            List<Entity> nearby = attacker.getNearbyEntities(6, 6, 6).stream()
                .filter(e -> e instanceof LivingEntity && e != target && e != attacker)
                .toList();
            if (!nearby.isEmpty()) {
                LivingEntity chainTarget = (LivingEntity) nearby.get(random.nextInt(nearby.size()));
                attacker.getWorld().strikeLightningEffect(chainTarget.getLocation());
                chainTarget.damage(4.0, attacker);
                attacker.sendActionBar("§eChain lightning!");
            }
        }
    }

    private void handleFrostHit(LivingEntity target) {
        PotionEffect current = target.getPotionEffect(PotionEffectType.SLOWNESS);
        int currentLevel = (current != null) ? current.getAmplifier() : -1;
        int newLevel = Math.min(currentLevel + 1, 2);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, newLevel, false, true, true));
    }

    private void handleBloodHit(Player attacker, EntityDamageByEntityEvent event) {
        if (manager.isInBerserk(attacker.getUniqueId())) {
            event.setDamage(event.getDamage() * 2.0);
            attacker.sendActionBar("§4§l⚔ BERSERK ⚔");
        }
    }
}
