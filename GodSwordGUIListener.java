package com.customswords.listeners;

import com.customswords.CustomSwords;
import com.customswords.SwordType;
import com.customswords.managers.SwordManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class GodSwordGUIListener implements Listener {

    private final CustomSwords plugin;
    private final SwordManager manager;
    private final SwordAbilityListener abilityListener;
    private final Map<UUID, Inventory> openGUIs = new HashMap<>();
    private final Map<UUID, String>    guiPage  = new HashMap<>();

    public GodSwordGUIListener(CustomSwords plugin) {
        this.plugin = plugin;
        this.manager = plugin.getSwordManager();
        this.abilityListener = new SwordAbilityListener(plugin);
    }

    @EventHandler
    public void onShiftRightClick(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        SwordType type = manager.getHeldSwordType(p);
        if (type != SwordType.GOD) return;

        event.setCancelled(true);
        openMainGUI(p);
    }

    private void openMainGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 9, "§6§l✦ God Sword Menu ✦");
        SwordType chosen = manager.getGodAbilityChoice(p.getUniqueId());
        String label = chosen == null ? "ALL" : chosen.getDisplayName();
        gui.setItem(2, makeItem(Material.NETHERITE_SWORD, "§b§lUSE ABILITY", "§7Trigger the selected ability\n§8Current: §f" + label));
        gui.setItem(6, makeItem(Material.NETHER_STAR,     "§e§lCHOOSE ABILITY", "§7Pick which ability to use\n§8Current: §f" + label));
        openGUIs.put(p.getUniqueId(), gui);
        guiPage.put(p.getUniqueId(), "main");
        p.openInventory(gui);
    }

    private void openChooseGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 9, "§6§l✦ Choose Ability ✦");
        gui.setItem(0, makeItem(Material.BLAZE_POWDER,   "§c§lMAGMA",  "§73 fireballs (2 charges)"));
        gui.setItem(1, makeItem(Material.SCULK_SHRIEKER, "§8§lECHO",   "§7Sonic beam (60s CD)"));
        gui.setItem(2, makeItem(Material.SOUL_LANTERN,   "§5§lSOUL",   "§7Soul Drain AOE (45s CD)"));
        gui.setItem(3, makeItem(Material.DRAGON_BREATH,  "§3§lVOID",   "§7Dragon breath (" + manager.getVoidCharges(p.getUniqueId()) + " charges)"));
        gui.setItem(4, makeItem(Material.VINE,           "§a§lNATURE", "§7Grapple (10s CD)"));
        gui.setItem(5, makeItem(Material.LIGHTNING_ROD,  "§e§lSTORM",  "§7Launch upward (25s CD)"));
        gui.setItem(6, makeItem(Material.BLUE_ICE,       "§f§lFROST",  "§7Ice beam (15s CD)"));
        gui.setItem(7, makeItem(Material.NETHER_STAR,    "§4§lBLOOD",  "§7Berserk (30s CD)"));
        gui.setItem(8, makeItem(Material.FEATHER,        "§7§lALL",    "§7Use all abilities at once"));
        openGUIs.put(p.getUniqueId(), gui);
        guiPage.put(p.getUniqueId(), "choose");
        p.openInventory(gui);
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        Inventory gui = openGUIs.get(p.getUniqueId());
        if (gui == null || !event.getInventory().equals(gui)) return;

        event.setCancelled(true);
        int slot = event.getSlot();
        String page = guiPage.getOrDefault(p.getUniqueId(), "main");

        p.closeInventory();
        openGUIs.remove(p.getUniqueId());
        guiPage.remove(p.getUniqueId());

        if (page.equals("main")) {
            if (slot == 2) triggerGodAbility(p);
            else if (slot == 6) openChooseGUI(p);
        } else if (page.equals("choose")) {
            SwordType[] types = {SwordType.MAGMA, SwordType.ECHO, SwordType.SOUL, SwordType.VOID,
                                 SwordType.NATURE, SwordType.STORM, SwordType.FROST, SwordType.BLOOD, null};
            if (slot >= 0 && slot < types.length) {
                manager.setGodAbilityChoice(p.getUniqueId(), types[slot]);
                String label = types[slot] == null ? "ALL" : types[slot].getDisplayName();
                p.sendActionBar("§6God Sword: §fAbility set to §e" + label);
            }
        }
    }

    private void triggerGodAbility(Player p) {
        SwordType chosen = manager.getGodAbilityChoice(p.getUniqueId());
        if (chosen == null) {
            // Use all
            abilityListener.abilityMagma(p);
            abilityListener.abilityEcho(p);
            abilityListener.abilitySoul(p);
            abilityListener.abilityVoid(p);
            abilityListener.abilityNature(p);
            abilityListener.abilityStorm(p);
            abilityListener.abilityFrost(p);
            abilityListener.abilityBlood(p);
            p.sendTitle("§6§l✦ GOD ABILITY ✦", "", 5, 25, 10);
        } else {
            switch (chosen) {
                case MAGMA  -> abilityListener.abilityMagma(p);
                case ECHO   -> abilityListener.abilityEcho(p);
                case SOUL   -> abilityListener.abilitySoul(p);
                case VOID   -> abilityListener.abilityVoid(p);
                case NATURE -> abilityListener.abilityNature(p);
                case STORM  -> abilityListener.abilityStorm(p);
                case FROST  -> abilityListener.abilityFrost(p);
                case BLOOD  -> abilityListener.abilityBlood(p);
                default -> {}
            }
            p.sendTitle("§6§l✦ " + chosen.getDisplayName().toUpperCase() + " ✦", "", 5, 25, 10);
        }
    }

    private ItemStack makeItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore.split("\n")));
        item.setItemMeta(meta);
        return item;
    }
}
