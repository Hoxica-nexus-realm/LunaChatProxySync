// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync.util;

import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.LunaChatBukkit;
import com.github.ucchyocean.lc3.channel.Channel;
import com.github.ucchyocean.lc3.japanize.JapanizeType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;


import javax.annotation.Nullable;

public class LunaChatUtil {
    public static LunaChatAPI getLunaChatAPI() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("LunaChat");
        if (plugin != null && plugin instanceof LunaChatBukkit) {
            return ((LunaChatBukkit) plugin).getLunaChatAPI();
        }
        return null;
    }
    public static String getFormattedMessage(Channel channel, String sender, String message, @Nullable String prefix, @Nullable String suffix){
        String format = channel.getFormat();
        if (format == null || format.isEmpty()) {
            format = "%prefix&7[&b{channel}&7] &f{sender}&7: &f{message}%suffix";
        }

        String displayPrefix = (prefix != null && !prefix.isEmpty()) ?
                ChatColor.translateAlternateColorCodes('&', prefix) : "";
        String displaySuffix = (suffix != null && !suffix.isEmpty()) ?
                " " + ChatColor.translateAlternateColorCodes('&', suffix) : "";

        String formattedMessage = format
                .replace("%prefix", displayPrefix)
                .replace("%suffix", displaySuffix)
                .replace("%color",channel.getColorCode())
                .replace("%ch", channel.getName())
                .replace("%username", sender)
                .replace("%msg", message)
                .replace("%displayname", sender);
        return formattedMessage;
    }

    public static String getJapanized(@NotNull String string, @NotNull JapanizeType japanizeType, @NotNull LunaChatAPI lunaChatAPI){
        if(japanizeType.toString().equalsIgnoreCase("kana")){
            return lunaChatAPI.japanize(string,japanizeType);
        } else if (japanizeType.toString().equalsIgnoreCase("googleime")) {
            return lunaChatAPI.japanize(string,japanizeType);
        } else {
            return null;
        }
    }

    public static boolean getUserJapanized(@NotNull String playerName, @NotNull LunaChatAPI lunaChatAPI){
        return lunaChatAPI.isPlayerJapanize(playerName);
    }
}
