// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync.util;

import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.channel.Channel;
import com.github.ucchyocean.lc3.japanize.JapanizeType;
import com.github.ucchyocean.lc3.member.ChannelMember;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.UUID;


public class PublishMessage {
    public static void publish(ConfigurationManager configurationManager, LunaChatAPI lunaChatAPI, LunaChatConfigManager lunaChatConfigManager, String channelName, String playerName, String rawMessage, @Nullable String prefix, @Nullable String suffix, Plugin plugin, boolean userJapanized, JapanizeType japanizeType, NGWordReplacer ngWordReplacer, ClickableFormatter clickableFormatter){
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
            ComponentBuilder builderA;
            if(useNGWordReplace){
                builderA = clickableFormatter.getFirstComponent(channel,playerName,ngWordReplacer.replaceNGWord(translateColorCode(rawMessage),"*") ,prefix,suffix);
            }else{
                builderA = clickableFormatter.getFirstComponent(channel,playerName,translateColorCode(rawMessage),prefix,suffix);
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
            BaseComponent[] clickableText;
            ComponentBuilder builderB = new ComponentBuilder();
            if(userJapanized){
                builderB.append(" §6(" + japanizedMessage + ")").event((ClickEvent) null).event((HoverEvent) null);
                clickableText = builderA.append(builderB.create(), ComponentBuilder.FormatRetention.NONE).create();
            }else{
                clickableText = builderA.create();
            }
            for (ChannelMember memberName : channel.getMembers()) {
                UUID memberUUID = UUID.fromString(memberName.toString().replaceAll("\\$",""));
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    if(userJapanized){
                        member.spigot().sendMessage(clickableText);
                    }else{
                        member.spigot().sendMessage(clickableText);
                    }
                    if (configurationManager.getDebugMode()) {
                        plugin.getLogger().info("[Debug] チャンネル\"" + channelName + "\"におけるプレイヤー " + member.getName() + " にメッセージ\"" + rawMessage + "\"を送信しました");
                    }
                }
            }
        });
    }

    public static void publishPrivate(ConfigurationManager configurationManager, LunaChatAPI lunaChatAPI, LunaChatConfigManager lunaChatConfigManager, String sender, String target, String rawMessage, boolean userJapanized, JapanizeType japanizeType, NGWordReplacer ngWordReplacer, Plugin plugin, ClickableFormatter clickableFormatter, boolean isSelf){
        boolean useNGWordReplace = !lunaChatConfigManager.getNgWordPattern().isEmpty();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ComponentBuilder builderA;
            if (useNGWordReplace) {
                builderA = clickableFormatter.getPrivateComponent(sender,target,ngWordReplacer.replaceNGWord(translateColorCode(rawMessage),"*"));
            } else {
                builderA = clickableFormatter.getPrivateComponent(sender,target,translateColorCode(rawMessage));
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
            BaseComponent[] clickableText;
            ComponentBuilder builderB = new ComponentBuilder();
            if(userJapanized){
                builderB.append(" §6(" + japanizedMessage + ")").event((ClickEvent) null).event((HoverEvent) null);
                clickableText = builderA.append(builderB.create(), ComponentBuilder.FormatRetention.NONE).create();
            }else{
                clickableText = builderA.create();
            }
            Player targetPlayer;
            if(isSelf){
                targetPlayer = Bukkit.getServer().getPlayer(sender);
            }else{
                targetPlayer = Bukkit.getServer().getPlayer(target);
            }
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.spigot().sendMessage(clickableText);
                if(configurationManager.getDebugMode()){
                    plugin.getLogger().info("[Debug] プレイヤー" + targetPlayer.getName() + "に" + sender + "からのメッセージ" + "\"" + rawMessage + "\"を送信しました");
                }
            }
        });
    }

    public static String translateColorCode(String string){
        return string.replaceAll("&([0-9A-Fa-fK-Ok-oRr])","§$1");
    }

    private static String trimColorCode(String string){
        return string.replaceAll("&([0-9A-Fa-fK-Ok-oRr])","");
    }
}
