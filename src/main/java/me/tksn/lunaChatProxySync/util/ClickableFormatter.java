// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync.util;

import com.github.ucchyocean.lc3.channel.Channel;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClickableFormatter {
    private final String urlRegex;
    private final Pattern urlPattern;
    private final Pattern firstPattern;
    public ClickableFormatter() {
        this.urlRegex = "(?i)((§[0-9a-fk-or])?h(§[0-9a-fk-or])?t(§[0-9a-fk-or])?t(§[0-9a-fk-or])?p(§[0-9a-fk-or])?s?(§[0-9a-fk-or])?:/(§[0-9a-fk-or])?/(§[0-9a-fk-or])?\\S+)";
        this.urlPattern = Pattern.compile(urlRegex);
        this.firstPattern = Pattern.compile("(?i)(%prefix|%suffix|%color|%ch|%username|%displayname|%msg)");
    }

    private TextComponent convertText(String text){
        return new TextComponent(TextComponent.fromLegacyText(text));
    }

    private List<String> splittedText(String text){
        Matcher matcher = urlPattern.matcher(text);
        List<String> splitted = new ArrayList<>(List.of());
        int n = 0;
        while (matcher.find()){
            if(n == 0 && text.startsWith(matcher.group())) {
                splitted.add(matcher.group());
                String notMatched = text.substring(n, matcher.start());
                if (!notMatched.isEmpty()) {
                    splitted.add(notMatched);
                }
            }else{
                String notMatched = text.substring(n, matcher.start());
                if (!notMatched.isEmpty()) {
                    splitted.add(notMatched);
                }
                splitted.add(matcher.group());
            }
            n = matcher.end();
        }

        if(n < text.length()){
            splitted.add(text.substring(n));
        }

        return splitted;
    }

    public ComponentBuilder getFirstComponent(Channel channel, String sender, String rawMessage, @Nullable String prefix, @Nullable String suffix){
        String format = channel.getFormat();
        if (format == null || format.isEmpty()) {
            format = "%prefix&7[&b{channel}&7] &f{sender}&7: &f{message}%suffix";
        }
        String displayPrefix = (prefix != null && !prefix.isEmpty()) ?
                ChatColor.translateAlternateColorCodes('&', prefix) : "";
        String displaySuffix = (suffix != null && !suffix.isEmpty()) ?
                " " + ChatColor.translateAlternateColorCodes('&', suffix) : "";
        Matcher matcher = firstPattern.matcher(format);
        List<String> splitted = new ArrayList<>(List.of());
        int n = 0;
        while (matcher.find()){
            if(n == 0 && format.startsWith(matcher.group())) {
                splitted.add(matcher.group());
                String notMatched = format.substring(n, matcher.start());
                if (!notMatched.isEmpty()) {
                    splitted.add(notMatched);
                }
            }else{
                String notMatched = format.substring(n, matcher.start());
                if (!notMatched.isEmpty()) {
                    splitted.add(notMatched);
                }
                splitted.add(matcher.group());
            }
            n = matcher.end();
        }

        if(n < format.length()) {
            splitted.add(format.substring(n));
        }
        ComponentBuilder builder = new ComponentBuilder();

        int splittedSize = splitted.size();
        for(int i = 0; i < splittedSize; i++){
            String currentString = splitted.get(i);
            if(firstPattern.matcher(currentString).matches()){
                if(currentString.equals("%ch")) {
                    builder.append(convertText(PublishMessage.translateColorCode(channel.getName())), ComponentBuilder.FormatRetention.ALL).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ch " + trimColorCode(channel.getName()))).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("発言先を" + trimColorCode(channel.getName()) + "にする")));
                }else if(currentString.equals("%username") || currentString.equals("%displayname")){
                    builder.append(convertText(sender), ComponentBuilder.FormatRetention.ALL).event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + trimColorCode(sender))).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(trimColorCode(sender) + "にプライベートメッセージを送る")));
                }else if(currentString.equals("%msg")){
                    List<String> splittedMsgString = splittedText(PublishMessage.translateColorCode(rawMessage));
                    int splittedMsgSize = splittedMsgString.size();
                    for(int c = 0; c < splittedMsgSize; c++){
                        String currentMsgString = splittedMsgString.get(c);
                        if(urlPattern.matcher(currentMsgString).matches()){
                            builder.append(convertText(currentMsgString), ComponentBuilder.FormatRetention.ALL).event(new ClickEvent(ClickEvent.Action.OPEN_URL, trimColorCode(currentMsgString))).event((HoverEvent) null);
                        }else{
                            builder.append(convertText(currentMsgString), ComponentBuilder.FormatRetention.ALL).event((ClickEvent) null).event((HoverEvent) null);
                        }
                    }
                }else if(currentString.equals("%prefix")){
                    builder.append(convertText(PublishMessage.translateColorCode(displayPrefix)), ComponentBuilder.FormatRetention.ALL).event((ClickEvent) null).event((HoverEvent) null);
                }else if(currentString.equals("%suffix")){
                    builder.append(convertText(PublishMessage.translateColorCode(displaySuffix)), ComponentBuilder.FormatRetention.ALL).event((ClickEvent) null).event((HoverEvent) null);
                }else if(currentString.equals("%color")){
                    builder.append(convertText(PublishMessage.translateColorCode(channel.getColorCode())), ComponentBuilder.FormatRetention.ALL).event((ClickEvent) null).event((HoverEvent) null);
                }
            }else{
                builder.append(convertText(PublishMessage.translateColorCode(currentString)), ComponentBuilder.FormatRetention.ALL).event((ClickEvent) null).event((HoverEvent) null);
            }
        }
        return builder;
    }

    public ComponentBuilder getPrivateComponent(String sender, String target, String rawMessage){
        ComponentBuilder builder = new ComponentBuilder();
        builder.append(convertText("§7["), ComponentBuilder.FormatRetention.ALL).event((ClickEvent) null).event((HoverEvent) null);
        builder.append(convertText(sender), ComponentBuilder.FormatRetention.ALL).event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + trimColorCode(sender))).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(trimColorCode(sender) + "にプライベートメッセージを送る")));
        builder.append(convertText("§f -> "), ComponentBuilder.FormatRetention.ALL).event((ClickEvent) null).event((HoverEvent) null);
        builder.append(convertText(target), ComponentBuilder.FormatRetention.ALL).event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + trimColorCode(target))).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(trimColorCode(target) + "にプライベートメッセージを送る")));
        builder.append(convertText("§f]§r "), ComponentBuilder.FormatRetention.ALL).event((ClickEvent) null).event((HoverEvent) null);
        List<String> splitted = splittedText(rawMessage);
        int splittedSize = splitted.size();
        for(int i = 0; i < splittedSize; i++){
            String currentString = splitted.get(i);
            if(firstPattern.matcher(currentString).matches()){
                builder.append(convertText(PublishMessage.translateColorCode(currentString)), ComponentBuilder.FormatRetention.ALL).event(new ClickEvent(ClickEvent.Action.OPEN_URL, trimColorCode(currentString))).event((HoverEvent) null);
            }else{
                builder.append(convertText(PublishMessage.translateColorCode(currentString)), ComponentBuilder.FormatRetention.ALL).event((ClickEvent) null).event((HoverEvent) null);
            }
        }
        return builder;
    }

    private static String trimColorCode(String string){
        return string.replaceAll("§([0-9A-Fa-fK-Ok-oRr])","");
    }
}
