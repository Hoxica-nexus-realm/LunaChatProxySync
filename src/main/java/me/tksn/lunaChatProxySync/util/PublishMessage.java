// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync.util;

import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.channel.Channel;
import com.github.ucchyocean.lc3.japanize.JapanizeType;
import com.github.ucchyocean.lc3.member.ChannelMember;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.UUID;


public class PublishMessage {
    public static void publish(ConfigurationManager configurationManager, LunaChatAPI lunaChatAPI, LunaChatConfigManager lunaChatConfigManager, String channelName, String playerName, String rawMessage, @Nullable String prefix, @Nullable String suffix, Plugin plugin, boolean userJapanized, JapanizeType japanizeType, NGWordReplacer ngWordReplacer){
        if(!configurationManager.getSyncChannels().contains(channelName)) {
            if(configurationManager.getDebugMode()){
                plugin.getLogger().info("[Debug] チャンネル\"" + channelName + "\"は同期チャンネルリストに含まれていません");
            }
            return;
        }
        if(!lunaChatAPI.isExistChannel(channelName)){
            if(configurationManager.getDebugMode()){
                plugin.getLogger().info("[Debug] チャンネル\"" + channelName + "\"は存在しないためメッセージは送信されませんでした");
            }
            return;
        }
        Channel channel = lunaChatAPI.getChannel(channelName);
        boolean useNGWordReplace = !lunaChatConfigManager.getNgWordPattern().isEmpty();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String formattedMessage;
            if(useNGWordReplace){
                formattedMessage = ngWordReplacer.replaceNGWord(translateColorCode(LunaChatUtil.getFormattedMessage(channel,playerName,rawMessage,prefix,suffix)),"*");
            }else{
                formattedMessage = translateColorCode(LunaChatUtil.getFormattedMessage(channel,playerName,rawMessage,prefix,suffix));
            }
            String japanizedMessage = "";
            String replacedRawMessage;
            if(userJapanized){
                if(useNGWordReplace){
                    replacedRawMessage = trimColorCode(rawMessage);
                    String plainjapanizedMessage = LunaChatUtil.getJapanized(replacedRawMessage, japanizeType, lunaChatAPI);
                    japanizedMessage = ngWordReplacer.replaceNGWord(plainjapanizedMessage,"*");
                }else {
                    replacedRawMessage = trimColorCode(rawMessage);
                    japanizedMessage = LunaChatUtil.getJapanized(replacedRawMessage, japanizeType, lunaChatAPI);
                }
            }
            for (ChannelMember memberName : channel.getMembers()) {
                UUID memberUUID = UUID.fromString(memberName.toString().replaceAll("\\$",""));
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    if(userJapanized){
                        member.sendMessage(formattedMessage + " §6(" + japanizedMessage + ")");
                    }else{
                        member.sendMessage(formattedMessage);
                    }
                    if (configurationManager.getDebugMode()) {
                        plugin.getLogger().info("[Debug] チャンネル\"" + channelName + "\"におけるプレイヤー " + member.getName() + " にメッセージ\"" + formattedMessage + "\"を送信しました");
                    }
                }
            }
        });
    }

    private static String translateColorCode(String string){
        return string.replaceAll("&([0-9A-Fa-fK-Ok-oRr])","§$1");
    }

    private static String trimColorCode(String string){
        return string.replaceAll("&([0-9A-Fa-fK-Ok-oRr])","");
    }
}
