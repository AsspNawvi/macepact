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
        openAbilityGUI(p);
    }

    private void openAbilityGUI(Player p) {
        Inventory gui = Bukkit.createInventory(null, 9, "§6§l✦ Choose God Ability ✦");

        gui.setItem(0, makeItem(Material.BLAZE_POWDER,    "§c§lMAGMA",   "§7Launch 2 fireballs"));
        gui.setItem(1, makeItem(Material.SCULK_SHRIEKER,  "§8§lECHO",    "§7Warden sonic beam (60s CD)"));
        gui.setItem(2, makeItem(Material.SOUL_LANTERN,    "§5§lSOUL",    "§7Spawn hostile mob (30s CD)"));
        gui.setItem(3, makeItem(Material.DRAGON_BREATH,   "§3§lVOID",    "§7Dragon breath (" + manager.getVoidCharges(p.getUniqueId()) + "/3 charges)"));
        gui.setItem(4, makeItem(Material.VINE,            "§a§lNATURE",  "§7Vine grapple (30 blocks)"));
        gui.setItem(5, makeItem(Material.LIGHTNING_ROD,   "§e§lSTORM",   "§7Launch 20 blocks up"));
        gui.setItem(6, makeItem(Material.BLUE_ICE,        "§f§lFROST",   "§7Ice beam - instant freeze"));
        gui.setItem(7, makeItem(Material.NETHER_STAR,     "§4§lBLOOD",   "§7Berserk on 3-hit combo"));
        gui.setItem(8, makeItem(Material.FEATHER,         "§b§lSPEED",   "§7Dash (" + manager.getSpeedDashes(p.getUniqueId()) + "/3 charges)"));

        openGUIs.put(p.getUniqueId(), gui);
        p.openInventory(gui);
    }

    private ItemStack makeItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore, "§7Click to use"));
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
        p.closeInventory();
        openGUIs.remove(p.getUniqueId());

        switch (slot) {
            case 0 -> godMagma(p);
            case 1 -> godEcho(p);
            case 2 -> godSoul(p);
            case 3 -> godVoid(p);
            case 4 -> godNature(p);
            case 5 -> godStorm(p);
            case 6 -> godFrost(p);
            case 7 -> {} // Blood berserk is on 3-hit combo, no manual trigger
            case 8 -> godSpeed(p);
        }
    }

    // Same ability logic as MaceAbilityListener but using God Mace cooldowns

    private void godMagma(Player p) {
        for (int i = 0; i < 2; i++) {
            Fireball fb = p.getWorld().spawn(p.getEyeLocation(), Fireball.class);
            fb.setDirection(p.getLocation().getDirection().multiply(2));
            fb.setShooter(p);
            fb.setYield(2.0f);
        }
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
        int charges = manager.getVoidCharges(p.getUniqueId());
        if (charges <= 0) { p.sendActionBar("§3Void: §7No charges!"); return; }
        manager.useVoidCharge(p.getUniqueId());
        DragonFireball ball = p.getWorld().spawn(p.getEyeLocation(), DragonFireball.class);
        ball.setDirection(p.getLocation().getDirection().multiply(2));
        ball.setShooter(p);
        p.sendActionBar("§3Dragon breath: §b" + (charges - 1) + "/3 charges");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);
    }

    private void godNature(Player p) {
        RayTraceResult result = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getLocation().getDirection(), 30);
        if (result == null || result.getHitBlock() == null) { p.sendActionBar("§aNo block in range!"); return; }
        Vector dir = result.getHitBlock().getLocation().add(0.5, 0.5, 0.5).toVector()
            .subtract(p.getLocation().toVector()).normalize().multiply(2.5);
        dir.setY(Math.min(dir.getY() + 0.5, 1.5));
        p.setVelocity(dir);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_VINE_PLACE, 1.0f, 1.0f);
    }

    private void godStorm(Player p) {
        p.getWorld().strikeLightningEffect(p.getLocation());
        p.setVelocity(new Vector(0, 3.5, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.5f);
    }

    private void godFrost(Player p) {
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
