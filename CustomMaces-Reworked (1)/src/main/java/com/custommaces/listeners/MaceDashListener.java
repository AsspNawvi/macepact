package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class MaceDashListener implements Listener {
    private final CustomMaces plugin;

    public MaceDashListener(CustomMaces plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (plugin.getMaceManager().isHoldingShield(player)) return;

        boolean hasSpeed = false;
        for (ItemStack item : player.getInventory().getContents()) {
            MaceType type = MaceType.fromItemStack(item, plugin);
            if (type == MaceType.SPEED || type == MaceType.GOD) {
                hasSpeed = true;
                break;
            }
        }
        if (!hasSpeed) return;

        int charges = plugin.getMaceManager().getDashCharges(player);
        if (charges <= 0) {
            player.sendActionBar(Component.text("No dash charges!", TextColor.color(0xFF5555)));
            return;
        }

        plugin.getMaceManager().useDashCharge(player);
        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(direction.multiply(3.0).add(new Vector(0, 0.3, 0)));
        player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 20, 0.3, 0.3, 0.3, 0.1);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.5f);

        int remaining = plugin.getMaceManager().getDashCharges(player);
        player.sendActionBar(Component.text("Dash! (" + remaining + "/3 charges)", TextColor.color(0x00FF7F)));
    }
}
