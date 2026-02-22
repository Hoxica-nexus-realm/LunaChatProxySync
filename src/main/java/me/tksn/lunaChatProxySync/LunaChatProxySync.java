// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync;

import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.channel.Channel;
import com.github.ucchyocean.lc3.japanize.JapanizeType;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.base.Strings;
import me.tksn.lunaChatProxySync.util.*;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Objects;


public final class LunaChatProxySync extends JavaPlugin implements PluginMessageListener{
    private ConfigurationManager configurationManager;
    private LuckPerms lpApi;
    private LunaChatAPI lunaChatAPI;
    private LunaChatConfigManager lunaChatConfigManager;
    private JapanizeType japanizeType;
    private NGWordReplacer ngWordReplacer;
    private String globalChannel;
    private String globalMarker;
    private String noneJapanizeMarker;
    @Override
    public void onEnable(){
        Plugin lc = getServer().getPluginManager().getPlugin("LunaChat");
        if(lc == null || !lc.isEnabled()){
            getLogger().warning("LunaChatが検出されないためプラグインは有効になりません");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        this.lunaChatAPI = LunaChatUtil.getLunaChatAPI();
        this.configurationManager = new ConfigurationManager(this,lc);
        getServer().getPluginManager().registerEvents(new ChatListener(this,this.configurationManager), this);
        this.lunaChatConfigManager = new LunaChatConfigManager(this,lc, configurationManager);
        this.lpApi = LuckPermsProvider.get();
        this.japanizeType = lunaChatConfigManager.getJapanizeType();
        this.ngWordReplacer = new NGWordReplacer(this.lunaChatConfigManager);
        this.globalChannel = this.lunaChatConfigManager.getGlobalChannel();
        this.globalMarker = this.lunaChatConfigManager.getGlobalMarker();
        this.noneJapanizeMarker = this.lunaChatConfigManager.getNoneJapanizeMarker();
    }

    @Override
    public void onDisable(){
        getLogger().info("LunaChatProxySyncを無効化しました");
    }

    @Override
    public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
        if(configurationManager.getDebugMode()){
            getLogger().info("[Debug] PluginMessageを受信しました");
        }
        if (!s.equals("BungeeCord")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String subchannel = in.readUTF();

        if (subchannel.equals("LunaChatProxySync")) {
            if(configurationManager.getDebugMode()){
                getLogger().info("[Debug] PluginMessageのサブチャンネルが一致しました");
            }
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);

            ByteArrayDataInput msgin = ByteStreams.newDataInput(msgbytes);
            String message = msgin.readUTF();
            String channelName = msgin.readUTF();
            String rawMessage = msgin.readUTF();
            String playerName = msgin.readUTF();
            String prefix = Strings.emptyToNull(msgin.readUTF());
            String suffix = Strings.emptyToNull(msgin.readUTF());
            boolean userJapanized = msgin.readUTF().equals("yes");
            if(configurationManager.getDebugMode()){
                getLogger().info("[Debug] 【受信内容】メッセージ: " + message + ", チャンネル名: " + channelName + ", 生のメッセージ: " + rawMessage);
            }
            PublishMessage.publish(configurationManager, lunaChatAPI, lunaChatConfigManager, channelName, playerName, rawMessage, prefix, suffix, this, userJapanized, japanizeType, ngWordReplacer);
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(args.length == 0){
            commandSendHelp(sender);
            return true;
        }
        String subCmd = args[0].toLowerCase();
        if(subCmd.equals("reload")){
            configurationManager.asyncReloadConfig(lunaChatConfigManager,ngWordReplacer);
            japanizeType = lunaChatConfigManager.getJapanizeType();
            sender.sendMessage("§b設定を非同期でリロードしました");
            return true;
        }else if(subCmd.equals("help")){
            commandSendHelp(sender);
            return true;
        }else{
            commandSendHelp(sender);
            return true;
        }
    }

    public void forwardChat(AsyncPlayerChatEvent event){
        if(configurationManager.getDebugMode()){
            getLogger().info("[Debug] AsyncPlayerChatEventを受信しました");
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("LunaChatProxySync");

        ByteArrayDataOutput messageBytes = ByteStreams.newDataOutput();
        String debugLog;
        String prefix = null;
        String suffix = null;
        Player player = event.getPlayer();
        boolean japanized;
        Channel channel;
        String rawMessage = event.getMessage();
        if(rawMessage.startsWith(globalMarker) && rawMessage.length() > 1){
            rawMessage = rawMessage.substring(1);
            channel = lunaChatAPI.getChannel(globalChannel);
        }else{
            channel = lunaChatAPI.getDefaultChannel(player.getName());
        }
        if(rawMessage.startsWith(noneJapanizeMarker) && rawMessage.length() > 1){
            rawMessage = rawMessage.substring(1);
            japanized = false;
        }else{
            japanized = LunaChatUtil.getUserJapanized(player.getName(),lunaChatAPI);
        }
        if(!configurationManager.getSyncChannels().contains(channel.getName())){
            return;
        }
        User user = lpApi.getUserManager().loadUser(player.getUniqueId()).join();
        if(user != null){
            CachedMetaData metaData = user.getCachedData().getMetaData();
            prefix = metaData.getPrefix();
            suffix = metaData.getSuffix();
        }
        String message = LunaChatUtil.getFormattedMessage(channel,player.getName(),rawMessage,prefix,suffix);
        messageBytes.writeUTF(message);
        messageBytes.writeUTF(channel.getName());
        messageBytes.writeUTF(rawMessage);
        messageBytes.writeUTF(player.getName());
        messageBytes.writeUTF(Objects.requireNonNullElse(prefix, ""));
        messageBytes.writeUTF(Objects.requireNonNullElse(suffix, ""));
        messageBytes.writeUTF(japanized ? "yes" : "no");
        debugLog = "プレイヤー名: " + player.getName() + ", プレイヤーUUID: " + player.getUniqueId() + ", メッセージ: " + message + ", LunaChatチャンネル名: " + channel.getName() + ", Japanize変換: " + (japanized ? "有効" : "無効") + ", LuckPerms Prefix: " + prefix + ", LuckPerms Suffix: " + suffix;

        if(configurationManager.getDebugMode()){
            getLogger().info("[Debug] PluginMessageChannelにメッセージに転送しました: " + debugLog);
        }
        byte[] data = messageBytes.toByteArray();
        out.writeShort(data.length);
        out.write(data);

        event.getPlayer().sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    private void commandSendHelp(CommandSender sender){
        sender.sendMessage("§b=== LunaChatProxySync ===");
        sender.sendMessage("§b/lcps reload - 構成をリロードします");
        sender.sendMessage("§b/lcps help - このヘルプを表示します");
    }

    public void sendDebugMessage(String message){
        getLogger().info("[Debug] " + message);
    }
}