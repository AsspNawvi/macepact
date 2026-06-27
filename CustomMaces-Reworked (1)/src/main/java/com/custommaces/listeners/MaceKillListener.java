package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class MaceKillListener implements Listener {
    private final CustomMaces plugin;

    public MaceKillListener(CustomMaces plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player killer = event.getEntity().getKiller();
        if (plugin.getMaceManager().isHoldingShield(killer)) return;

        boolean hasBlood = false;
        for (ItemStack item : killer.getInventory().getContents()) {
            MaceType type = MaceType.fromItemStack(item, plugin);
            if (type == MaceType.BLOOD || type == MaceType.GOD) {
                hasBlood = true;
                break;
            }
        }

        if (hasBlood && event.getEntity() instanceof Player) {
            plugin.getMaceManager().addKill(killer);
            int streak = plugin.getMaceManager().getKillStreak(killer);
            if (streak >= 3) {
                killer.sendActionBar(net.kyori.adventure.text.Component.text("🔥 Kill Streak: " + streak + "! Strength III!", net.kyori.adventure.text.format.TextColor.color(0xDC143C)));
            }
        }
    }
}
