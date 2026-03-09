// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync;

import me.tksn.lunaChatProxySync.util.ConfigurationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public record DirectMessageCommandListener(LunaChatProxySync plugin, ConfigurationManager configurationManager) implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event){
        if(configurationManager.getDebugMode()){
            plugin.sendDebugMessage("イベントを受信しました");
            plugin.sendDebugMessage("Property: LOWEST");
        }
        plugin.directMessageCommand(event);
    }
}
