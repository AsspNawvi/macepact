package com.customswords.commands;

import com.customswords.CustomSwords;
import com.customswords.SwordType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SwordListCommand implements CommandExecutor {

    public SwordListCommand(CustomSwords plugin) {}

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage("§6§l--- Custom Swords ---");
        for (SwordType t : SwordType.values())
            sender.sendMessage(t.getItemName() + " §8- §7" + t.getLore());
        return true;
    }
}
