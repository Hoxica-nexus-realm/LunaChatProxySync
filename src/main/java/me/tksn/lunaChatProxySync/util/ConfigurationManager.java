// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync.util;

import me.tksn.lunaChatProxySync.LunaChatProxySync;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationManager {
    private final LunaChatProxySync plugin;
    private boolean isDebugMode;
    private String channels = "";
    private List<String> syncChannels;
    private Plugin lunaChat;
    private boolean isDiscordEnabled;
    private Map<String,Object> webhooks;
    private String chatEventPriority;
    private boolean allowMentions;

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
        this.isDebugMode = config.getBoolean("advanced.enable-debug-mode");
        if(this.isDebugMode) {
            plugin.getLogger().info("[Debug] デバッグモードが有効です");
        }
        this.chatEventPriority = config.getString("advanced.chat-event-priority","LOWEST").toUpperCase();
        this.syncChannels = config.getStringList("sync-channels");
        if(this.isDebugMode){
            this.syncChannels.forEach(channel -> {
                channels += channel + " ";
            });
            plugin.getLogger().info("[Debug] 同期するチャンネルを読み込みました: " + channels);
        }
        this.isDiscordEnabled = config.getBoolean("discord.enabled",false);
        if(this.isDebugMode){
            plugin.getLogger().info("[Debug] Discordへのチャット転送は" + (this.isDiscordEnabled ? "有効" : "無効") + "です");
        }
        this.allowMentions = config.getBoolean("discord.allow-mentions",false);
        if(this.isDebugMode){
            plugin.getLogger().info("[Debug] Discordへのチャット転送時のメンションの許可は" + (this.allowMentions ? "有効" : "無効") + "です");
        }
        ConfigurationSection webhookSection = config.getConfigurationSection("discord.webhooks");
        if(webhookSection != null){
            this.webhooks = webhookSection.getValues(false);
        }else{
            this.webhooks = new HashMap<>();
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

    public boolean getDiscordEnabled() {
        return isDiscordEnabled;
    }

    public List<String> getChannelWebhooks(String channelName){
        if(webhooks == null){
            return new ArrayList<>();
        }
        List<String> webhookUrls = new ArrayList<>();
        Object urlValues = webhooks.get(channelName);
        if(urlValues instanceof List<?>){
            webhookUrls.addAll(((List<?>) urlValues).stream().map(Object::toString).toList());
        }else if(urlValues instanceof String){
            webhookUrls.add((String) urlValues);
        }
        return webhookUrls;
    }

    public String getChatEventPriority(){
        return chatEventPriority;
    }

    public boolean getAllowMentions(){
        return allowMentions;
    }
}
