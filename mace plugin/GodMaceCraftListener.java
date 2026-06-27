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
        // Check if player is trying to craft the God Mace
        MaceType resultType = manager.getMaceType(event.getRecipe().getResult());
        if (resultType != MaceType.GOD) return;

        // Gather all mace types present in the crafting grid
        Set<MaceType> present = EnumSet.noneOf(MaceType.class);
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient == null) continue;
            MaceType type = manager.getMaceType(ingredient);
            if (type != null && type != MaceType.GOD) {
                present.add(type);
            }
        }

        // Must have exactly one of each of the 9 base maces
        Set<MaceType> required = EnumSet.of(
            MaceType.MAGMA, MaceType.TIDE, MaceType.SOUL, MaceType.VOID,
            MaceType.NATURE, MaceType.STORM, MaceType.FROST, MaceType.BLOOD, MaceType.SPEED
        );

        if (!present.containsAll(required)) {
            // Cancel the craft and tell the player what they're missing
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                Set<MaceType> missing = EnumSet.copyOf(required);
                missing.removeAll(present);
                StringBuilder msg = new StringBuilder("§cGod Mace requires all 9 custom maces! Missing: ");
                missing.forEach(m -> msg.append("§7").append(m.getDisplayName()).append("§c, "));
                player.sendMessage(msg.toString().replaceAll(", $", ""));
            }
        }
    }
}
