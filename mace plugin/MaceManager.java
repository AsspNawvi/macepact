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

    // Per-player state tracking
    private final Map<UUID, Integer> bloodKillStreaks = new HashMap<>();
    private final Map<UUID, Long> bloodBerserkExpiry = new HashMap<>();
    private final Map<UUID, Integer> speedDashCharges = new HashMap<>();
    private final Map<UUID, Long> speedDashRecharge = new HashMap<>();
    private final Map<UUID, Integer> hitCounters = new HashMap<>();

    public MaceManager(CustomMaces plugin) {
        this.plugin = plugin;
        this.maceKey = new NamespacedKey(plugin, "mace_type");
        registerGodMaceRecipe();
    }

    // ── Item creation ──────────────────────────────────────────────────────

    public ItemStack createMace(MaceType type) {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(type.getItemName());

        List<String> lore = new ArrayList<>();
        lore.add(type.getLore());
        if (type == MaceType.GOD) {
            lore.add("§8───────────────────");
            lore.add("§c🔥 Fire Resistance | Burn aura");
            lore.add("§b🌊 Dolphin's Grace | Water Breathing");
            lore.add("§5👻 Life steal | +8 hearts");
            lore.add("§8🌀 Target teleport every 4 hits");
            lore.add("§a🌿 Regen II | Saturation");
            lore.add("§e⚡ 30% chain lightning");
            lore.add("§f❄ Stacking Slowness");
            lore.add("§4⚔ Kill streak Strength I-III");
            lore.add("§3💨 Speed II | 2 dash charges");
            lore.add("§8───────────────────");
            lore.add("§6§lSMASH: ALL abilities at once");
        }
        lore.add("§8[Custom Mace]");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(maceKey, PersistentDataType.STRING, type.name());

        // Base enchants
        meta.addEnchant(Enchantment.DENSITY, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);

        // Type-specific enchants
        switch (type) {
            case BLOOD -> meta.addEnchant(Enchantment.SHARPNESS, 5, true);
            case FROST -> meta.addEnchant(Enchantment.BANE_OF_ARTHROPODS, 3, true);
            case STORM -> meta.addEnchant(Enchantment.SHARPNESS, 3, true);
            case GOD -> {
                meta.addEnchant(Enchantment.SHARPNESS, 10, true);
                meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                meta.addEnchant(Enchantment.LOOTING, 3, true);
                meta.addEnchant(Enchantment.MENDING, 1, true);
            }
            default -> {}
        }

        item.setItemMeta(meta);
        return item;
    }

    // ── God Mace shapeless crafting recipe ────────────────────────────────
    // Place all 9 custom maces anywhere in the crafting grid

    private void registerGodMaceRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "god_mace");
        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, createMace(MaceType.GOD));

        // Requires 9 maces (one of each type) — all are Material.MACE
        // We use 9 mace ingredients; the actual type-checking is done in
        // the GodMaceCraftListener to ensure one of each custom mace is present
        for (int i = 0; i < 9; i++) {
            recipe.addIngredient(Material.MACE);
        }

        plugin.getServer().addRecipe(recipe);
    }

    // ── Type identification ───────────────────────────────────────────────

    public MaceType getMaceType(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String stored = meta.getPersistentDataContainer().get(maceKey, PersistentDataType.STRING);
        if (stored == null) return null;
        try { return MaceType.valueOf(stored); } catch (IllegalArgumentException e) { return null; }
    }

    public MaceType getHeldMaceType(Player player) {
        return getMaceType(player.getInventory().getItemInMainHand());
    }

    public NamespacedKey getMaceKey() { return maceKey; }

    // ── Blood Mace: kill streak ───────────────────────────────────────────

    public int getKillStreak(UUID uuid) {
        return bloodKillStreaks.getOrDefault(uuid, 0);
    }

    public void incrementKillStreak(UUID uuid) {
        bloodKillStreaks.merge(uuid, 1, Integer::sum);
    }

    public void resetKillStreak(UUID uuid) {
        bloodKillStreaks.remove(uuid);
    }

    public boolean isInBerserk(UUID uuid) {
        Long expiry = bloodBerserkExpiry.get(uuid);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    public void startBerserk(UUID uuid, int durationSeconds) {
        bloodBerserkExpiry.put(uuid, System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    // ── Speed Mace: dash charges ──────────────────────────────────────────

    public int getDashCharges(UUID uuid) {
        return speedDashCharges.getOrDefault(uuid, 2);
    }

    public boolean useDashCharge(UUID uuid) {
        int charges = getDashCharges(uuid);
        if (charges <= 0) return false;
        speedDashCharges.put(uuid, charges - 1);
        if (!speedDashRecharge.containsKey(uuid)) {
            scheduleDashRecharge(uuid);
        }
        return true;
    }

    private void scheduleDashRecharge(UUID uuid) {
        speedDashRecharge.put(uuid, System.currentTimeMillis());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int current = getDashCharges(uuid);
            if (current < 2) speedDashCharges.put(uuid, current + 1);
            speedDashRecharge.remove(uuid);
            if (getDashCharges(uuid) < 2) scheduleDashRecharge(uuid);
        }, 6 * 20L);
    }

    // ── Void Mace: hit counter ────────────────────────────────────────────

    public int getAndIncrementHitCounter(UUID uuid) {
        int count = hitCounters.getOrDefault(uuid, 0) + 1;
        hitCounters.put(uuid, count);
        return count;
    }

    public void resetHitCounter(UUID uuid) {
        hitCounters.remove(uuid);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    public void cleanup() {
        bloodKillStreaks.clear();
        bloodBerserkExpiry.clear();
        speedDashCharges.clear();
        speedDashRecharge.clear();
        hitCounters.clear();
    }
}
