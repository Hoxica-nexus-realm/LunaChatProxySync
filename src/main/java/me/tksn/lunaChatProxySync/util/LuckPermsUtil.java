package me.tksn.lunaChatProxySync.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;

public class LuckPermsUtil {
    private final JavaPlugin plugin;
    private LuckPerms lpAPI;
    private boolean isDebugMode;

    public LuckPermsUtil(JavaPlugin plugin,ConfigurationManager configurationManager){
        this.plugin = plugin;
        this.isDebugMode = configurationManager.getDebugMode();
        getLuckPerms();
    }

    public void updateDebugStatus(ConfigurationManager configurationManager){
        isDebugMode = configurationManager.getDebugMode();
    }

    private void getLuckPerms(){
        Plugin lp = Bukkit.getPluginManager().getPlugin("LuckPerms");
        if(lp == null || !lp.isEnabled()){
            return;
        }
        if(isDebugMode){
            plugin.getLogger().info("[Debug] LuckPermsを取得しています...");
        }

        try {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if(provider != null){
                lpAPI = provider.getProvider();
                if(isDebugMode){
                    plugin.getLogger().info("[Debug] LuckPermsを取得しました");
                }
            }else{
                lpAPI = null;
                if(isDebugMode) {
                    plugin.getLogger().info("[Debug] LuckPermsの取得に失敗しました");
                }
            }
        } catch (NoClassDefFoundError e) {
            if(isDebugMode) {
                plugin.getLogger().info("[Debug] LuckPermsの取得に失敗しました");
            }
            plugin.getLogger().warning(e.getMessage());
        }
    }

    private CachedMetaData getCachedMetaData(User user){
        return user.getCachedData().getMetaData();
    }

    private User getUser(Player player){
        if(lpAPI != null) {
            return lpAPI.getUserManager().loadUser(player.getUniqueId()).join();
        }else{
            return null;
        }
    }

    public String getUserPrefix(Player player){
        User user = getUser(player);
        if(user != null) {
            return getCachedMetaData(user).getPrefix();
        }else{
            return null;
        }
    }

    public String getUserSuffix(Player player){
        User user = getUser(player);
        if(user != null){
            return getCachedMetaData(user).getSuffix();
        }else{
            return null;
        }
    }
}
