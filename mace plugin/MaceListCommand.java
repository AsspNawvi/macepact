package com.custommaces.commands;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MaceListCommand implements CommandExecutor {

    public MaceListCommand(CustomMaces plugin) {}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("§6§l== Custom Maces ==");
        for (MaceType type : MaceType.values()) {
            sender.sendMessage(type.getItemName() + " §8— §7" + type.getLore());
        }
        sender.sendMessage("§7§o✦ God Mace: place all 9 in a crafting table");
        sender.sendMessage("§7Use §e/givemace <player> <mace> §7to obtain one.");
        return true;
    }
}
