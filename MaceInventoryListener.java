package com.custommaces.commands;

import com.custommaces.CustomMaces;
import com.custommaces.MaceType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class MaceListCommand implements CommandExecutor {
    private final CustomMaces plugin;

    public MaceListCommand(CustomMaces plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(Component.text("=== CUSTOM MACES ===", TextColor.color(0xFFD700)));
        sender.sendMessage(Component.text(""));
        for (MaceType mace : MaceType.values()) {
            sender.sendMessage(Component.text(mace.getDisplayName(), mace.getColor())
                .append(Component.text(" | Passive: " + mace.getPassiveDesc(), TextColor.color(0xAAAAAA))));
            sender.sendMessage(Component.text("  Smash(3-hit): " + mace.getSmashDesc(), TextColor.color(0xFF55FF)));
            sender.sendMessage(Component.text("  On-Hit: " + mace.getHitDesc(), TextColor.color(0x55FFFF)));
            sender.sendMessage(Component.text(""));
        }
        sender.sendMessage(Component.text("Combine all 9 in crafting table -> God Mace", TextColor.color(0xFF1493)));
        sender.sendMessage(Component.text("Enchanted with Wind Burst I only", TextColor.color(0xAAAAAA)));
        sender.sendMessage(Component.text("Effects disabled while holding shield", TextColor.color(0xFF5555)));
        return true;
    }
}
