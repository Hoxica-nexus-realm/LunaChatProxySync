// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync.util;

import me.tksn.lunaChatProxySync.LunaChatProxySync;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigurationManager {
    private final LunaChatProxySync plugin;
    private boolean isDebugMode;
    private String channels = "";
    private List<String> syncChannels;
    private Plugin lunaChat;

    public ConfigurationManager(LunaChatProxySync plugin, Plugin lunaChat) {
        this.plugin = plugin;
        this.lunaChat = lunaChat;
        configLoaderHandle();
    }

    private void configLoaderHandle(){
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        channels = "";
        this.isDebugMode = config.getBoolean("debug-mode");
        if(this.isDebugMode) {
            plugin.getLogger().info("[Debug] デバッグモードが有効です");
        }
        this.syncChannels = config.getStringList("sync-channels");
        if(this.isDebugMode){
            this.syncChannels.forEach(channel -> {
                channels += channel + " ";
            });
            plugin.getLogger().info("[Debug] 同期するチャンネルを読み込みました: " + channels);
        }
    }


    public List<String> getSyncChannels(){
        return this.syncChannels;
    }

    public boolean getDebugMode(){
        return this.isDebugMode;
    }

    public void asyncReloadConfig(LunaChatConfigManager lunaChatConfigManager, NGWordReplacer ngWordReplacer){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            lunaChat.reloadConfig();
            configLoaderHandle();
            lunaChatConfigManager.lunaChatConfigReload();
            ngWordReplacer.reloadNGWordList();
        });
        if(this.isDebugMode){
            plugin.getLogger().info("[Debug] 設定を非同期でリロードしました");
        }
    }
}
