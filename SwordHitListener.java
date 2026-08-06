package com.customswords.listeners;

import com.customswords.CustomSwords;
import com.customswords.SwordType;
import com.customswords.managers.SwordManager;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class SwordHitListener implements Listener {

    private final CustomSwords plugin;
    private final SwordManager manager;

    public SwordHitListener(CustomSwords plugin) {
        this.plugin = plugin;
        this.manager = plugin.getSwordManager();
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Shield check
        if (target instanceof Player tp) {
            ItemStack off  = tp.getInventory().getItemInOffHand();
            ItemStack main = tp.getInventory().getItemInMainHand();
            if (off.getType() == Material.SHIELD || main.getType() == Material.SHIELD) return;
        }

        SwordType type = manager.getHeldSwordType(attacker);
        if (type == null) return;

        boolean god = (type == SwordType.GOD);

        // BLOOD / GOD — always autocrit (1.5x), berserk adds 2x on top
        if (type == SwordType.BLOOD || god) {
            event.setDamage(event.getDamage() * 1.5);
            if (manager.isBloodBerserkActive(attacker.getUniqueId()))
                event.setDamage(event.getDamage() * 2.0);
        }

        // MAGMA / GOD — set target on fire
        if (type == SwordType.MAGMA || god)
            target.setFireTicks(80);

        // ECHO / GOD — 4 hearts true damage on every hit (scheduled to bypass armor)
        if (type == SwordType.ECHO || god) {
            attacker.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
            attacker.removePotionEffect(org.bukkit.potion.PotionEffectType.DARKNESS);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (target.isValid()) target.damage(8.0, attacker); // 4 hearts true
            }, 1L);
        }
    }
}
