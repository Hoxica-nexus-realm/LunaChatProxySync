// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync.util;

import me.tksn.lunaChatProxySync.LunaChatProxySync;
import org.bukkit.configuration.file.FileConfiguration;
import com.github.ucchyocean.lc3.japanize.JapanizeType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LunaChatConfigManager {
    private final LunaChatProxySync plugin;
    private FileConfiguration config;
    private final Plugin lunaChat;
    private String japanizeTypeString;
    private List<String> ngWordPatternString;
    private List<Pattern> ngWordPattern = new ArrayList<>();
    private JapanizeType japanizeType;
    private String globalMarker;
    private String globalChannel;
    private String noneJapanizeMarker;

    private final ConfigurationManager configurationManager;
    private boolean debugMode;

    public LunaChatConfigManager(LunaChatProxySync plugin, Plugin lunaChat, ConfigurationManager configurationManager) {
        this.plugin = plugin;
        this.lunaChat = lunaChat;
        this.configurationManager = configurationManager;
        loadLunaChatConfig();
    }

    private void loadLunaChatConfig(){
        config = lunaChat.getConfig();
        debugMode = configurationManager.getDebugMode();
        japanizeTypeString = config.getString("japanizeType","none").toLowerCase();
        if(japanizeTypeString.equals("kana")){
            japanizeType = JapanizeType.KANA;
        }else if(japanizeTypeString.equals("googleime")){
            japanizeType = JapanizeType.GOOGLE_IME;
        }else{
            japanizeType = JapanizeType.NONE;
        }
        ngWordPatternString = config.getStringList("ngword");
        globalMarker = config.getString("globalMarker");
        globalChannel = config.getString("globalChannel");
        noneJapanizeMarker = config.getString("noneJapanizeMarker");
        ngWordPattern = new ArrayList<>();
        if(debugMode) {
            plugin.sendDebugMessage("グローバルマーカーは\"" + globalMarker + "\"です");
            plugin.sendDebugMessage("グローバルチャンネル名は\"" + globalChannel + "\"です");
            plugin.sendDebugMessage("かな変換タイプは\"" + japanizeTypeString + "\"です");
            plugin.sendDebugMessage("ノンジャパナイズマーカーは\"" + noneJapanizeMarker + "\"です");
            plugin.sendDebugMessage(ngWordPatternString.size() + "個のNGワードパターンを検出しました:");
            int ngWordsizeLoop;
            if(ngWordPatternString.size() > 10){
                ngWordsizeLoop = 10;
            }else{
                ngWordsizeLoop = ngWordPatternString.size();
            }
            for (int i = 0; i < ngWordsizeLoop; i++) {
                plugin.sendDebugMessage("パターン" + (i + 1) + ": \"" + ngWordPatternString.get(i) + "\"");
            }
            if((ngWordPatternString.size() - ngWordsizeLoop) > 0){
                plugin.sendDebugMessage("の他" + (ngWordPatternString.size() - ngWordsizeLoop) + "個のNGワードパターン");
            }
        }
        ngWordPatternString.forEach(s -> {
            ngWordPattern.add(Pattern.compile(s));
            if(debugMode){
                plugin.sendDebugMessage("正規表現パターン\"" + s +"\"をコンパイルしています...");
            }
        });
    }

    public void lunaChatConfigReload(){
        loadLunaChatConfig();
    }

    public JapanizeType getJapanizeType() {
        return japanizeType;
    }

    public List<Pattern> getNgWordPattern() {
        return ngWordPattern;
    }

    public String getGlobalMarker(){
        return globalMarker;
    }

    public String getGlobalChannel(){
        return globalChannel;
    }

    public String getNoneJapanizeMarker(){
        return noneJapanizeMarker;
    }
}
