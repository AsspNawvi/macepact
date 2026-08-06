package com.customswords.managers;

import com.customswords.CustomSwords;
import com.customswords.SwordType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class SwordManager {

    private final CustomSwords plugin;
    private final NamespacedKey swordKey;

    private final Map<UUID, Long>    echoBeamCD        = new HashMap<>();
    private final Map<UUID, Long>    echoSmashCD       = new HashMap<>();
    private final Map<UUID, Long>    soulSpawnCD       = new HashMap<>();
    private final Map<UUID, Long>    soulDrainCD       = new HashMap<>();
    private final Map<UUID, Long>    voidChargeCD      = new HashMap<>();
    private final Map<UUID, Integer> voidCharges       = new HashMap<>();
    private final Map<UUID, Long>    voidSingularityCD = new HashMap<>();
    private final Map<UUID, Long>    bloodBerserkCD    = new HashMap<>();
    private final Map<UUID, Long>    bloodBerserkActive= new HashMap<>();
    private final Map<UUID, Long>    speedHitCD        = new HashMap<>();
    private final Map<UUID, Integer> speedDashes       = new HashMap<>();
    private final Map<UUID, Long>    speedDashRecharge = new HashMap<>();
    private final Map<UUID, Long>    stormAbilityCD    = new HashMap<>();
    private final Map<UUID, Long>    natureGrappleCD   = new HashMap<>();
    private final Map<UUID, Long>    frostBeamCD       = new HashMap<>();
    private final Map<UUID, Integer> magmaCharges      = new HashMap<>();
    private final Map<UUID, Long>    magmaChargeCD     = new HashMap<>();
    private final Map<UUID, Integer> bloodStreak       = new HashMap<>();
    private final Map<UUID, SwordType> godAbilityChoice = new HashMap<>();

    public SwordManager(CustomSwords plugin) {
        this.plugin = plugin;
        this.swordKey = new NamespacedKey(plugin, "sword_type");
        registerGodSwordRecipe();
    }

    // ── Item creation ─────────────────────────────────────────────────────

    public ItemStack createSword(SwordType type) {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(type.getItemName());

        List<String> lore = new ArrayList<>();
        lore.add(type.getLore());
        if (type == SwordType.GOD) {
            lore.add("§8───────────────────");
            lore.add("§7Craft with all 9 custom swords");
            lore.add("§7Shift+Right-Click: Choose ability");
        }
        lore.add("§8[Custom Sword]");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(swordKey, PersistentDataType.STRING, type.name());

        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        if (type == SwordType.BLOOD || type == SwordType.GOD)
            meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
        if (type == SwordType.GOD) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addEnchant(Enchantment.LOOTING, 3, true);
            meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private void registerGodSwordRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "god_sword");
        ShapelessRecipe recipe = new ShapelessRecipe(key, createSword(SwordType.GOD));
        for (int i = 0; i < 9; i++) recipe.addIngredient(Material.NETHERITE_SWORD);
        plugin.getServer().addRecipe(recipe);
    }

    // ── Type identification ───────────────────────────────────────────────

    public SwordType getSwordType(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String s = meta.getPersistentDataContainer().get(swordKey, PersistentDataType.STRING);
        if (s == null) return null;
        try { return SwordType.valueOf(s); } catch (Exception e) { return null; }
    }

    public SwordType getHeldSwordType(Player p) {
        return getSwordType(p.getInventory().getItemInMainHand());
    }

    public SwordType getInventorySwordType(Player p) {
        SwordType held = getHeldSwordType(p);
        if (held != null) return held;
        for (ItemStack item : p.getInventory().getContents()) {
            SwordType t = getSwordType(item);
            if (t != null) return t;
        }
        return null;
    }

    public NamespacedKey getSwordKey() { return swordKey; }

    // ── Echo beam CD (60s) ────────────────────────────────────────────────
    public boolean isEchoBeamReady(UUID u)  { return System.currentTimeMillis() > echoBeamCD.getOrDefault(u, 0L); }
    public void setEchoBeamCD(UUID u)       { echoBeamCD.put(u, System.currentTimeMillis() + 60_000L); }
    public long echoBeamCDRemaining(UUID u) { return Math.max(0, echoBeamCD.getOrDefault(u, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Soul drain CD (45s) ───────────────────────────────────────────────
    public boolean isSoulDrainReady(UUID u)  { return System.currentTimeMillis() > soulDrainCD.getOrDefault(u, 0L); }
    public void setSoulDrainCD(UUID u)       { soulDrainCD.put(u, System.currentTimeMillis() + 45_000L); }
    public long soulDrainCDRemaining(UUID u) { return Math.max(0, soulDrainCD.getOrDefault(u, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Soul spawn CD (30s) ───────────────────────────────────────────────
    public boolean isSoulSpawnReady(UUID u)  { return System.currentTimeMillis() > soulSpawnCD.getOrDefault(u, 0L); }
    public void setSoulSpawnCD(UUID u)       { soulSpawnCD.put(u, System.currentTimeMillis() + 30_000L); }
    public long soulSpawnCDRemaining(UUID u) { return Math.max(0, soulSpawnCD.getOrDefault(u, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Void charges (3 max, 90s recharge, overworld cap 2) ──────────────
    public int getVoidCharges(UUID u) { return voidCharges.getOrDefault(u, 3); }

    public boolean useVoidCharge(UUID u) {
        int charges = getVoidCharges(u);
        if (charges <= 0) return false;
        voidCharges.put(u, charges - 1);
        if (!voidChargeCD.containsKey(u)) scheduleVoidRecharge(u);
        return true;
    }

    private void scheduleVoidRecharge(UUID u) {
        voidChargeCD.put(u, System.currentTimeMillis());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int c = getVoidCharges(u);
            if (c < 3) voidCharges.put(u, c + 1);
            voidChargeCD.remove(u);
            if (getVoidCharges(u) < 3) scheduleVoidRecharge(u);
        }, 90 * 20L);
    }

    // ── Void singularity CD (60s) ─────────────────────────────────────────
    public boolean isVoidSingularityReady(UUID u)  { return System.currentTimeMillis() > voidSingularityCD.getOrDefault(u, 0L); }
    public void setVoidSingularityCD(UUID u)       { voidSingularityCD.put(u, System.currentTimeMillis() + 60_000L); }
    public long voidSingularityCDRemaining(UUID u) { return Math.max(0, voidSingularityCD.getOrDefault(u, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Storm ability CD (25s) ────────────────────────────────────────────
    public boolean isStormAbilityReady(UUID u)  { return System.currentTimeMillis() > stormAbilityCD.getOrDefault(u, 0L); }
    public void setStormAbilityCD(UUID u)       { stormAbilityCD.put(u, System.currentTimeMillis() + 25_000L); }
    public long stormAbilityCDRemaining(UUID u) { return Math.max(0, stormAbilityCD.getOrDefault(u, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Blood berserk (30s CD, 10s active) ───────────────────────────────
    public boolean isBloodBerserkReady(UUID u)  { return System.currentTimeMillis() > bloodBerserkCD.getOrDefault(u, 0L); }
    public boolean isBloodBerserkActive(UUID u) { return System.currentTimeMillis() < bloodBerserkActive.getOrDefault(u, 0L); }
    public void activateBloodBerserk(UUID u) {
        bloodBerserkActive.put(u, System.currentTimeMillis() + 10_000L);
        bloodBerserkCD.put(u, System.currentTimeMillis() + 30_000L);
    }
    public long bloodBerserkCDRemaining(UUID u) { return Math.max(0, bloodBerserkCD.getOrDefault(u, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Blood streak ──────────────────────────────────────────────────────
    public int getBloodStreak(UUID u)        { return bloodStreak.getOrDefault(u, 0); }
    public void incrementBloodStreak(UUID u) { bloodStreak.merge(u, 1, Integer::sum); }
    public void resetBloodStreak(UUID u)     { bloodStreak.remove(u); }

    // ── Speed dashes (3 charges, 6s recharge) ────────────────────────────
    public int getSpeedDashes(UUID u) { return speedDashes.getOrDefault(u, 3); }

    public boolean useSpeedDash(UUID u) {
        int c = getSpeedDashes(u);
        if (c <= 0) return false;
        speedDashes.put(u, c - 1);
        if (!speedDashRecharge.containsKey(u)) scheduleSpeedRecharge(u);
        return true;
    }

    private void scheduleSpeedRecharge(UUID u) {
        speedDashRecharge.put(u, System.currentTimeMillis());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int c = getSpeedDashes(u);
            if (c < 3) speedDashes.put(u, c + 1);
            speedDashRecharge.remove(u);
            if (getSpeedDashes(u) < 3) scheduleSpeedRecharge(u);
        }, 6 * 20L);
    }

    public boolean isSpeedHitReady(UUID u) { return System.currentTimeMillis() > speedHitCD.getOrDefault(u, 0L); }
    public void setSpeedHitCD(UUID u)      { speedHitCD.put(u, System.currentTimeMillis() + 30_000L); }

    // ── Nature grapple CD (10s) ───────────────────────────────────────────
    public boolean isNatureGrappleReady(UUID u)  { return System.currentTimeMillis() > natureGrappleCD.getOrDefault(u, 0L); }
    public void setNatureGrappleCD(UUID u)       { natureGrappleCD.put(u, System.currentTimeMillis() + 10_000L); }
    public long natureGrappleCDRemaining(UUID u) { return Math.max(0, natureGrappleCD.getOrDefault(u, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Frost beam CD (15s) ───────────────────────────────────────────────
    public boolean isFrostBeamReady(UUID u)  { return System.currentTimeMillis() > frostBeamCD.getOrDefault(u, 0L); }
    public void setFrostBeamCD(UUID u)       { frostBeamCD.put(u, System.currentTimeMillis() + 15_000L); }
    public long frostBeamCDRemaining(UUID u) { return Math.max(0, frostBeamCD.getOrDefault(u, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Magma charges (2 max, 15s recharge) ──────────────────────────────
    public int getMagmaCharges(UUID u) { return magmaCharges.getOrDefault(u, 2); }

    public boolean useMagmaCharge(UUID u) {
        int c = getMagmaCharges(u);
        if (c <= 0) return false;
        magmaCharges.put(u, c - 1);
        if (!magmaChargeCD.containsKey(u)) scheduleMagmaRecharge(u);
        return true;
    }

    private void scheduleMagmaRecharge(UUID u) {
        magmaChargeCD.put(u, System.currentTimeMillis());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int c = getMagmaCharges(u);
            if (c < 2) magmaCharges.put(u, c + 1);
            magmaChargeCD.remove(u);
            if (getMagmaCharges(u) < 2) scheduleMagmaRecharge(u);
        }, 15 * 20L);
    }

    // ── God ability selector ──────────────────────────────────────────────
    public SwordType getGodAbilityChoice(UUID u)          { return godAbilityChoice.get(u); }
    public void setGodAbilityChoice(UUID u, SwordType t)  {
        if (t == null || t == SwordType.GOD) godAbilityChoice.remove(u);
        else godAbilityChoice.put(u, t);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────
    public void cleanup() {
        echoBeamCD.clear(); echoSmashCD.clear(); soulSpawnCD.clear();
        soulDrainCD.clear(); voidChargeCD.clear(); voidCharges.clear();
        voidSingularityCD.clear(); bloodBerserkCD.clear(); bloodBerserkActive.clear();
        speedDashes.clear(); speedDashRecharge.clear(); bloodStreak.clear();
        speedHitCD.clear(); stormAbilityCD.clear(); natureGrappleCD.clear();
        frostBeamCD.clear(); magmaCharges.clear(); magmaChargeCD.clear();
        godAbilityChoice.clear();
    }
}
