// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync;

import me.tksn.lunaChatProxySync.util.ConfigurationManager;
import me.tksn.lunaChatProxySync.util.ReplyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;


public record PlayerQuitListener(LunaChatProxySync plugin, ConfigurationManager configurationManager,
                                 ReplyManager replyManager) implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event){
        if(configurationManager.getDebugMode()){
            plugin.sendDebugMessage("イベントを受信しました");
            plugin.sendDebugMessage("Property: LOWEST");
        }
        replyManager.removeCurrentReplyTarget(event.getPlayer().getName());
    }

}
