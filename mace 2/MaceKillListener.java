package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import com.custommaces.managers.MaceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class MaceKillListener implements Listener {

    private final MaceManager manager;

    public MaceKillListener(CustomMaces plugin) {
        this.manager = plugin.getMaceManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Reset victim's streak
        manager.resetBloodStreak(victim.getUniqueId());

        if (killer == null) return;
        MaceType type = manager.getHeldMaceType(killer);
        if (type != MaceType.BLOOD && type != MaceType.GOD) return;

        manager.incrementBloodStreak(killer.getUniqueId());
        int streak = manager.getBloodStreak(killer.getUniqueId());
        String msg = switch (streak) {
            case 1 -> "§cKill streak: 1 — §7Strength I";
            case 2 -> "§cKill streak: 2 — §cStrength II";
            default -> "§4Kill streak: " + streak + " — §4Strength III";
        };
        killer.sendActionBar(msg);
    }
}
