// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync.util;

import me.tksn.lunaChatProxySync.LunaChatProxySync;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;


public record CommandTabCompleter(LunaChatProxySync plugin, ConfigurationManager configurationManager) implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args){
        List<String> directMessageCommands = plugin.getDirectMessageCommands();
        Player player = plugin.getServer().getPlayer(sender.getName());
        if(directMessageCommands.contains(command.getName().toLowerCase()) && player != null && player.isOnline()){
            return plugin.getProxyPlayers();
        } else if (directMessageCommands.contains(alias.toLowerCase()) && player != null && player.isOnline()) {
            return plugin.getProxyPlayers();
        } else if (command.getName().equalsIgnoreCase("lcps")) {
            return List.of("help", "reload");
        } else {
            return List.of();
        }
    }
}
