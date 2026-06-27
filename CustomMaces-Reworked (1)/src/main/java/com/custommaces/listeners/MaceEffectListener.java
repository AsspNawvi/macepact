package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class MaceEffectListener implements Listener {
    private final CustomMaces plugin;

    public MaceEffectListener(CustomMaces plugin) {
        this.plugin = plugin;
    }

    private boolean hasEchoMace(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            MaceType type = MaceType.fromItemStack(item, plugin);
            if (type == MaceType.ECHO || type == MaceType.GOD) return true;
        }
        return false;
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player player && event.getEntity().getType() == EntityType.WARDEN) {
            if (hasEchoMace(player)) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasEchoMace(player)) return;
        if (event.getNewEffect() == null) return;
        PotionEffectType type = event.getNewEffect().getType();
        if (type == PotionEffectType.BLINDNESS || type == PotionEffectType.DARKNESS) {
            event.setCancelled(true);
        }
    }
}
