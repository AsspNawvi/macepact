package com.custommaces.managers;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class MaceManager {

    private final CustomMaces plugin;
    private final NamespacedKey maceKey;

    private final Map<UUID, Integer> hitCounters       = new HashMap<>();
    private final Map<UUID, Long>    echoBeamCD        = new HashMap<>();
    private final Map<UUID, Long>    echoSmashCD       = new HashMap<>();   // NEW: 60s smash CD
    private final Map<UUID, Long>    soulSpawnCD       = new HashMap<>();
    private final Map<UUID, Long>    soulDrainCD       = new HashMap<>();   // NEW: soul drain smash CD
    private final Map<UUID, Long>    voidChargeCD      = new HashMap<>();
    private final Map<UUID, Integer> voidCharges       = new HashMap<>();
    private final Map<UUID, Long>    voidSingularityCD = new HashMap<>();   // NEW: singularity smash CD
    private final Map<UUID, Long>    bloodBerserkCD    = new HashMap<>();
    private final Map<UUID, Long>    bloodBerserkActive= new HashMap<>();
    private final Map<UUID, Long>    speedHitCD        = new HashMap<>();
    private final Map<UUID, Integer> speedDashes       = new HashMap<>();
    private final Map<UUID, Long>    speedDashRecharge = new HashMap<>();
    private final Map<UUID, Long>    stormAbilityCD    = new HashMap<>();   // NEW: 25s ability CD
    private final Map<UUID, Long>    natureGrappleCD   = new HashMap<>();
    private final Map<UUID, Long>    frostBeamCD       = new HashMap<>();
    private final Map<UUID, Integer> magmaCharges      = new HashMap<>();
    private final Map<UUID, Long>    magmaChargeCD     = new HashMap<>();
    private final Map<UUID, Integer> bloodStreak       = new HashMap<>();
    private final Map<UUID, Integer> frostSmashCount   = new HashMap<>();
    private final Map<UUID, MaceType> godSmashAbility  = new HashMap<>();

    public MaceManager(CustomMaces plugin) {
        this.plugin = plugin;
        this.maceKey = new NamespacedKey(plugin, "mace_type");
        registerGodMaceRecipe();
    }

    // ── Item creation ─────────────────────────────────────────────────────

    public ItemStack createMace(MaceType type) {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(type.getItemName());
        List<String> lore = new ArrayList<>();
        lore.add(type.getLore());
        if (type == MaceType.GOD) {
            lore.add("§8───────────────────");
            lore.add("§7Craft with all 9 custom maces");
            lore.add("§7Shift+Right-Click: Choose smash ability");
        }
        lore.add("§8[Custom Mace]");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(maceKey, PersistentDataType.STRING, type.name());
        meta.addEnchant(Enchantment.DENSITY, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        if (type == MaceType.BLOOD || type == MaceType.GOD) meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        if (type == MaceType.GOD) {
            meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addEnchant(Enchantment.LOOTING, 3, true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void registerGodMaceRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "god_mace");
        ShapelessRecipe recipe = new ShapelessRecipe(key, createMace(MaceType.GOD));
        for (int i = 0; i < 9; i++) recipe.addIngredient(Material.MACE);
        plugin.getServer().addRecipe(recipe);
    }

    // ── Type identification ───────────────────────────────────────────────

    public MaceType getMaceType(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String s = meta.getPersistentDataContainer().get(maceKey, PersistentDataType.STRING);
        if (s == null) return null;
        try { return MaceType.valueOf(s); } catch (Exception e) { return null; }
    }

    public MaceType getHeldMaceType(Player p) { return getMaceType(p.getInventory().getItemInMainHand()); }

    public MaceType getInventoryMaceType(Player p) {
        MaceType held = getHeldMaceType(p);
        if (held != null) return held;
        for (ItemStack item : p.getInventory().getContents()) {
            MaceType t = getMaceType(item);
            if (t != null) return t;
        }
        return null;
    }

    public NamespacedKey getMaceKey() { return maceKey; }

    // ── Hit combo counter ─────────────────────────────────────────────────

    public int incrementHitCounter(UUID uuid) {
        int count = hitCounters.getOrDefault(uuid, 0) + 1;
        if (count > 3) count = 1;
        hitCounters.put(uuid, count);
        return count;
    }

    public int getHitCounter(UUID uuid) { return hitCounters.getOrDefault(uuid, 0); }

    // ── Echo beam ability CD (60s) ────────────────────────────────────────

    public boolean isEchoBeamReady(UUID uuid) { return System.currentTimeMillis() > echoBeamCD.getOrDefault(uuid, 0L); }
    public void setEchoBeamCD(UUID uuid) { echoBeamCD.put(uuid, System.currentTimeMillis() + 60_000L); }
    public long echoBeamCDRemaining(UUID uuid) { return Math.max(0, echoBeamCD.getOrDefault(uuid, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Echo smash CD (60s) ───────────────────────────────────────────────

    public boolean isEchoSmashReady(UUID uuid) { return System.currentTimeMillis() > echoSmashCD.getOrDefault(uuid, 0L); }
    public void setEchoSmashCD(UUID uuid) { echoSmashCD.put(uuid, System.currentTimeMillis() + 60_000L); }
    public long echoSmashCDRemaining(UUID uuid) { return Math.max(0, echoSmashCD.getOrDefault(uuid, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Soul spawn ability CD (30s) ───────────────────────────────────────

    public boolean isSoulSpawnReady(UUID uuid) { return System.currentTimeMillis() > soulSpawnCD.getOrDefault(uuid, 0L); }
    public void setSoulSpawnCD(UUID uuid) { soulSpawnCD.put(uuid, System.currentTimeMillis() + 30_000L); }
    public long soulSpawnCDRemaining(UUID uuid) { return Math.max(0, soulSpawnCD.getOrDefault(uuid, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Soul drain smash CD (45s) ─────────────────────────────────────────

    public boolean isSoulDrainReady(UUID uuid) { return System.currentTimeMillis() > soulDrainCD.getOrDefault(uuid, 0L); }
    public void setSoulDrainCD(UUID uuid) { soulDrainCD.put(uuid, System.currentTimeMillis() + 45_000L); }
    public long soulDrainCDRemaining(UUID uuid) { return Math.max(0, soulDrainCD.getOrDefault(uuid, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Void charges (3 max, 90s recharge each) ──────────────────────────

    public int getVoidCharges(UUID uuid) { return voidCharges.getOrDefault(uuid, 3); }

    public boolean useVoidCharge(UUID uuid) {
        int charges = getVoidCharges(uuid);
        if (charges <= 0) return false;
        voidCharges.put(uuid, charges - 1);
        if (!voidChargeCD.containsKey(uuid)) scheduleVoidRecharge(uuid);
        return true;
    }

    private void scheduleVoidRecharge(UUID uuid) {
        voidChargeCD.put(uuid, System.currentTimeMillis());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int current = getVoidCharges(uuid);
            if (current < 3) voidCharges.put(uuid, current + 1);
            voidChargeCD.remove(uuid);
            if (getVoidCharges(uuid) < 3) scheduleVoidRecharge(uuid);
        }, 90 * 20L);
    }

    // ── Void singularity smash CD (60s) ──────────────────────────────────

    public boolean isVoidSingularityReady(UUID uuid) { return System.currentTimeMillis() > voidSingularityCD.getOrDefault(uuid, 0L); }
    public void setVoidSingularityCD(UUID uuid) { voidSingularityCD.put(uuid, System.currentTimeMillis() + 60_000L); }
    public long voidSingularityCDRemaining(UUID uuid) { return Math.max(0, voidSingularityCD.getOrDefault(uuid, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Storm ability CD (25s) ────────────────────────────────────────────

    public boolean isStormAbilityReady(UUID uuid) { return System.currentTimeMillis() > stormAbilityCD.getOrDefault(uuid, 0L); }
    public void setStormAbilityCD(UUID uuid) { stormAbilityCD.put(uuid, System.currentTimeMillis() + 25_000L); }
    public long stormAbilityCDRemaining(UUID uuid) { return Math.max(0, stormAbilityCD.getOrDefault(uuid, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Blood berserk ─────────────────────────────────────────────────────

    public boolean isBloodBerserkReady(UUID uuid) { return System.currentTimeMillis() > bloodBerserkCD.getOrDefault(uuid, 0L); }
    public boolean isBloodBerserkActive(UUID uuid) { return System.currentTimeMillis() < bloodBerserkActive.getOrDefault(uuid, 0L); }
    public void activateBloodBerserk(UUID uuid) {
        bloodBerserkActive.put(uuid, System.currentTimeMillis() + 10_000L);
        bloodBerserkCD.put(uuid, System.currentTimeMillis() + 30_000L);
    }

    // ── Blood streak ──────────────────────────────────────────────────────

    public int getBloodStreak(UUID uuid) { return bloodStreak.getOrDefault(uuid, 0); }
    public void incrementBloodStreak(UUID uuid) { bloodStreak.merge(uuid, 1, Integer::sum); }
    public void resetBloodStreak(UUID uuid) { bloodStreak.remove(uuid); }

    // ── Speed dashes (3 charges, 6s recharge) ────────────────────────────

    public int getSpeedDashes(UUID uuid) { return speedDashes.getOrDefault(uuid, 3); }

    public boolean useSpeedDash(UUID uuid) {
        int charges = getSpeedDashes(uuid);
        if (charges <= 0) return false;
        speedDashes.put(uuid, charges - 1);
        if (!speedDashRecharge.containsKey(uuid)) scheduleSpeedRecharge(uuid);
        return true;
    }

    private void scheduleSpeedRecharge(UUID uuid) {
        speedDashRecharge.put(uuid, System.currentTimeMillis());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int current = getSpeedDashes(uuid);
            if (current < 3) speedDashes.put(uuid, current + 1);
            speedDashRecharge.remove(uuid);
            if (getSpeedDashes(uuid) < 3) scheduleSpeedRecharge(uuid);
        }, 6 * 20L);
    }

    public boolean isSpeedHitReady(UUID uuid) { return System.currentTimeMillis() > speedHitCD.getOrDefault(uuid, 0L); }
    public void setSpeedHitCD(UUID uuid) { speedHitCD.put(uuid, System.currentTimeMillis() + 30_000L); }

    // ── Nature grapple CD (10s) ───────────────────────────────────────────

    public boolean isNatureGrappleReady(UUID uuid) { return System.currentTimeMillis() > natureGrappleCD.getOrDefault(uuid, 0L); }
    public void setNatureGrappleCD(UUID uuid) { natureGrappleCD.put(uuid, System.currentTimeMillis() + 10_000L); }
    public long natureGrappleCDRemaining(UUID uuid) { return Math.max(0, natureGrappleCD.getOrDefault(uuid, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Frost beam CD (15s) ───────────────────────────────────────────────

    public boolean isFrostBeamReady(UUID uuid) { return System.currentTimeMillis() > frostBeamCD.getOrDefault(uuid, 0L); }
    public void setFrostBeamCD(UUID uuid) { frostBeamCD.put(uuid, System.currentTimeMillis() + 15_000L); }
    public long frostBeamCDRemaining(UUID uuid) { return Math.max(0, frostBeamCD.getOrDefault(uuid, 0L) - System.currentTimeMillis()) / 1000; }

    // ── Magma charges (2 max, 15s recharge) ──────────────────────────────

    public int getMagmaCharges(UUID uuid) { return magmaCharges.getOrDefault(uuid, 2); }

    public boolean useMagmaCharge(UUID uuid) {
        int charges = getMagmaCharges(uuid);
        if (charges <= 0) return false;
        magmaCharges.put(uuid, charges - 1);
        if (!magmaChargeCD.containsKey(uuid)) scheduleMagmaRecharge(uuid);
        return true;
    }

    private void scheduleMagmaRecharge(UUID uuid) {
        magmaChargeCD.put(uuid, System.currentTimeMillis());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int current = getMagmaCharges(uuid);
            if (current < 2) magmaCharges.put(uuid, current + 1);
            magmaChargeCD.remove(uuid);
            if (getMagmaCharges(uuid) < 2) scheduleMagmaRecharge(uuid);
        }, 15 * 20L);
    }

    // ── Frost smash counter ───────────────────────────────────────────────

    public int incrementFrostSmash(UUID uuid) {
        int count = frostSmashCount.getOrDefault(uuid, 0) + 1;
        if (count > 3) count = 1;
        frostSmashCount.put(uuid, count);
        return count;
    }

    // ── God smash ability selector ────────────────────────────────────────

    public MaceType getGodSmashAbility(UUID uuid) { return godSmashAbility.get(uuid); }
    public void setGodSmashAbility(UUID uuid, MaceType type) {
        if (type == null || type == MaceType.GOD) godSmashAbility.remove(uuid);
        else godSmashAbility.put(uuid, type);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    public void cleanup() {
        hitCounters.clear(); echoBeamCD.clear(); echoSmashCD.clear();
        soulSpawnCD.clear(); soulDrainCD.clear(); voidChargeCD.clear();
        voidCharges.clear(); voidSingularityCD.clear(); bloodBerserkCD.clear();
        bloodBerserkActive.clear(); speedDashes.clear(); speedDashRecharge.clear();
        bloodStreak.clear(); frostSmashCount.clear(); speedHitCD.clear();
        stormAbilityCD.clear(); natureGrappleCD.clear(); frostBeamCD.clear();
        magmaCharges.clear(); magmaChargeCD.clear(); godSmashAbility.clear();
    }
}
