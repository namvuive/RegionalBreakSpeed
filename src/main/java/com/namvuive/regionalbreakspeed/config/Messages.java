package com.namvuive.regionalbreakspeed.config;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class Messages {

    private final Plugin plugin;
    private final File messagesFile;
    private YamlConfiguration messages = new YamlConfiguration();
    private YamlConfiguration defaults = new YamlConfiguration();

    public Messages(Plugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
    }

    public void load() {
        this.defaults = loadDefaults();
        YamlConfiguration loaded = new YamlConfiguration();
        loaded.setDefaults(defaults);
        if (messagesFile.exists()) {
            try {
                loaded.load(messagesFile);
            } catch (IOException | InvalidConfigurationException exception) {
                plugin.getLogger().log(Level.WARNING,
                        "messages.yml has a syntax error, using defaults:\n" + exception.getMessage(), exception);
                loaded = new YamlConfiguration();
                loaded.setDefaults(defaults);
            }
        }
        this.messages = loaded;
    }

    private YamlConfiguration loadDefaults() {
        InputStream input = plugin.getResource("messages.yml");
        if (input == null) {
            return new YamlConfiguration();
        }
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to read bundled messages.yml", exception);
            return new YamlConfiguration();
        }
    }

    public String get(String key, String... placeholders) {
        String raw = messages.getString(key);
        if (raw == null) {
            raw = defaults.getString(key);
        }
        if (raw == null) {
            raw = key;
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public void send(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(get("prefix") + get(key, placeholders));
    }

    public void sendRaw(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    public String helpDescription(String sub) {
        return get("help-entries." + sub);
    }
}
