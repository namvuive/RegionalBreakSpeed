package com.namvuive.regionalbreakspeed.listener;

import com.namvuive.regionalbreakspeed.RegionalBreakSpeed;
import com.namvuive.regionalbreakspeed.mmoitems.LevelReader;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BreakSpeedListener implements Listener {

    private final RegionalBreakSpeed plugin;
    private final Map<UUID, Double> appliedSpeeds = new HashMap<>();

    public BreakSpeedListener(RegionalBreakSpeed plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        GameMode gameMode = player.getGameMode();
        if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        LevelReader.ToolData tool = LevelReader.read(item);
        Double speed = plugin.getRegistry().resolveSpeed(event.getBlock(), tool.group(), tool.level());

        AttributeInstance instance = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (instance == null) {
            return;
        }

        if (speed == null) {
            if (appliedSpeeds.remove(player.getUniqueId()) != null) {
                instance.setBaseValue(instance.getDefaultValue());
            }
            return;
        }

        if (Math.abs(instance.getBaseValue() - speed) >= 1.0E-6) {
            instance.setBaseValue(speed);
        }
        appliedSpeeds.put(player.getUniqueId(), speed);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (appliedSpeeds.remove(player.getUniqueId()) != null) {
            RegionalBreakSpeed.resetSpeed(player);
        }
    }

    public void resetAll() {
        for (UUID id : new ArrayList<>(appliedSpeeds.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                RegionalBreakSpeed.resetSpeed(player);
            }
        }
        appliedSpeeds.clear();
    }
}
