package com.namvuive.regionalbreakspeed.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {

    private final Plugin plugin;
    private final File configFile;
    private YamlConfiguration config;
    private boolean broken = false;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.config = new YamlConfiguration();
    }

    public void saveDefault() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
    }

    /**
     * @return {@code null} if the file was loaded successfully, otherwise a short description of the error
     */
    public String load() {
        YamlConfiguration newConfig = new YamlConfiguration();
        newConfig.options().parseComments(true);
        try {
            newConfig.load(configFile);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "config.yml has a syntax error and was not loaded:\n" + exception.getMessage(), exception);
            broken = true;
            return shortError(exception);
        }
        this.config = newConfig;
        broken = false;
        return null;
    }

    private static String shortError(Exception exception) {
        String message = exception.getMessage();
        String line = null;
        if (message != null) {
            for (String candidate : message.split("\\R")) {
                if (!candidate.isBlank()) {
                    line = candidate.trim();
                    break;
                }
            }
        }
        if (line == null) {
            line = exception.getClass().getSimpleName();
        }
        if (line.length() > 200) {
            line = line.substring(0, 200);
        }
        return line;
    }

    public boolean isBroken() {
        return broken;
    }

    public boolean save() {
        try {
            config.save(configFile);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save config.yml", exception);
            return false;
        }
    }

    public YamlConfiguration get() {
        return config;
    }

    public String findRegionKey(String name) {
        ConfigurationSection regions = config.getConfigurationSection("regions");
        if (regions == null) {
            return null;
        }
        for (String key : regions.getKeys(false)) {
            if (key.equalsIgnoreCase(name)) {
                return key;
            }
        }
        return null;
    }

    public List<String> getRegionKeys() {
        ConfigurationSection regions = config.getConfigurationSection("regions");
        if (regions == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(regions.getKeys(false));
    }
}
