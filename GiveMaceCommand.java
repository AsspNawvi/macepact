package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class MaceInventoryListener implements Listener {
    private final CustomMaces plugin;

    public MaceInventoryListener(CustomMaces plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getMaceManager().applyInventoryPassives(player);
            }, 1L);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getMaceManager().applyInventoryPassives(player);
        }, 1L);
    }

    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (MaceType.isAnyMace(event.getItem().getItemStack(), plugin)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getMaceManager().applyInventoryPassives(player);
            }, 1L);
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        plugin.getMaceManager().applyInventoryPassives(event.getPlayer());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getMaceManager().isHoldingShield(player)) return;

        for (ItemStack item : player.getInventory().getContents()) {
            MaceType type = MaceType.fromItemStack(item, plugin);
            if (type == MaceType.MAGMA || type == MaceType.GOD) {
                for (org.bukkit.entity.Entity entity : player.getNearbyEntities(3, 3, 3)) {
                    if (entity != player && entity instanceof org.bukkit.entity.LivingEntity living) {
                        living.setFireTicks(60);
                    }
                }
                break;
            }
        }
    }
}
