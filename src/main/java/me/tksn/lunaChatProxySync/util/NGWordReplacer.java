// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hoxica Nexus Realm
package me.tksn.lunaChatProxySync.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NGWordReplacer {
    private final LunaChatConfigManager lunaChatConfigManager;
    private List<Pattern> ngWordPattern;

    public NGWordReplacer(LunaChatConfigManager lunaChatConfigManager) {
        this.lunaChatConfigManager = lunaChatConfigManager;
        this.ngWordPattern = this.lunaChatConfigManager.getNgWordPattern();
    }

    public String replaceNGWord(String target,String with){
        String result = target;
        for (Pattern p : ngWordPattern){
            Matcher m = p.matcher(result);
            if(m.find()){
                result = m.replaceAll(r ->
                        with.repeat(r.group().length())
                );
            }
        }
        return result;
    }

    public void reloadNGWordList(){
        ngWordPattern = lunaChatConfigManager.getNgWordPattern();
    }
}
