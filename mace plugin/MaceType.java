package com.custommaces;

public enum MaceType {

    MAGMA("Magma Mace", "§c§lMAGMA MACE", "§7Ignites the ground on smash | Burn aura | Fire Resistance"),
    TIDE("Tide Mace", "§b§lTIDE MACE", "§7Water wave smash | Dolphin's Grace | Water Breathing"),
    SOUL("Soul Mace", "§5§lSOUL MACE", "§7Life steal on hit | +8 hearts | Soul vortex smash"),
    VOID("Void Mace", "§8§lVOID MACE", "§7Teleports target every 4 hits | Void rift smash"),
    NATURE("Nature Mace", "§a§lNATURE MACE", "§7Regen II | Vine root smash | Saturation aura"),
    STORM("Storm Mace", "§e§lSTORM MACE", "§730% chain lightning | 5-bolt smash"),
    FROST("Frost Mace", "§f§lFROST MACE", "§7Stacking Slowness | Flash freeze smash"),
    BLOOD("Blood Mace", "§4§lBLOOD MACE", "§7Kill streak Strength I-III | Berserk smash"),
    SPEED("Speed Mace", "§3§lSPEED MACE", "§7Speed II | 2 dash charges | Shockwave smash"),
    GOD("God Mace", "§6§l✦ GOD MACE ✦", "§7ALL abilities combined | Craft with all 9 maces");

    private final String displayName;
    private final String itemName;
    private final String lore;

    MaceType(String displayName, String itemName, String lore) {
        this.displayName = displayName;
        this.itemName = itemName;
        this.lore = lore;
    }

    public String getDisplayName() { return displayName; }
    public String getItemName() { return itemName; }
    public String getLore() { return lore; }

    public static MaceType fromString(String name) {
        for (MaceType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
