package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import com.custommaces.managers.MaceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public class GodMaceCraftListener implements Listener {

    private final MaceManager manager;

    public GodMaceCraftListener(CustomMaces plugin) {
        this.manager = plugin.getMaceManager();
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        MaceType result = manager.getMaceType(event.getRecipe().getResult());
        if (result != MaceType.GOD) return;

        Set<MaceType> present = EnumSet.noneOf(MaceType.class);
        for (ItemStack ing : event.getInventory().getMatrix()) {
            if (ing == null) continue;
            MaceType t = manager.getMaceType(ing);
            if (t != null && t != MaceType.GOD) present.add(t);
        }

        Set<MaceType> required = EnumSet.of(
            MaceType.MAGMA, MaceType.ECHO, MaceType.SOUL, MaceType.VOID,
            MaceType.NATURE, MaceType.STORM, MaceType.FROST, MaceType.BLOOD, MaceType.SPEED
        );

        if (!present.containsAll(required)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player p) {
                Set<MaceType> missing = EnumSet.copyOf(required);
                missing.removeAll(present);
                StringBuilder msg = new StringBuilder("§cNeed all 9 maces! Missing: ");
                missing.forEach(m -> msg.append("§7").append(m.getDisplayName()).append("§c, "));
                p.sendMessage(msg.toString().replaceAll(", $", ""));
            }
        }
    }
}
