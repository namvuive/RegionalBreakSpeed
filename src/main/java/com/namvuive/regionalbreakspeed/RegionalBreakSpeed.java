package com.namvuive.regionalbreakspeed;

import com.namvuive.regionalbreakspeed.command.RgsCommand;
import com.namvuive.regionalbreakspeed.config.ConfigManager;
import com.namvuive.regionalbreakspeed.config.Messages;
import com.namvuive.regionalbreakspeed.hook.WorldEditHook;
import com.namvuive.regionalbreakspeed.hook.WorldGuardHook;
import com.namvuive.regionalbreakspeed.listener.BreakSpeedListener;
import com.namvuive.regionalbreakspeed.mmoitems.RegionalBreakSpeedGroupStat;
import com.namvuive.regionalbreakspeed.mmoitems.RegionalBreakSpeedLevelStat;
import com.namvuive.regionalbreakspeed.region.RegionRegistry;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class RegionalBreakSpeed extends JavaPlugin {

    private ConfigManager configManager;
    private Messages messages;
    private RegionRegistry registry;
    private WorldGuardHook worldGuardHook;
    private WorldEditHook worldEditHook;
    private BreakSpeedListener breakSpeedListener;

    @Override
    public void onLoad() {
        try {
            MMOItems.plugin.getStats().register(new RegionalBreakSpeedLevelStat());
            MMOItems.plugin.getStats().register(new RegionalBreakSpeedGroupStat());
        } catch (Throwable throwable) {
            getLogger().log(Level.SEVERE, "Failed to register MMOItems stat", throwable);
        }
    }

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        configManager = new ConfigManager(this);
        configManager.saveDefault();
        if (!new File(getDataFolder(), "messages.yml").exists()) {
            saveResource("messages.yml", false);
        }

        detectHooks();

        messages = new Messages(this);
        messages.load();

        registry = new RegionRegistry();
        String configError = configManager.load();
        registry.load(configManager.get(), getLogger(), worldGuardHook);
        if (configError != null) {
            getLogger().severe(
                    "Running with no regions until config.yml is fixed and /rgs reload is executed.");
        }

        breakSpeedListener = new BreakSpeedListener(this);
        getServer().getPluginManager().registerEvents(breakSpeedListener, this);

        RgsCommand command = new RgsCommand(this);
        if (getCommand("regionalbreakspeed") != null) {
            getCommand("regionalbreakspeed").setExecutor(command);
            getCommand("regionalbreakspeed").setTabCompleter(command);
        }
    }

    @Override
    public void onDisable() {
        if (breakSpeedListener != null) {
            breakSpeedListener.resetAll();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            resetSpeed(player);
        }
    }

    private void detectHooks() {
        PluginManager pluginManager = Bukkit.getPluginManager();

        Plugin worldGuardPlugin = pluginManager.getPlugin("WorldGuard");
        boolean hasWorldGuard = worldGuardPlugin != null && worldGuardPlugin.isEnabled();

        Plugin worldEditPlugin = pluginManager.getPlugin("WorldEdit");
        if (worldEditPlugin == null) {
            worldEditPlugin = pluginManager.getPlugin("FastAsyncWorldEdit");
        }
        boolean hasWorldEdit = worldEditPlugin != null && worldEditPlugin.isEnabled();

        if (hasWorldGuard && hasWorldEdit) {
            try {
                worldGuardHook = new WorldGuardHook();
            } catch (Throwable throwable) {
                getLogger().log(Level.WARNING, "Failed to hook into WorldGuard", throwable);
                worldGuardHook = null;
            }
        }

        if (hasWorldEdit) {
            try {
                worldEditHook = new WorldEditHook();
            } catch (Throwable throwable) {
                getLogger().log(Level.WARNING, "Failed to hook into WorldEdit", throwable);
                worldEditHook = null;
            }
        }

        getLogger().info("WorldGuard hook: " + (worldGuardHook != null ? "enabled" : "disabled"));
        getLogger().info("WorldEdit hook: "
                + (worldEditHook != null ? "enabled (" + worldEditPlugin.getName() + ")" : "disabled"));
    }

    public static void resetSpeed(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (instance != null) {
            instance.setBaseValue(instance.getDefaultValue());
        }
    }

    public RegionRegistry getRegistry() {
        return registry;
    }

    public Messages getMessages() {
        return messages;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public WorldEditHook getWorldEditHook() {
        return worldEditHook;
    }

    public BreakSpeedListener getBreakSpeedListener() {
        return breakSpeedListener;
    }
}
