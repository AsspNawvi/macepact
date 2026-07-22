package com.custommaces.commands;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import org.bukkit.command.*;

public class MaceListCommand implements CommandExecutor {

    public MaceListCommand(CustomMaces plugin) {}

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage("§6§l== Custom Maces ==");
        for (MaceType type : MaceType.values())
            sender.sendMessage(type.getItemName() + " §8— §7" + type.getLore());
        sender.sendMessage("§7§oGod Mace: craft with all 9 maces");
        sender.sendMessage("§7Use §e/givemace <player> <mace> §7to obtain.");
        return true;
    }
}
