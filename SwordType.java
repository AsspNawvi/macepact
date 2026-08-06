package com.customswords;

public enum SwordType {
    MAGMA("Magma Sword",  "§c§lMAGMA SWORD",   "§7Passive: Fire Resistance | Right-Click: 3 fireballs (2 charges)"),
    ECHO("Echo Sword",    "§8§lECHO SWORD",     "§7Passive: Warden immunity | Right-Click: Sonic beam (4 true dmg)"),
    SOUL("Soul Sword",    "§5§lSOUL SWORD",     "§7Passive: +5 hearts | Right-Click: Soul Drain AOE (45s CD)"),
    VOID("Void Sword",    "§3§lVOID SWORD",     "§7Passive: none | Right-Click: Dragon breath (2 true + 0.5/tick)"),
    NATURE("Nature Sword","§a§lNATURE SWORD",   "§7Passive: Regen II + Saturation | Right-Click: Grapple (pulls players/projectiles)"),
    STORM("Storm Sword",  "§e§lSTORM SWORD",    "§7Passive: Speed II | Right-Click: Launch upward (25s CD)"),
    FROST("Frost Sword",  "§f§lFROST SWORD",    "§7Passive: Snow on totem | Right-Click: Ice beam freeze (15s CD)"),
    BLOOD("Blood Sword",  "§4§lBLOOD SWORD",    "§7Passive: Strength I + Autocrit | Right-Click: Berserk (30s CD)"),
    GOD("God Sword",      "§6§l✦ GOD SWORD ✦",  "§7ALL passives | Shift+Right-Click: Choose ability via GUI");

    private final String displayName;
    private final String itemName;
    private final String lore;

    SwordType(String displayName, String itemName, String lore) {
        this.displayName = displayName;
        this.itemName = itemName;
        this.lore = lore;
    }

    public String getDisplayName() { return displayName; }
    public String getItemName()    { return itemName; }
    public String getLore()        { return lore; }

    public static SwordType fromString(String name) {
        for (SwordType t : values())
            if (t.name().equalsIgnoreCase(name) || t.displayName.equalsIgnoreCase(name)) return t;
        return null;
    }
}
