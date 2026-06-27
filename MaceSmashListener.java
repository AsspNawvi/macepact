package com.custommaces;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import java.util.Arrays;
import java.util.List;

public enum MaceType {
    MAGMA("Magma Mace", Material.BLAZE_ROD, TextColor.color(0xFF4500), 
          "Fire Resistance + burn aura", "Launches 2 fireballs", "Explosion on every hit"),
    ECHO("Echo Mace", Material.ECHO_SHARD, TextColor.color(0x4B0082),
         "Warden & blindness/darkness immunity", "Warden beam (1m cooldown, 4❤)", "Blindness 2s every 3 hits (20s cd)"),
    SOUL("Soul Mace", Material.WITHER_SKELETON_SKULL, TextColor.color(0x8B0000),
         "+5 bonus hearts", "Spawn random hostile mob", "Remove 3❤ every 3 hits (30s cd)"),
    ENDER_VOID("Ender Mace", Material.ENDER_PEARL, TextColor.color(0x9932CC),
               "+20 hearts in The End", "Dragon breath (3 charges, 90s cd)", "Vortex pull on every hit"),
    NATURE("Nature Mace", Material.OAK_SAPLING, TextColor.color(0x228B22),
           "Regen II + Saturation", "Vine grapple (30 blocks)", "+1 Absorption heart every hit"),
    STORM("Storm Mace", Material.NETHER_STAR, TextColor.color(0xFFD700),
          "Speed II permanent", "Lightning launches you 20 blocks up", "Lightning strikes target every 3 hits"),
    FROST("Frost Mace", Material.PACKED_ICE, TextColor.color(0x00BFFF),
          "Frost walker aura", "Ice beam freezes 3s", "Slowness II every 3 hits"),
    BLOOD("Blood Mace", Material.NETHERITE_INGOT, TextColor.color(0xDC143C),
          "Permanent Strength I", "Bonus damage from kill streaks", "10s Berserk every 3 hits (30s cd)"),
    SPEED("Speed Mace", Material.FEATHER, TextColor.color(0x00FF7F),
          "Speed III permanent", "Gain 3 dash charges", "Speed V for 5s every 3 hits (30s cd)"),
    GOD("God Mace", Material.NETHERITE_BLOCK, TextColor.color(0xFF1493),
        "ALL passives combined", "Shift+RC to pick, then RC to use any smash", "ALL on-hit effects combined");

    private final String displayName;
    private final Material material;
    private final TextColor color;
    private final String passiveDesc;
    private final String smashDesc;
    private final String hitDesc;

    MaceType(String displayName, Material material, TextColor color, String passiveDesc, String smashDesc, String hitDesc) {
        this.displayName = displayName;
        this.material = material;
        this.color = color;
        this.passiveDesc = passiveDesc;
        this.smashDesc = smashDesc;
        this.hitDesc = hitDesc;
    }

    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public TextColor getColor() { return color; }
    public String getPassiveDesc() { return passiveDesc; }
    public String getSmashDesc() { return smashDesc; }
    public String getHitDesc() { return hitDesc; }

    public ItemStack createItemStack(CustomMaces plugin) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName, color, TextDecoration.BOLD));
        List<Component> lore = Arrays.asList(
            Component.text("", TextColor.color(0xAAAAAA)),
            Component.text("✦ Passive: " + passiveDesc, TextColor.color(0x55FF55)),
            Component.text("✦ Smash (3-hit): " + smashDesc, TextColor.color(0xFF55FF)),
            Component.text("✦ On-Hit: " + hitDesc, TextColor.color(0x55FFFF)),
            Component.text("", TextColor.color(0xAAAAAA)),
            Component.text("Enchanted with Wind Burst I", TextColor.color(0xAAAAAA))
        );
        meta.lore(lore);
        meta.addEnchant(Enchantment.WIND_BURST, 1, true);
        NamespacedKey key = new NamespacedKey(plugin, "mace_type");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, name().toLowerCase());
        item.setItemMeta(meta);
        return item;
    }

    public static MaceType fromItemStack(ItemStack item, CustomMaces plugin) {
        if (item == null || !item.hasItemMeta()) return null;
        NamespacedKey key = new NamespacedKey(plugin, "mace_type");
        String type = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (type == null) return null;
        try { return MaceType.valueOf(type.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    public static boolean isAnyMace(ItemStack item, CustomMaces plugin) {
        return fromItemStack(item, plugin) != null;
    }

    public static MaceType[] getBaseMaces() {
        return new MaceType[]{MAGMA, ECHO, SOUL, ENDER_VOID, NATURE, STORM, FROST, BLOOD, SPEED};
    }
}
