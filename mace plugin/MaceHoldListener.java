package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import com.custommaces.managers.MaceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class MaceHoldListener implements Listener {

    private final CustomMaces plugin;
    private final MaceManager manager;

    public MaceHoldListener(CustomMaces plugin) {
        this.plugin = plugin;
        this.manager = plugin.getMaceManager();
        startPassiveLoop();
    }

    private void startPassiveLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    MaceType type = manager.getHeldMaceType(player);
                    if (type == null) continue;
                    applyPassives(player, type);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void applyPassives(Player player, MaceType type) {
        int duration = 60;
        boolean isGod = (type == MaceType.GOD);

        if (type == MaceType.MAGMA || isGod)
            applyEffect(player, PotionEffectType.FIRE_RESISTANCE, duration, 0);

        if (type == MaceType.TIDE || isGod) {
            applyEffect(player, PotionEffectType.DOLPHINS_GRACE, duration, 0);
            applyEffect(player, PotionEffectType.WATER_BREATHING, duration, 0);
        }

        if (type == MaceType.SOUL || isGod)
            applyEffect(player, PotionEffectType.HEALTH_BOOST, duration, 3);

        if (type == MaceType.NATURE || isGod) {
            applyEffect(player, PotionEffectType.REGENERATION, duration, 1);
            applyEffect(player, PotionEffectType.SATURATION, duration, 0);
        }

        if (type == MaceType.SPEED || isGod)
            applyEffect(player, PotionEffectType.SPEED, duration, isGod ? 2 : 1); // God gets Speed III

        if (type == MaceType.BLOOD || isGod) {
            int streak = manager.getKillStreak(player.getUniqueId());
            if (streak >= 3) applyEffect(player, PotionEffectType.STRENGTH, duration, isGod ? 3 : 2);
            else if (streak == 2) applyEffect(player, PotionEffectType.STRENGTH, duration, isGod ? 2 : 1);
            else if (streak >= 1) applyEffect(player, PotionEffectType.STRENGTH, duration, isGod ? 1 : 0);
            else if (isGod) applyEffect(player, PotionEffectType.STRENGTH, duration, 0); // base Strength I always on God
            if (manager.isInBerserk(player.getUniqueId()))
                applyEffect(player, PotionEffectType.HASTE, duration, 3);
        }
    }

    private void applyEffect(Player player, PotionEffectType type, int duration, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, false));
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        MaceType oldType = manager.getMaceType(player.getInventory().getItem(event.getPreviousSlot()));
        if (oldType == null) return;

        // Remove passives from old mace (covers both individual and God)
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.SATURATION);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.HASTE);
    }
}
