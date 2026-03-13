// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync.util;

import com.github.ucchyocean.lc3.channel.Channel;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordWebhookManager {
    private final Plugin plugin;
    private final Pattern webhookUrlPattern = Pattern.compile("https?://(ptb\\.|canary\\.|www\\.)?discord(app)?\\.com/api/webhooks/[0-9]+/[a-zA-Z0-9_-]+");
    private ConfigurationManager configurationManager;

    public DiscordWebhookManager(Plugin plugin, ConfigurationManager configurationManager){
        this.plugin = plugin;
        this.configurationManager = configurationManager;
    }

    public void publishDiscordWebhooks(Channel channel, Player player, String message){
        List<String> webhookUrls = configurationManager.getChannelWebhooks(channel.getName());
        for(String webhookUrl : webhookUrls){
            postToDiscordAPI(webhookUrl,player.getDisplayName(),getAvatarUrl(player),message);
        }
    }

    private void postToDiscordAPI(String webhookUrl, String Name, @Nullable String iconUrl, String message){
        boolean isDebugMode = configurationManager.getDebugMode();
        if(webhookUrl.isEmpty()){
            plugin.getLogger().warning("WebhookのURLが空文字です");
            return;
        }
        Matcher matcher = webhookUrlPattern.matcher(webhookUrl);
        if(!matcher.matches()){
            plugin.getLogger().warning("WebhookのURLが不正です");
            if(isDebugMode){
                plugin.getLogger().warning("[Debug] 不正なWebhookURL: " + webhookUrl);
            }
            return;
        }
        String webhookContent = getEscapedString(trimColorCode(message));
        if(webhookContent.isEmpty() && isDebugMode){
            plugin.getLogger().info("[Debug] Webhookで送信するメッセージが空文字のためWebhookは送信されません");
            return;
        }
        String webhookName = getEscapedString(trimColorCode(Name));
        String webhookAvatar = getEscapedString(iconUrl);
        String webhookFormat;
        String webhookFormatAllowMentions = configurationManager.getAllowMentions() ? "}" : ",\"allowed_mentions\":{\"parse\":[]}}";
        if((webhookName.isEmpty()) && (iconUrl == null || iconUrl.isEmpty())){
            webhookFormat = String.format("{\"content\":\"%s\"",webhookContent);
        }else if(iconUrl == null || iconUrl.isEmpty()){
            webhookFormat = String.format("{\"content\":\"%s\",\"username\":\"%s\"%s",webhookContent,webhookName,webhookFormatAllowMentions);
        }else if(webhookName.isEmpty()){
            webhookFormat = String.format("{\"content\":\"%s\",\"avatar_url\":\"%s\"%s",webhookContent,iconUrl,webhookFormatAllowMentions);
        }else{
            webhookFormat = String.format("{\"content\":\"%s\",\"username\":\"%s\",\"avatar_url\":\"%s\"%s",webhookContent,webhookName,webhookAvatar,webhookFormatAllowMentions);
        }
        if(isDebugMode){
            plugin.getLogger().info("[Debug] DiscordAPI\"" + webhookUrl + "\"にPOSTリクエストを送信しました:");
            plugin.getLogger().info("[Debug] 【内容】" + webhookFormat);
        }
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type","application/json")
                .header("User-Agent","Mozilla/5.0")
                .POST(HttpRequest.BodyPublishers.ofString(webhookFormat))
                .build();
        try{
            if(isDebugMode){
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                plugin.getLogger().info("[Debug] DiscordAPIからのレスポンスを受信しました:");
                plugin.getLogger().info("[Debug] ステータスコード: " + response.statusCode() + ", 内容: " + response.body());
            }else{
                httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
            }
        }catch (Exception e){
            plugin.getLogger().warning("Webhook送信中にエラーが発生しました");
            if(isDebugMode){
                e.printStackTrace();
            }
        }
    };

    private String getEscapedString(String string) {
        if(string == null){
            return "";
        }
        return string.replace("\"", "\\\"")
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String trimColorCode(String string){
        return string.replaceAll("&([0-9A-Fa-fK-Ok-oRr])","")
                .replaceAll("§([0-9A-Fa-fK-Ok-oRr])","");
    }

    private String getAvatarUrl(Player player){
        return String.format("https://crafthead.net/helm/%s",player.getName());
    }
}
