package com.custommaces.commands;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class GiveMaceCommand implements CommandExecutor {
    private final CustomMaces plugin;

    public GiveMaceCommand(CustomMaces plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("custommaces.give")) {
            sender.sendMessage(Component.text("No permission!", TextColor.color(0xFF5555)));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /givemace <player> <mace>", TextColor.color(0xFFAA00)));
            sender.sendMessage(Component.text("magma, echo, soul, ender, nature, storm, frost, blood, speed, god", TextColor.color(0xAAAAAA)));
            return true;
        }
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", TextColor.color(0xFF5555)));
            return true;
        }
        String name = args[1].toUpperCase();
        if (name.equals("ENDER")) name = "ENDER_VOID";
        MaceType mace;
        try { mace = MaceType.valueOf(name); }
        catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid mace! Use: magma, echo, soul, ender, nature, storm, frost, blood, speed, god", TextColor.color(0xFF5555)));
            return true;
        }
        target.getInventory().addItem(mace.createItemStack(plugin));
        sender.sendMessage(Component.text("Gave " + mace.getDisplayName() + " to " + target.getName(), TextColor.color(0x55FF55)));
        return true;
    }
}
