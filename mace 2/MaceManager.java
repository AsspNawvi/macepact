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

    // Hit counters (any weapon, resets at 3)
    private final Map<UUID, Integer> hitCounters = new HashMap<>();

    // Cooldowns: key = uuid, value = System.currentTimeMillis() when ability expires
    private final Map<UUID, Long> echoBeamCD    = new HashMap<>();
    private final Map<UUID, Long> soulSpawnCD   = new HashMap<>();
    private final Map<UUID, Long> voidChargeCD  = new HashMap<>();
    private final Map<UUID, Integer> voidCharges = new HashMap<>();
    private final Map<UUID, Long> bloodBerserkCD = new HashMap<>();
    private final Map<UUID, Long> bloodBerserkActive = new HashMap<>();
    private final Map<UUID, Long> speedHitCD    = new HashMap<>();
    private final Map<UUID, Integer> speedDashes = new HashMap<>();
    private final Map<UUID, Long> speedDashRecharge = new HashMap<>();

    // Blood kill streak (player kills only)
    private final Map<UUID, Integer> bloodStreak = new HashMap<>();

    // Frost smash hit counter (separate from main combo)
    private final Map<UUID, Integer> frostSmashCount = new HashMap<>();

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

    public MaceType getHeldMaceType(Player p) {
        return getMaceType(p.getInventory().getItemInMainHand());
    }

    public NamespacedKey getMaceKey() { return maceKey; }

    // ── Hit combo counter (any weapon) ────────────────────────────────────

    public int incrementHitCounter(UUID uuid) {
        int count = hitCounters.getOrDefault(uuid, 0) + 1;
        if (count > 3) count = 1; // reset after 3
        hitCounters.put(uuid, count);
        return count;
    }

    public int getHitCounter(UUID uuid) {
        return hitCounters.getOrDefault(uuid, 0);
    }

    // ── Echo beam cooldown (60s) ──────────────────────────────────────────

    public boolean isEchoBeamReady(UUID uuid) {
        return System.currentTimeMillis() > echoBeamCD.getOrDefault(uuid, 0L);
    }

    public void setEchoBeamCD(UUID uuid) {
        echoBeamCD.put(uuid, System.currentTimeMillis() + 60_000L);
    }

    public long echoBeamCDRemaining(UUID uuid) {
        return Math.max(0, echoBeamCD.getOrDefault(uuid, 0L) - System.currentTimeMillis()) / 1000;
    }

    // ── Soul spawn cooldown (30s) ─────────────────────────────────────────

    public boolean isSoulSpawnReady(UUID uuid) {
        return System.currentTimeMillis() > soulSpawnCD.getOrDefault(uuid, 0L);
    }

    public void setSoulSpawnCD(UUID uuid) {
        soulSpawnCD.put(uuid, System.currentTimeMillis() + 30_000L);
    }

    public long soulSpawnCDRemaining(UUID uuid) {
        return Math.max(0, soulSpawnCD.getOrDefault(uuid, 0L) - System.currentTimeMillis()) / 1000;
    }

    // ── Void dragon breath charges (3 charges, 90s recharge) ─────────────

    public int getVoidCharges(UUID uuid) {
        return voidCharges.getOrDefault(uuid, 3);
    }

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

    // ── Blood berserk (30s cooldown, 10s active) ──────────────────────────

    public boolean isBloodBerserkReady(UUID uuid) {
        return System.currentTimeMillis() > bloodBerserkCD.getOrDefault(uuid, 0L);
    }

    public boolean isBloodBerserkActive(UUID uuid) {
        return System.currentTimeMillis() < bloodBerserkActive.getOrDefault(uuid, 0L);
    }

    public void activateBloodBerserk(UUID uuid) {
        bloodBerserkActive.put(uuid, System.currentTimeMillis() + 10_000L);
        bloodBerserkCD.put(uuid, System.currentTimeMillis() + 30_000L);
    }

    public long bloodBerserkCDRemaining(UUID uuid) {
        return Math.max(0, bloodBerserkCD.getOrDefault(uuid, 0L) - System.currentTimeMillis()) / 1000;
    }

    // ── Blood kill streak (player kills only) ────────────────────────────

    public int getBloodStreak(UUID uuid) { return bloodStreak.getOrDefault(uuid, 0); }
    public void incrementBloodStreak(UUID uuid) { bloodStreak.merge(uuid, 1, Integer::sum); }
    public void resetBloodStreak(UUID uuid) { bloodStreak.remove(uuid); }

    // ── Speed dashes (3 charges) ──────────────────────────────────────────

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

    // ── Speed hit ability cooldown (30s) ──────────────────────────────────

    public boolean isSpeedHitReady(UUID uuid) {
        return System.currentTimeMillis() > speedHitCD.getOrDefault(uuid, 0L);
    }

    public void setSpeedHitCD(UUID uuid) {
        speedHitCD.put(uuid, System.currentTimeMillis() + 30_000L);
    }

    // ── Frost smash hit counter ───────────────────────────────────────────

    public int incrementFrostSmash(UUID uuid) {
        int count = frostSmashCount.getOrDefault(uuid, 0) + 1;
        if (count > 3) count = 1;
        frostSmashCount.put(uuid, count);
        return count;
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    public void cleanup() {
        hitCounters.clear(); echoBeamCD.clear(); soulSpawnCD.clear();
        voidChargeCD.clear(); voidCharges.clear(); bloodBerserkCD.clear();
        bloodBerserkActive.clear(); speedDashes.clear(); speedDashRecharge.clear();
        bloodStreak.clear(); frostSmashCount.clear(); speedHitCD.clear();
    }
}
