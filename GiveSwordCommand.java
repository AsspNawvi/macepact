package com.customswords.commands;

import com.customswords.CustomSwords;
import com.customswords.SwordType;
import com.customswords.managers.SwordManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiveSwordCommand implements CommandExecutor {

    private final SwordManager manager;

    public GiveSwordCommand(CustomSwords plugin) {
        this.manager = plugin.getSwordManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /givesword <type> [player]");
            StringBuilder types = new StringBuilder("§7Types: ");
            for (SwordType t : SwordType.values()) types.append(t.name()).append(", ");
            sender.sendMessage(types.toString().replaceAll(", $", ""));
            return true;
        }

        SwordType type = SwordType.fromString(args[0]);
        if (type == null) {
            sender.sendMessage("§cUnknown sword type: " + args[0]);
            return true;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage("§cPlayer not found: " + args[1]); return true; }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("§cSpecify a player when running from console."); return true;
        }

        target.getInventory().addItem(manager.createSword(type));
        target.sendMessage("§aYou received: " + type.getItemName());
        if (!target.equals(sender)) sender.sendMessage("§aGave " + type.getDisplayName() + " to " + target.getName());
        return true;
    }
}
