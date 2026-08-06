package com.customswords.listeners;

import com.customswords.CustomSwords;
import com.customswords.SwordType;
import com.customswords.managers.SwordManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

public class GodSwordCraftListener implements Listener {

    private final SwordManager manager;

    public GodSwordCraftListener(CustomSwords plugin) {
        this.manager = plugin.getSwordManager();
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (manager.getSwordType(result) != SwordType.GOD) return;

        // Verify all 9 ingredients are custom swords (not plain netherite swords)
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient == null) continue;
            if (manager.getSwordType(ingredient) == null) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage("§cYou need all 9 custom swords to craft the God Sword!");
                return;
            }
        }
    }
}
