package com.custommaces;

import com.custommaces.commands.GiveMaceCommand;
import com.custommaces.commands.MaceListCommand;
import com.custommaces.listeners.*;
import com.custommaces.managers.MaceManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomMaces extends JavaPlugin {

    private static CustomMaces instance;
    private MaceManager maceManager;

    @Override
    public void onEnable() {
        instance = this;
        maceManager = new MaceManager(this);

        getServer().getPluginManager().registerEvents(new MaceHoldListener(this), this);
        getServer().getPluginManager().registerEvents(new MaceHitListener(this), this);
        getServer().getPluginManager().registerEvents(new MaceSmashListener(this), this);
        getServer().getPluginManager().registerEvents(new MaceAbilityListener(this), this);
        getServer().getPluginManager().registerEvents(new GodMaceGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new MaceKillListener(this), this);
        getServer().getPluginManager().registerEvents(new GodMaceCraftListener(this), this);

        getCommand("givemace").setExecutor(new GiveMaceCommand(this));
        getCommand("macelist").setExecutor(new MaceListCommand(this));

        getLogger().info("CustomMaces v2 enabled! 9 maces + God Mace ready.");
    }

    @Override
    public void onDisable() {
        maceManager.cleanup();
    }

    public static CustomMaces getInstance() { return instance; }
    public MaceManager getMaceManager()     { return maceManager; }
}
