// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync.util;

import me.tksn.lunaChatProxySync.LunaChatProxySync;

import java.util.concurrent.ConcurrentHashMap;

public class ReplyManager {
    private final ConcurrentHashMap<String,String> replyHashMap;
    private ConfigurationManager configurationManager;
    private LunaChatProxySync plugin;
    private boolean isDebugMode;
    public ReplyManager(LunaChatProxySync plugin, ConfigurationManager configurationManager){
        this.replyHashMap = new ConcurrentHashMap<>();
        this.configurationManager = configurationManager;
        this.plugin = plugin;
        this.isDebugMode = configurationManager.getDebugMode();
    }

    public void reloadConfiguration(){
        this.isDebugMode = configurationManager.getDebugMode();
    }

    public String getCurrentReplyTarget(String playerName){
        if(replyHashMap.containsKey(playerName)){
            if(isDebugMode){
                plugin.sendDebugMessage("プレイヤー" + playerName + "の現在の会話相手は" + replyHashMap.get(playerName) + "です");
            }
            return replyHashMap.get(playerName);
        }else{
            return null;
        }
    }

    public void setCurrentReplyTarget(String playerName, String target){
        replyHashMap.remove(playerName);
        replyHashMap.put(playerName,target);
        if(isDebugMode){
            plugin.sendDebugMessage("プレイヤー" + playerName + "の現在の会話相手を" + target + "に変更しました");
        }
    }

    public void removeCurrentReplyTarget(String playerName){
        if(replyHashMap.containsKey(playerName)){
            replyHashMap.remove(playerName);
            if(isDebugMode){
                plugin.sendDebugMessage("プレイヤー" + playerName + "をreply-listから削除しました");
            }
        }
    }

    public boolean isContainCurrentReplyTarget(String playerName){
        if(playerName != null) {
            if (isDebugMode) {
                plugin.sendDebugMessage("プレイヤー" + playerName + "はreply-listに含まれていま" + (replyHashMap.containsKey(playerName) ? "す" : "せん"));
            }
            return replyHashMap.containsKey(playerName);
        }else{
            if (isDebugMode) {
                plugin.sendDebugMessage("プレイヤーはnullであるためreply-listに含まれていません");
            }
            return false;
        }

    }
}
