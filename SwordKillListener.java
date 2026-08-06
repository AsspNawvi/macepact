package com.customswords.listeners;

import com.customswords.CustomSwords;
import com.customswords.SwordType;
import com.customswords.managers.SwordManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class SwordKillListener implements Listener {

    private final SwordManager manager;

    public SwordKillListener(CustomSwords plugin) {
        this.manager = plugin.getSwordManager();
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player killer)) return;
        SwordType type = manager.getHeldSwordType(killer);
        if (type != SwordType.BLOOD && type != SwordType.GOD) return;
        manager.incrementBloodStreak(killer.getUniqueId());
    }
}
