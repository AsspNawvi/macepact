package com.custommaces.commands;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GiveMaceCommand implements CommandExecutor, TabCompleter {

    private final CustomMaces plugin;

    public GiveMaceCommand(CustomMaces plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("custommaces.give")) {
            sender.sendMessage("§cNo permission."); return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§eUsage: /givemace <player> <mace>");
            sender.sendMessage("§7Maces: " + Arrays.stream(MaceType.values())
                .map(m -> m.name().toLowerCase()).collect(Collectors.joining(", ")));
            return true;
        }
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
        MaceType type = MaceType.fromString(args[1]);
        if (type == null) { sender.sendMessage("§cUnknown mace: " + args[1]); return true; }
        ItemStack mace = plugin.getMaceManager().createMace(type);
        target.getInventory().addItem(mace);
        target.sendMessage("§aYou received the §r" + type.getItemName() + "§a!");
        sender.sendMessage("§aGave §r" + type.getItemName() + " §ato " + target.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1)
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2)
            return Arrays.stream(MaceType.values()).map(m -> m.name().toLowerCase())
                .filter(n -> n.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        return List.of();
    }
}
