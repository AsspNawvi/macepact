package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import com.custommaces.managers.MaceManager;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

public class MaceDashListener implements Listener {

    private final MaceManager manager;

    public MaceDashListener(CustomMaces plugin) {
        this.manager = plugin.getMaceManager();
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        MaceType type = manager.getHeldMaceType(player);
        if (type != MaceType.SPEED && type != MaceType.GOD) return;

        int charges = manager.getDashCharges(player.getUniqueId());
        if (charges <= 0) {
            player.sendActionBar("§7No dash charges! Recharging...");
            return;
        }

        if (manager.useDashCharge(player.getUniqueId())) {
            performDash(player);
            int remaining = manager.getDashCharges(player.getUniqueId());
            player.sendActionBar("§3Dash! §7Charges: §b" + remaining + "/2");
        }
    }

    private void performDash(Player player) {
        Vector direction = player.getLocation().getDirection().normalize();
        direction.multiply(1.8);
        direction.setY(0.3);
        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
    }
}
