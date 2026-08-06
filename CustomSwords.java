package com.customswords;

import com.customswords.commands.GiveSwordCommand;
import com.customswords.commands.SwordListCommand;
import com.customswords.listeners.*;
import com.customswords.managers.SwordManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomSwords extends JavaPlugin {

    private SwordManager swordManager;

    @Override
    public void onEnable() {
        swordManager = new SwordManager(this);

        getServer().getPluginManager().registerEvents(new SwordHoldListener(this), this);
        getServer().getPluginManager().registerEvents(new SwordHitListener(this), this);
        getServer().getPluginManager().registerEvents(new SwordAbilityListener(this), this);
        getServer().getPluginManager().registerEvents(new SwordKillListener(this), this);
        getServer().getPluginManager().registerEvents(new GodSwordGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new GodSwordCraftListener(this), this);

        getCommand("givesword").setExecutor(new GiveSwordCommand(this));
        getCommand("swordlist").setExecutor(new SwordListCommand(this));

        getLogger().info("CustomSwords enabled!");
    }

    @Override
    public void onDisable() {
        swordManager.cleanup();
        getLogger().info("CustomSwords disabled.");
    }

    public SwordManager getSwordManager() { return swordManager; }
}
