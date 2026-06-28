package com.custommaces.listeners;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import com.custommaces.managers.MaceManager;
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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public class GodMaceGUIListener implements Listener {

    private final CustomMaces plugin;
    private final MaceManager manager;
    private final Map<UUID, Inventory> openGUIs = new HashMap<>();
    // Track which GUI page is open: "ability" or "smash"
    private final Map<UUID, String> guiPage = new HashMap<>();

    public GodMaceGUIListener(CustomMaces plugin) {
        this.plugin = plugin;
        this.manager = plugin.getMaceManager();
    }

    @EventHandler
    public void onShiftRightClick(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        MaceType type = manager.getHeldMaceType(p);
        if (type != MaceType.GOD) return;

        event.setCancelled(true);
        openMainGUI(p);
    }

    // ── Main GUI: Choose Ability or Choose Smash ─────────────────────────

    private void openMainGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 9, "§6§l✦ God Mace Menu ✦");

        gui.setItem(2, makeItem(Material.MACE, "§b§lUSE ABILITY", "§7Right-click to trigger a mace ability"));
        gui.setItem(6, makeItem(Material.NETHER_STAR, "§e§lSET SMASH ABILITY", "§7Choose which smash to use on landing\n§8Current: §f" + getSmashLabel(p)));

        openGUIs.put(p.getUniqueId(), gui);
        guiPage.put(p.getUniqueId(), "main");
        p.openInventory(gui);
    }

    private String getSmashLabel(Player p) {
        MaceType chosen = manager.getGodSmashAbility(p.getUniqueId());
        return chosen == null ? "ALL" : chosen.getDisplayName();
    }

    // ── Ability GUI ───────────────────────────────────────────────────────

    private void openAbilityGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 9, "§6§l✦ Choose God Ability ✦");

        gui.setItem(0, makeItem(Material.BLAZE_POWDER,   "§c§lMAGMA",  "§7Launch fireball (" + manager.getMagmaCharges(p.getUniqueId()) + "/2 charges)"));
        gui.setItem(1, makeItem(Material.SCULK_SHRIEKER, "§8§lECHO",   "§7Warden sonic beam (" + manager.echoBeamCDRemaining(p.getUniqueId()) + "s CD)"));
        gui.setItem(2, makeItem(Material.SOUL_LANTERN,   "§5§lSOUL",   "§7Spawn hostile mob (" + manager.soulSpawnCDRemaining(p.getUniqueId()) + "s CD)"));
        gui.setItem(3, makeItem(Material.DRAGON_BREATH,  "§3§lVOID",   "§7Dragon breath (" + manager.getVoidCharges(p.getUniqueId()) + "/3 charges)"));
        gui.setItem(4, makeItem(Material.VINE,           "§a§lNATURE", "§7Vine grapple (pulls players) (" + manager.natureGrappleCDRemaining(p.getUniqueId()) + "s CD)"));
        gui.setItem(5, makeItem(Material.LIGHTNING_ROD,  "§e§lSTORM",  "§7Launch 20 blocks up"));
        gui.setItem(6, makeItem(Material.BLUE_ICE,       "§f§lFROST",  "§7Ice beam (" + manager.frostBeamCDRemaining(p.getUniqueId()) + "s CD)"));
        gui.setItem(7, makeItem(Material.NETHER_STAR,    "§4§lBLOOD",  "§7Berserk on 3-hit combo"));
        gui.setItem(8, makeItem(Material.FEATHER,        "§b§lSPEED",  "§7Dash (" + manager.getSpeedDashes(p.getUniqueId()) + "/3 charges)"));

        openGUIs.put(p.getUniqueId(), gui);
        guiPage.put(p.getUniqueId(), "ability");
        p.openInventory(gui);
    }

    // ── Smash Ability Selector GUI ────────────────────────────────────────

    private void openSmashGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 9, "§6§l✦ Choose Smash Ability ✦");

        MaceType current = manager.getGodSmashAbility(p.getUniqueId());

        gui.setItem(0, makeItem(Material.BLAZE_POWDER,   "§c§lMAGMA SMASH",  "§7Fire ring + launch nearby"));
        gui.setItem(1, makeItem(Material.SCULK_SHRIEKER, "§8§lECHO SMASH",   "§7Blindness + darkness AOE"));
        gui.setItem(2, makeItem(Material.SOUL_LANTERN,   "§5§lSOUL SMASH",   "§7Spawn mob + heal target 3 hearts"));
        gui.setItem(3, makeItem(Material.DRAGON_BREATH,  "§3§lVOID SMASH",   "§7+20 hearts + AOE pull (End only)"));
        gui.setItem(4, makeItem(Material.VINE,           "§a§lNATURE SMASH", "§7Root + poison nearby"));
        gui.setItem(5, makeItem(Material.LIGHTNING_ROD,  "§e§lSTORM SMASH",  "§75 lightning strikes"));
        gui.setItem(6, makeItem(Material.BLUE_ICE,       "§f§lFROST SMASH",  "§7Freeze AOE (every 3rd smash)"));
        gui.setItem(7, makeItem(Material.NETHER_STAR,    "§4§lBLOOD SMASH",  "§7Roar + damage particles"));
        gui.setItem(8, makeItem(Material.NETHER_STAR,    "§b§lSPEED SMASH",  "§7Knockback AOE + Speed boost"));

        // Slot 4 of row 2 (slot 13 in 18-wide, but we have 9-wide) — add a "Use ALL" button
        // We'll put it via a second row; reopen as 18-slot
        openGUIs.put(p.getUniqueId(), gui);
        guiPage.put(p.getUniqueId(), "smash");
        p.openInventory(gui);

        if (current != null) {
            p.sendActionBar("§eCurrent smash: §f" + current.getDisplayName() + " §8(click to change)");
        } else {
            p.sendActionBar("§eCurrent smash: §fALL");
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

        switch (page) {
            case "main" -> {
                if (slot == 2) openAbilityGUI(p);
                else if (slot == 6) openSmashGUI(p);
            }
            case "ability" -> handleAbilityClick(p, slot);
            case "smash"   -> handleSmashClick(p, slot);
        }
    }

    private void handleAbilityClick(Player p, int slot) {
        switch (slot) {
            case 0 -> godMagma(p);
            case 1 -> godEcho(p);
            case 2 -> godSoul(p);
            case 3 -> godVoid(p);
            case 4 -> godNature(p);
            case 5 -> godStorm(p);
            case 6 -> godFrost(p);
            case 7 -> {} // Blood is passive combo
            case 8 -> godSpeed(p);
        }
    }

    private void handleSmashClick(Player p, int slot) {
        MaceType[] smashTypes = {
            MaceType.MAGMA, MaceType.ECHO, MaceType.SOUL, MaceType.VOID,
            MaceType.NATURE, MaceType.STORM, MaceType.FROST, MaceType.BLOOD, MaceType.SPEED
        };
        if (slot >= 0 && slot < smashTypes.length) {
            MaceType chosen = smashTypes[slot];
            // Toggle: if same ability is clicked again, reset to ALL
            if (chosen == manager.getGodSmashAbility(p.getUniqueId())) {
                manager.setGodSmashAbility(p.getUniqueId(), null);
                p.sendActionBar("§6God Smash: §fALL abilities");
            } else {
                manager.setGodSmashAbility(p.getUniqueId(), chosen);
                p.sendActionBar("§6God Smash set to: §f" + chosen.getDisplayName());
            }
        }
    }

    // ── Ability implementations ───────────────────────────────────────────

    private void godMagma(Player p) {
        int charges = manager.getMagmaCharges(p.getUniqueId());
        if (charges <= 0) { p.sendActionBar("§cMagma: §7No charges! Recharging..."); return; }
        manager.useMagmaCharge(p.getUniqueId());
        Fireball fb = p.getWorld().spawn(p.getEyeLocation(), Fireball.class);
        fb.setDirection(p.getLocation().getDirection().multiply(2));
        fb.setShooter(p);
        fb.setYield(2.0f);
        p.sendActionBar("§c🔥 Fireball! §7Charges: §c" + (charges - 1) + "/2");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
    }

    private void godEcho(Player p) {
        if (!manager.isEchoBeamReady(p.getUniqueId())) {
            p.sendActionBar("§8Echo beam: §7" + manager.echoBeamCDRemaining(p.getUniqueId()) + "s");
            return;
        }
        manager.setEchoBeamCD(p.getUniqueId());
        RayTraceResult result = p.getWorld().rayTraceEntities(
            p.getEyeLocation(), p.getLocation().getDirection(), 30,
            e -> e instanceof LivingEntity && e != p
        );
        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            target.damage(Math.min(target.getHealth(), 8.0), p);
            target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.0f);
        p.getWorld().spawnParticle(Particle.SONIC_BOOM, p.getEyeLocation().add(p.getLocation().getDirection().multiply(5)), 1, 0, 0, 0, 0);
    }

    private void godSoul(Player p) {
        if (!manager.isSoulSpawnReady(p.getUniqueId())) {
            p.sendActionBar("§5Soul spawn: §7" + manager.soulSpawnCDRemaining(p.getUniqueId()) + "s");
            return;
        }
        manager.setSoulSpawnCD(p.getUniqueId());
        EntityType[] mobs = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                             EntityType.SPIDER, EntityType.WITCH, EntityType.BLAZE};
        EntityType mob = mobs[(int)(Math.random() * mobs.length)];
        Entity spawned = p.getWorld().spawnEntity(p.getLocation().add(1, 0, 1), mob);
        if (spawned instanceof Mob m) {
            LivingEntity nearest = null;
            double dist = Double.MAX_VALUE;
            for (Entity e : p.getNearbyEntities(20, 20, 20)) {
                if (e instanceof LivingEntity le && e != p && e != spawned) {
                    double d = e.getLocation().distance(p.getLocation());
                    if (d < dist) { dist = d; nearest = le; }
                }
            }
            if (nearest != null) m.setTarget(nearest);
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
    }

    private void godVoid(Player p) {
        boolean inEnd = p.getWorld().getEnvironment() == World.Environment.THE_END;
        int maxCharges = inEnd ? 3 : 2;
        int charges = Math.min(manager.getVoidCharges(p.getUniqueId()), maxCharges);
        if (charges <= 0) {
            p.sendActionBar("§3Void: §7No charges!" + (inEnd ? "" : " §8(Max 2 in Overworld)"));
            return;
        }
        manager.useVoidCharge(p.getUniqueId());
        DragonFireball ball = p.getWorld().spawn(p.getEyeLocation(), DragonFireball.class);
        ball.setDirection(p.getLocation().getDirection().multiply(2));
        ball.setShooter(p);
        p.sendActionBar("§3Dragon breath: §b" + (charges - 1) + "/" + maxCharges + " charges");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);
    }

    private void godNature(Player p) {
        if (!manager.isNatureGrappleReady(p.getUniqueId())) {
            p.sendActionBar("§aGrapple: §7" + manager.natureGrappleCDRemaining(p.getUniqueId()) + "s cooldown");
            return;
        }
        RayTraceResult result = p.getWorld().rayTraceEntities(
            p.getEyeLocation(), p.getLocation().getDirection(), 30,
            e -> e instanceof LivingEntity && e != p
        );
        if (result != null && result.getHitEntity() instanceof LivingEntity hitEntity) {
            Vector pull = p.getLocation().toVector()
                .subtract(hitEntity.getLocation().toVector()).normalize().multiply(2.5);
            pull.setY(0.5);
            hitEntity.setVelocity(pull);
            manager.setNatureGrappleCD(p.getUniqueId());
            p.sendActionBar("§aGrapple: §7Pulled target!");
        } else {
            RayTraceResult blockResult = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getLocation().getDirection(), 30);
            if (blockResult == null || blockResult.getHitBlock() == null) { p.sendActionBar("§aNo target in range!"); return; }
            Vector dir = blockResult.getHitBlock().getLocation().add(0.5, 0.5, 0.5).toVector()
                .subtract(p.getLocation().toVector()).normalize().multiply(2.5);
            dir.setY(Math.min(dir.getY() + 0.5, 1.5));
            p.setVelocity(dir);
            manager.setNatureGrappleCD(p.getUniqueId());
        }
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_VINE_PLACE, 1.0f, 1.0f);
    }

    private void godStorm(Player p) {
        p.getWorld().strikeLightningEffect(p.getLocation());
        p.setVelocity(new Vector(0, 3.5, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.5f);
    }

    private void godFrost(Player p) {
        if (!manager.isFrostBeamReady(p.getUniqueId())) {
            p.sendActionBar("§fFrost beam: §7" + manager.frostBeamCDRemaining(p.getUniqueId()) + "s cooldown");
            return;
        }
        manager.setFrostBeamCD(p.getUniqueId());
        RayTraceResult result = p.getWorld().rayTraceEntities(
            p.getEyeLocation(), p.getLocation().getDirection(), 30, e -> e instanceof LivingEntity && e != p);
        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 127));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
        }
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_POWDER_SNOW_PLACE, 1.5f, 0.5f);
        p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getEyeLocation().add(p.getLocation().getDirection().multiply(5)), 20, 0.5, 0.5, 0.5, 0);
    }

    private void godSpeed(Player p) {
        int charges = manager.getSpeedDashes(p.getUniqueId());
        if (charges <= 0) { p.sendActionBar("§bNo dash charges!"); return; }
        manager.useSpeedDash(p.getUniqueId());
        Vector dir = p.getLocation().getDirection().normalize().multiply(2.5);
        dir.setY(0.4);
        p.setVelocity(dir);
        p.sendActionBar("§bDash! §7Charges: §3" + (charges - 1) + "/3");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.5f);
    }
}
