package com.custommaces;

public enum MaceType {
    MAGMA("Magma Mace",    "§c§lMAGMA MACE",    "§7Passive: Fire Resistance | Right-Click: Launch 2 fireballs | Hit: Explosion"),
    ECHO("Echo Mace",      "§8§lECHO MACE",     "§7Passive: Immunity to blindness/darkness | Right-Click: Warden beam | Hit: Blindness every 3 hits"),
    SOUL("Soul Mace",      "§5§lSOUL MACE",     "§7Passive: +5 hearts | Right-Click: Spawn hostile mob | Hit: Remove 3 hearts every 3 hits"),
    VOID("Void Mace",      "§3§lVOID MACE",     "§7Passive: +20 hearts in End | Right-Click: Dragon breath (3 charges) | Hit: Pull vortex"),
    NATURE("Nature Mace",  "§a§lNATURE MACE",   "§7Passive: Regen II + Saturation | Right-Click: Vine grapple | Hit: +1 Absorption"),
    STORM("Storm Mace",    "§e§lSTORM MACE",    "§7Passive: Speed II | Right-Click: Launch 20 blocks up | Hit: Lightning every 3 hits"),
    FROST("Frost Mace",    "§f§lFROST MACE",    "§7Passive: Powdered snow on totem pop | Right-Click: Ice beam | Hit: Slowness every 3 smashes"),
    BLOOD("Blood Mace",    "§4§lBLOOD MACE",    "§7Passive: Strength I | Right-Click: 3-hit berserk (30s CD) | Hit: Autocrit"),
    SPEED("Speed Mace",    "§b§lSPEED MACE",    "§7Passive: Speed III | Right-Click: 3 dashes | Hit: Speed V every 3 smashes (30s CD)"),
    GOD("God Mace",        "§6§l✦ GOD MACE ✦",  "§7ALL passives | Shift+Right-Click: Choose any ability | All smash abilities");

    private final String displayName;
    private final String itemName;
    private final String lore;

    MaceType(String displayName, String itemName, String lore) {
        this.displayName = displayName;
        this.itemName = itemName;
        this.lore = lore;
    }

    public String getDisplayName() { return displayName; }
    public String getItemName()    { return itemName; }
    public String getLore()        { return lore; }

    public static MaceType fromString(String name) {
        for (MaceType t : values())
            if (t.name().equalsIgnoreCase(name) || t.displayName.equalsIgnoreCase(name)) return t;
        return null;
    }
}
