package com.custommaces.managers;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MaceManager {
    private final CustomMaces plugin;
    private BukkitRunnable inventoryChecker;

    // Hit combos
    private final Map<UUID, Integer> hitCombos = new HashMap<>();
    private final Map<UUID, Long> lastHitTimes = new HashMap<>();
    private static final long COMBO_RESET_MS = 5000;

    // Cooldowns (milliseconds)
    private final Map<UUID, Long> echoBeamCooldowns = new HashMap<>();
    private final Map<UUID, Long> soulHeartCooldowns = new HashMap<>();
    private final Map<UUID, Long> bloodBerserkCooldowns = new HashMap<>();
    private final Map<UUID, Long> speedBoostCooldowns = new HashMap<>();
    private final Map<UUID, Long> echoBlindnessCooldowns = new HashMap<>();

    // Charges
    private final Map<UUID, Integer> voidBreathCharges = new HashMap<>();
    private final Map<UUID, Long> voidBreathRecharge = new HashMap<>();
    private final Map<UUID, Integer> dashCharges = new HashMap<>();

    // Kill streaks
    private final Map<UUID, Integer> killStreaks = new HashMap<>();
    private final Map<UUID, Long> lastKillTimes = new HashMap<>();

    // Berserk mode
    private final Set<UUID> berserkPlayers = new HashSet<>();

    // God Mace selected ability
    private final Map<UUID, MaceType> godMaceSelected = new HashMap<>();

    public MaceManager(CustomMaces plugin) {
        this.plugin = plugin;
    }

    public void startInventoryChecker() {
        inventoryChecker = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!isHoldingShield(player)) {
                        applyInventoryPassives(player);
                    } else {
                        removeMacePassives(player);
                    }
                }
            }
        };
        inventoryChecker.runTaskTimer(plugin, 20L, 20L);
    }

    public void stopInventoryChecker() {
        if (inventoryChecker != null) inventoryChecker.cancel();
    }

    public boolean isHoldingShield(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.SHIELD
            || player.getInventory().getItemInOffHand().getType() == Material.SHIELD;
    }

    private List<MaceType> getMacesInInventory(Player player) {
        List<MaceType> maces = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            MaceType type = MaceType.fromItemStack(item, plugin);
            if (type != null && !maces.contains(type)) maces.add(type);
        }
        return maces;
    }

    public void applyInventoryPassives(Player player) {
        List<MaceType> maces = getMacesInInventory(player);
        if (maces.isEmpty()) {
            removeMacePassives(player);
            return;
        }

        boolean hasGod = maces.contains(MaceType.GOD);

        for (MaceType mace : maces) {
            switch (mace) {
                case MAGMA -> player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, false, false, true));
                case ECHO -> { /* Immunity handled by MaceEffectListener */ }
                case SOUL -> player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 40, 0, false, false, true)); // +4 hearts
                case ENDER_VOID -> {
                    if (player.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 40, 4, false, false, true)); // +20 hearts
                    }
                }
                case NATURE -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, false, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 40, 0, false, false, true));
                }
                case STORM -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, true));
                case FROST -> {
                    // Frost walker aura
                    org.bukkit.Location loc = player.getLocation();
                    for (int x = -2; x <= 2; x++) {
                        for (int z = -2; z <= 2; z++) {
                            org.bukkit.block.Block b = loc.clone().add(x, -1, z).getBlock();
                            if (b.getType() == Material.WATER) b.setType(Material.FROSTED_ICE);
                        }
                    }
                }
                case BLOOD -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false, true));
                    int streak = getKillStreak(player);
                    if (streak >= 1) {
                        int level = Math.min(streak, 3);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, level - 1, false, false, true));
                    }
                }
                case SPEED -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, false, false, true));
                case GOD -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, false, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 40, 4, false, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, false, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 40, 0, false, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, false, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false, true));
                    int streak = getKillStreak(player);
                    if (streak >= 1) {
                        int level = Math.min(streak, 3);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, level - 1, false, false, true));
                    }
                }
            }
        }
    }

    private void removeMacePassives(Player player) {
        // Effects naturally expire since we only apply for 2 seconds at a time
    }

    // Hit combo tracking
    public int getHitCombo(Player player) {
        UUID uuid = player.getUniqueId();
        Long last = lastHitTimes.get(uuid);
        if (last == null || System.currentTimeMillis() - last > COMBO_RESET_MS) {
            hitCombos.put(uuid, 0);
        }
        return hitCombos.getOrDefault(uuid, 0);
    }

    public int incrementHitCombo(Player player) {
        UUID uuid = player.getUniqueId();
        int combo = getHitCombo(player) + 1;
        hitCombos.put(uuid, combo);
        lastHitTimes.put(uuid, System.currentTimeMillis());
        return combo;
    }

    public void resetHitCombo(Player player) {
        hitCombos.put(player.getUniqueId(), 0);
    }

    // Cooldown helpers
    public boolean isOnCooldown(Player player, Map<UUID, Long> cooldownMap, long cooldownMs) {
        Long last = cooldownMap.get(player.getUniqueId());
        return last != null && System.currentTimeMillis() - last < cooldownMs;
    }

    public void setCooldown(Player player, Map<UUID, Long> cooldownMap) {
        cooldownMap.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public long getCooldownRemaining(Player player, Map<UUID, Long> cooldownMap, long cooldownMs) {
        Long last = cooldownMap.get(player.getUniqueId());
        if (last == null) return 0;
        return Math.max(0, cooldownMs - (System.currentTimeMillis() - last));
    }

    public boolean isEchoBeamOnCooldown(Player player) { return isOnCooldown(player, echoBeamCooldowns, 60000); }
    public void setEchoBeamCooldown(Player player) { setCooldown(player, echoBeamCooldowns); }

    public boolean isSoulHeartOnCooldown(Player player) { return isOnCooldown(player, soulHeartCooldowns, 30000); }
    public void setSoulHeartCooldown(Player player) { setCooldown(player, soulHeartCooldowns); }

    public boolean isBloodBerserkOnCooldown(Player player) { return isOnCooldown(player, bloodBerserkCooldowns, 30000); }
    public void setBloodBerserkCooldown(Player player) { setCooldown(player, bloodBerserkCooldowns); }

    public boolean isSpeedBoostOnCooldown(Player player) { return isOnCooldown(player, speedBoostCooldowns, 30000); }
    public void setSpeedBoostCooldown(Player player) { setCooldown(player, speedBoostCooldowns); }

    public boolean isEchoBlindnessOnCooldown(Player player) { return isOnCooldown(player, echoBlindnessCooldowns, 20000); }
    public void setEchoBlindnessCooldown(Player player) { setCooldown(player, echoBlindnessCooldowns); }

    // Void breath charges
    public int getVoidBreathCharges(Player player) {
        UUID uuid = player.getUniqueId();
        int charges = voidBreathCharges.getOrDefault(uuid, 3);
        Long lastRecharge = voidBreathRecharge.get(uuid);
        if (lastRecharge == null) {
            voidBreathRecharge.put(uuid, System.currentTimeMillis());
            return charges;
        }
        long elapsed = System.currentTimeMillis() - lastRecharge;
        int recharges = (int)(elapsed / 90000);
        if (recharges > 0) {
            charges = Math.min(3, charges + recharges);
            voidBreathCharges.put(uuid, charges);
            voidBreathRecharge.put(uuid, System.currentTimeMillis());
        }
        return charges;
    }

    public void useVoidBreathCharge(Player player) {
        UUID uuid = player.getUniqueId();
        int charges = getVoidBreathCharges(player) - 1;
        voidBreathCharges.put(uuid, charges);
    }

    // Dash charges
    public int getDashCharges(Player player) { return dashCharges.getOrDefault(player.getUniqueId(), 0); }
    public void setDashCharges(Player player, int charges) { dashCharges.put(player.getUniqueId(), charges); }
    public void useDashCharge(Player player) {
        UUID uuid = player.getUniqueId();
        int charges = dashCharges.getOrDefault(uuid, 0);
        if (charges > 0) dashCharges.put(uuid, charges - 1);
    }

    // Kill streaks
    public void addKill(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastKillTimes.get(uuid);
        if (last != null && now - last > 30000) killStreaks.put(uuid, 0);
        int streak = killStreaks.getOrDefault(uuid, 0) + 1;
        killStreaks.put(uuid, streak);
        lastKillTimes.put(uuid, now);
    }

    public int getKillStreak(Player player) { return killStreaks.getOrDefault(player.getUniqueId(), 0); }
    public void resetKillStreak(Player player) {
        killStreaks.remove(player.getUniqueId());
        lastKillTimes.remove(player.getUniqueId());
    }

    // Berserk mode
    public boolean isBerserk(Player player) { return berserkPlayers.contains(player.getUniqueId()); }
    public void setBerserk(Player player, boolean berserk) {
        if (berserk) berserkPlayers.add(player.getUniqueId());
        else berserkPlayers.remove(player.getUniqueId());
    }

    // God Mace selection
    public MaceType getGodMaceSelection(Player player) {
        return godMaceSelected.getOrDefault(player.getUniqueId(), MaceType.MAGMA);
    }

    public void cycleGodMaceSelection(Player player) {
        MaceType[] base = MaceType.getBaseMaces();
        MaceType current = godMaceSelected.getOrDefault(player.getUniqueId(), MaceType.MAGMA);
        int idx = 0;
        for (int i = 0; i < base.length; i++) {
            if (base[i] == current) { idx = i; break; }
        }
        MaceType next = base[(idx + 1) % base.length];
        godMaceSelected.put(player.getUniqueId(), next);
        player.sendActionBar(net.kyori.adventure.text.Component.text("Selected: " + next.getDisplayName(), next.getColor()));
    }
}
