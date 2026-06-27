package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;

public class GodMaceCraftListener implements Listener {
    private final CustomMaces plugin;

    public GodMaceCraftListener(CustomMaces plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getInventory().getResult() != null) return;

        List<MaceType> found = new ArrayList<>();
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item == null) continue;
            MaceType type = MaceType.fromItemStack(item, plugin);
            if (type != null && type != MaceType.GOD && !found.contains(type)) {
                found.add(type);
            }
        }

        MaceType[] required = MaceType.getBaseMaces();
        boolean allPresent = true;
        for (MaceType req : required) {
            if (!found.contains(req)) { allPresent = false; break; }
        }

        if (allPresent && found.size() == 9) {
            event.getInventory().setResult(MaceType.GOD.createItemStack(plugin));
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null) return;
        MaceType type = MaceType.fromItemStack(result, plugin);
        if (type != MaceType.GOD) return;

        List<String> missing = new ArrayList<>();
        List<MaceType> found = new ArrayList<>();
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item == null) continue;
            MaceType itemType = MaceType.fromItemStack(item, plugin);
            if (itemType != null && itemType != MaceType.GOD && !found.contains(itemType)) {
                found.add(itemType);
            }
        }
        for (MaceType req : MaceType.getBaseMaces()) {
            if (!found.contains(req)) missing.add(req.getDisplayName());
        }
        if (!missing.isEmpty()) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof org.bukkit.entity.Player player) {
                player.sendMessage(Component.text("Missing: " + String.join(", ", missing), TextColor.color(0xFF5555)));
            }
        }
    }
}
