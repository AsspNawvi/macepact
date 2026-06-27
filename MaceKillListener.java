package com.custommaces;

import com.custommaces.commands.GiveMaceCommand;
import com.custommaces.commands.MaceListCommand;
import com.custommaces.listeners.*;
import com.custommaces.managers.MaceManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomMaces extends JavaPlugin {
    private static CustomMaces instance;
    private MaceManager maceManager;

    @Override
    public void onEnable() {
        instance = this;
        maceManager = new MaceManager(this);
        getLogger().info("CustomMaces v1.0.0 enabled!");

        getCommand("givemace").setExecutor(new GiveMaceCommand(this));
        getCommand("macelist").setExecutor(new MaceListCommand(this));

        getServer().getPluginManager().registerEvents(new MaceInventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new MaceHitListener(this), this);
        getServer().getPluginManager().registerEvents(new MaceSmashListener(this), this);
        getServer().getPluginManager().registerEvents(new MaceKillListener(this), this);
        getServer().getPluginManager().registerEvents(new MaceDashListener(this), this);
        getServer().getPluginManager().registerEvents(new GodMaceCraftListener(this), this);
        getServer().getPluginManager().registerEvents(new MaceEffectListener(this), this);

        maceManager.startInventoryChecker();
    }

    @Override
    public void onDisable() {
        if (maceManager != null) maceManager.stopInventoryChecker();
        getLogger().info("CustomMaces disabled!");
    }

    public static CustomMaces getInstance() { return instance; }
    public MaceManager getMaceManager() { return maceManager; }
}
