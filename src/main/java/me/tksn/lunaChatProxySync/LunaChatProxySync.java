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
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public final class LunaChatProxySync extends JavaPlugin implements PluginMessageListener{
    private ConfigurationManager configurationManager;
    private LunaChatAPI lunaChatAPI;
    private LunaChatConfigManager lunaChatConfigManager;
    private LuckPermsUtil luckPermsUtil;
    private JapanizeType japanizeType;
    private NGWordReplacer ngWordReplacer;
    private String globalChannel;
    private String globalMarker;
    private String noneJapanizeMarker;
    private boolean isLuckPermsAvailable;
    private ClickableFormatter clickableFormatter;
    private final String[] directMessageCommandsArray = {"tell","t","message","msg","m","reply","r"};
    private final List<String> directMessageCommands = Arrays.asList(directMessageCommandsArray);
    private List<String> proxyPlayers = new ArrayList<>();
    private CommandTabCompleter commandTabCompleter;
    private ReplyManager replyManager;
    @Override
    public void onEnable(){
        Plugin lc = getServer().getPluginManager().getPlugin("LunaChat");
        if(lc == null || !lc.isEnabled()){
            getLogger().warning("LunaChatが検出されないまたは無効のためプラグインは有効になりません");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        this.lunaChatAPI = LunaChatUtil.getLunaChatAPI();
        this.configurationManager = new ConfigurationManager(this,lc);
        Plugin lp = getServer().getPluginManager().getPlugin("LuckPerms");
        if(lp == null || !lp.isEnabled()){
            this.isLuckPermsAvailable = false;
            getLogger().info("LuckPermsが検出されないまたは無効のため一部機能は利用できません");
        }else{
            getLogger().info("LuckPermsを検出しました");
            this.isLuckPermsAvailable = true;
            this.luckPermsUtil = new LuckPermsUtil(this,configurationManager);
        }
        getServer().getPluginManager().registerEvents(new ChatListener(this,this.configurationManager), this);
        getServer().getPluginManager().registerEvents(new DirectMessageCommandListener(this,this.configurationManager), this);
        this.lunaChatConfigManager = new LunaChatConfigManager(this,lc, configurationManager);
        this.japanizeType = lunaChatConfigManager.getJapanizeType();
        this.ngWordReplacer = new NGWordReplacer(this.lunaChatConfigManager);
        this.globalChannel = this.lunaChatConfigManager.getGlobalChannel();
        this.globalMarker = this.lunaChatConfigManager.getGlobalMarker();
        this.noneJapanizeMarker = this.lunaChatConfigManager.getNoneJapanizeMarker();
        this.clickableFormatter = new ClickableFormatter();
        this.commandTabCompleter = new CommandTabCompleter(this,configurationManager);
        this.replyManager = new ReplyManager(this,configurationManager);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this,this.configurationManager,this.replyManager),this);
        getServer().getScheduler().runTaskTimer(this, () -> {
            Player player = getServer().getOnlinePlayers().stream().findAny().orElse(null);
            if(player != null){
                requestProxyPlayerList(player);
            }
        },20L, 300L);
        if(configurationManager.getDebugMode()) {
            getLogger().info("[Debug] 定期実行タスクを登録しました");
            getLogger().info("[Debug] 【タスク詳細】開始: 20Tick (1Sec)後, 間隔: 300Tick(15Sec)ごと, 内容: プロキシ上のプレイヤーの取得");
        }
        getServer().getScheduler().runTask(this, () -> {
            for (String directMessageCommand : directMessageCommands) {
                PluginCommand currentCommand = getCommand(directMessageCommand);
                if (currentCommand != null) {
                    currentCommand.setTabCompleter(commandTabCompleter);
                    if (configurationManager.getDebugMode()) {
                        getLogger().info("[Debug] コマンド\"" + currentCommand.getName() + "\"のタブ補完をセットしました");
                    }
                }
            }
            PluginCommand lcpsCommand = getCommand("lcps");
            if (lcpsCommand != null) {
                lcpsCommand.setTabCompleter(commandTabCompleter);
                if (configurationManager.getDebugMode()) {
                    getLogger().info("[Debug] コマンド\"" + lcpsCommand.getName() + "\"のタブ補完をセットしました");
                }
            }
        });
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
            PublishMessage.publish(configurationManager, lunaChatAPI, lunaChatConfigManager, channelName, playerName, rawMessage, prefix, suffix, this, userJapanized, japanizeType, ngWordReplacer, clickableFormatter);
        }else if(subchannel.equals("LunaChatProxySyncPrivate")){
            if(configurationManager.getDebugMode()){
                getLogger().info("[Debug] PluginMessageのサブチャンネル(プライベート用)が一致しました");
            }
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);

            ByteArrayDataInput msgin = ByteStreams.newDataInput(msgbytes);
            String sender = msgin.readUTF();
            String target = msgin.readUTF();
            String rawMessage = msgin.readUTF();
            boolean userJapanized = msgin.readUTF().equals("yes");
            if(configurationManager.getDebugMode()){
                getLogger().info("[Debug] 【受信内容】送信元: "+ sender + ", 送信先: " + target + ", メッセージ: " + rawMessage + "japanize変換: " + (userJapanized ? "有効" : "無効"));
            }
            PublishMessage.publishPrivate(configurationManager,lunaChatAPI,lunaChatConfigManager,sender,target,rawMessage,userJapanized,japanizeType,ngWordReplacer,this,clickableFormatter,false);
        }else if(subchannel.equals("PlayerList")){
            String server = in.readUTF();
            if(configurationManager.getDebugMode()){
                getLogger().info("[Debug] PluginMessageのサブチャンネル(プロキシ上のプレイヤーリスト用)が一致しました");
            }
            String rawPlayerList = in.readUTF();
            if (server.equals("ALL")) {
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    List<String> proxyPlayersList = List.of(rawPlayerList.split(", "));
                    Bukkit.getScheduler().runTask(this, () -> {
                        proxyPlayers = proxyPlayersList;
                    });
                    if (configurationManager.getDebugMode()) {
                        if(proxyPlayers.isEmpty()){
                            getLogger().info("[Debug] プロキシ上にオンラインのプレイヤーがいませんでした");
                            return;
                        }
                        String players = proxyPlayers.stream().limit(10).collect(Collectors.joining(", "));
                        if(proxyPlayers.size() > 10){
                            int otherPlayersSize = proxyPlayers.size() - 10;
                            getLogger().info("[Debug] " + players + "の他" + otherPlayersSize + "人, 合計 " + proxyPlayers.size() + "人のプレイヤーを取得しました");
                        }else{
                            getLogger().info("[Debug] " + players + ", 合計" + proxyPlayers.size() + "人のプレイヤーを取得しました");
                        }

                    }
                });
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(command.getName().equalsIgnoreCase("reply") || command.getName().equalsIgnoreCase("r")){
            String currentReplyTarget = replyManager.getCurrentReplyTarget(sender.getName());
            if(args.length < 1) {
                if (!replyManager.isContainCurrentReplyTarget(sender.getName())) {
                    sender.sendMessage("§a現在の会話相手はいません");
                }else{
                    if(currentReplyTarget != null){
                        sender.sendMessage("§a現在の会話相手は" + currentReplyTarget + "です");
                    }else{
                        sender.sendMessage("§a現在の会話相手はいません");
                    }
                }
                return true;
            }else{
                if(replyManager.isContainCurrentReplyTarget(sender.getName()) && currentReplyTarget != null){
                    int n = args.length;
                    StringBuilder rawMessage = new StringBuilder();
                    for(int i = 0; i < n; i++){
                        rawMessage.append(args[i]).append(" ");
                    }
                    String message = rawMessage.toString();
                    boolean japanized = lunaChatAPI.isPlayerJapanize(sender.getName()) && !message.startsWith(noneJapanizeMarker);
                    if(message.startsWith(noneJapanizeMarker) && message.length() > 1){
                        message = message.substring(1);
                    }
                    Player targetPlayer = getServer().getPlayer(currentReplyTarget);
                    Player senderPlayer = getServer().getPlayer(sender.getName());
                    if(targetPlayer != null && targetPlayer.isOnline() && senderPlayer != null && senderPlayer.isOnline()){
                        senderPlayer.performCommand("lunachat:" + command.getName() + " " + targetPlayer.getName() + " " + rawMessage);
                        replyManager.setCurrentReplyTarget(senderPlayer.getName(),targetPlayer.getName());
                        return true;
                    }else {
                        if(senderPlayer == null){
                            return true;
                        }
                        requestProxyPlayerList(senderPlayer);
                        if(!proxyPlayers.contains(currentReplyTarget)){
                            sender.sendMessage("§cプレイヤー" + currentReplyTarget + "が見つかりません");
                            return true;
                        }
                        forwardPrivateMessage(getServer().getPlayer(sender.getName()), currentReplyTarget, rawMessage.toString());
                        PublishMessage.publishPrivate(configurationManager, lunaChatAPI, lunaChatConfigManager, sender.getName(), currentReplyTarget, message, japanized, japanizeType, ngWordReplacer, this, clickableFormatter, true);
                        replyManager.setCurrentReplyTarget(senderPlayer.getName(),currentReplyTarget);
                        return true;
                    }
                }else{
                    sender.sendMessage("§a現在の会話相手はいません");
                    return true;
                }
            }
        }else if(args.length < 2 && directMessageCommands.contains(command.getName())){
            sendDirectMessageCommandHelp(getServer().getPlayer(sender.getName()), command.getName().toLowerCase());
            return true;
        }else if(args.length == 0){
            commandSendHelp(sender);
            return true;
        }
        String commandName = command.getName();
        String subCmd = args[0].toLowerCase();
        if(subCmd.equals("reload")){
            configurationManager.asyncReloadConfig(lunaChatConfigManager,ngWordReplacer);
            japanizeType = lunaChatConfigManager.getJapanizeType();
            replyManager.reloadConfiguration();
            sender.sendMessage("§b設定を非同期でリロードしました");
            if(isLuckPermsAvailable){
                luckPermsUtil.updateDebugStatus(configurationManager);
            }
            return true;
        }else if(subCmd.equals("help")){
            commandSendHelp(sender);
            return true;
        }else if(commandName.equals("message") || commandName.equals("msg") || commandName.equals("m") || commandName.equals("tell") || commandName.equals("t")){
            int n = args.length;
            StringBuilder rawMessage = new StringBuilder();
            for(int i = 1; i < n; i++){
                rawMessage.append(args[i]).append(" ");
            }
            String message = rawMessage.toString();
            boolean japanized = lunaChatAPI.isPlayerJapanize(sender.getName()) && !message.startsWith(noneJapanizeMarker);
            if(message.startsWith(noneJapanizeMarker) && message.length() > 1){
                message = message.substring(1);
            }
            Player targetPlayer = getServer().getPlayer(args[0]);
            Player senderPlayer = getServer().getPlayer(sender.getName());
            if(targetPlayer != null && targetPlayer.isOnline() && senderPlayer != null && senderPlayer.isOnline()){
                senderPlayer.performCommand("lunachat:" + command.getName() + " " + args[0] + " " + rawMessage);
                replyManager.setCurrentReplyTarget(senderPlayer.getName(),targetPlayer.getName());
                return true;
            }else {
                if(senderPlayer == null){
                    return true;
                }
                requestProxyPlayerList(senderPlayer);
                if(!proxyPlayers.contains(args[0])){
                    sender.sendMessage("§cプレイヤー" + args[0] + "が見つかりません");
                    return true;
                }
                forwardPrivateMessage(getServer().getPlayer(sender.getName()), args[0], rawMessage.toString());
                PublishMessage.publishPrivate(configurationManager, lunaChatAPI, lunaChatConfigManager, sender.getName(), args[0], message, japanized, japanizeType, ngWordReplacer, this, clickableFormatter, true);
                replyManager.setCurrentReplyTarget(senderPlayer.getName(),args[0]);
                return true;
            }
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
        if(isLuckPermsAvailable){
            prefix = luckPermsUtil.getUserPrefix(event.getPlayer());
            suffix = luckPermsUtil.getUserSuffix(event.getPlayer());
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

    private void forwardPrivateMessage(Player player, String target, String message){
        if(configurationManager.getDebugMode()){
            getLogger().info("[Debug] プライベートチャットを受信しました");
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("LunaChatProxySyncPrivate");

        ByteArrayDataOutput messageBytes = ByteStreams.newDataOutput();
        String debugLog;
        boolean japanized = lunaChatAPI.isPlayerJapanize(player.getName()) && !message.startsWith(noneJapanizeMarker);
        if(message.startsWith(noneJapanizeMarker) && message.length() > 1){
            message = message.substring(1);
        }
        messageBytes.writeUTF(player.getName());
        messageBytes.writeUTF(target);
        messageBytes.writeUTF(message);
        messageBytes.writeUTF(japanized ? "yes" : "no");
        if(player != null && player.isOnline()){
            messageBytes.writeUTF(player.getUniqueId().toString());
        }else{
            messageBytes.writeUTF("");
        }
        debugLog = "プレイヤー名: " + player.getName() + ", プレイヤーUUID: " + (player != null ? player.getUniqueId() : "null") + ", メッセージ: " + message + ", 送信先: " + target + ", Japanize変換: " + (japanized ? "有効" : "無効");

        byte[] data = messageBytes.toByteArray();
        out.writeShort(data.length);
        out.write(data);

        if(player != null && player.isOnline()) {
            player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
            if(configurationManager.getDebugMode()){
                getLogger().info("[Debug] PluginMessageChannel(プライベートチャット用)にメッセージに転送しました: " + debugLog);
            }
        }else{
            if(configurationManager.getDebugMode()){
                getLogger().info("[Debug] PlayerがnullであるためPluginMessageChannel(プライベートチャット用)に転送できませんでした");
            }
        }
    }

    private void commandSendHelp(CommandSender sender){
        sender.sendMessage("§b=== LunaChatProxySync ===");
        sender.sendMessage("§b/lcps reload - 構成をリロードします");
        sender.sendMessage("§b/lcps help - このヘルプを表示します");
    }

    private void sendDirectMessageCommandHelp(Player sender, String commandName){
        if(commandName.equals("message") || commandName.equals("msg") || commandName.equals("m")){
            sender.sendMessage("§a使用法: /lunachatproxysync:" + commandName + " (target) [message]");
        }else if(commandName.equals("tell") || commandName.equals("t")){
            sender.sendMessage("§a使用法: /lunachatproxysync:" + commandName + " (target) [message]");
        }
    }

    public void directMessageCommand(PlayerCommandPreprocessEvent event){
        String[] commands = event.getMessage().split(" ");
        String command = commands[0].toLowerCase().substring(1);
        String[] args;
        Player player = event.getPlayer();
        Player target;
        boolean isDebugMode = configurationManager.getDebugMode();
        if(isDebugMode){
            sendDebugMessage("コマンド: " + event.getMessage() + ", 実行者: " + player.getName());
        }
        if(commands.length > 1) {
            args = Arrays.copyOfRange(commands, 1, commands.length);
            target = getServer().getPlayer(args[0]);
        }else{
            args = new String[]{};
            target = null;
        }
        if(directMessageCommands.contains(command)){
            event.setCancelled(true);
            if(isDebugMode){
                sendDebugMessage("PlayerCommandPreprocessEventはキャンセルされました");
            }
            if(command.equals("reply") || command.equals("r")){
                String currentReplyTarget = replyManager.getCurrentReplyTarget(player.getName());
                if(args.length < 1) {
                    if(!replyManager.isContainCurrentReplyTarget(event.getPlayer().getName())){
                        player.sendMessage("§a現在の会話相手はいません");
                        return;
                    }else{
                        if(currentReplyTarget != null){
                            player.sendMessage("§a現在の会話相手は" + currentReplyTarget + "です");
                            return;
                        }else{
                            player.sendMessage("§a現在の会話相手はいません");
                            return;
                        }
                    }
                }else{
                    if(replyManager.isContainCurrentReplyTarget(player.getName()) && currentReplyTarget != null){
                        int n = args.length;
                        StringBuilder rawMessage = new StringBuilder();
                        for(int i = 0; i < n; i++){
                            rawMessage.append(args[i]).append(" ");
                        }
                        String message = rawMessage.toString();
                        boolean japanized = lunaChatAPI.isPlayerJapanize(player.getName()) && !message.startsWith(noneJapanizeMarker);
                        if(message.startsWith(noneJapanizeMarker) && message.length() > 1){
                            message = message.substring(1);
                        }
                        Player targetPlayer = getServer().getPlayer(currentReplyTarget);
                        Player senderPlayer = getServer().getPlayer(player.getName());
                        if(targetPlayer != null && targetPlayer.isOnline() && senderPlayer != null && senderPlayer.isOnline()){
                            senderPlayer.performCommand("lunachat:" + command + " " + targetPlayer.getName() + " " + rawMessage);
                            replyManager.setCurrentReplyTarget(senderPlayer.getName(),targetPlayer.getName());
                            return;
                        }else {
                            if(senderPlayer == null){
                                return;
                            }
                            requestProxyPlayerList(senderPlayer);
                            if(!proxyPlayers.contains(currentReplyTarget)){
                                player.sendMessage("§cプレイヤー" + currentReplyTarget + "が見つかりません");
                                return;
                            }
                            forwardPrivateMessage(getServer().getPlayer(player.getName()), currentReplyTarget, rawMessage.toString());
                            PublishMessage.publishPrivate(configurationManager, lunaChatAPI, lunaChatConfigManager, player.getName(), currentReplyTarget, message, japanized, japanizeType, ngWordReplacer, this, clickableFormatter, true);
                            replyManager.setCurrentReplyTarget(senderPlayer.getName(),currentReplyTarget);
                            return;
                        }
                    }else{
                        player.sendMessage("§a現在の会話相手はいません");
                        return;
                    }
                }
            }
            if(args.length < 2){
                sendDirectMessageCommandHelp(player, command);
                return;
            }
            int n = args.length;
            StringBuilder rawMessage = new StringBuilder();
            for(int i = 1; i < n; i++){
                rawMessage.append(args[i]).append(" ");
            }
            if(target != null && target.isOnline()){
                player.performCommand("lunachat:" + command + " " + target.getName() + " " + rawMessage);
                replyManager.setCurrentReplyTarget(player.getName(),args[0]);
            }else{
                requestProxyPlayerList(event.getPlayer());
                if(!proxyPlayers.contains(args[0])){
                    event.getPlayer().sendMessage("§cプレイヤー" + args[0] + "が見つかりません");
                    return;
                }
                String message = rawMessage.toString();
                boolean japanized = lunaChatAPI.isPlayerJapanize(player.getName()) && !message.startsWith(noneJapanizeMarker);
                if(message.startsWith(noneJapanizeMarker) && message.length() > 1){
                    message = message.substring(1);
                }
                PublishMessage.publishPrivate(configurationManager,lunaChatAPI,lunaChatConfigManager,player.getName(),args[0],message,japanized,japanizeType,ngWordReplacer,this,clickableFormatter,true);
                forwardPrivateMessage(player, args[0], rawMessage.toString());
                replyManager.setCurrentReplyTarget(player.getName(),args[0]);
            }
        }
    }

    public void requestProxyPlayerList(Player player){
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerList");
        out.writeUTF("ALL");

        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    public List<String> getProxyPlayers(){
        return proxyPlayers;
    }

    public List<String> getDirectMessageCommands(){
        return directMessageCommands;
    }

    public void sendDebugMessage(String message){
        getLogger().info("[Debug] " + message);
    }
}