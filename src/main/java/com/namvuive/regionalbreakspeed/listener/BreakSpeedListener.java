package com.namvuive.regionalbreakspeed.listener;

import com.namvuive.regionalbreakspeed.RegionalBreakSpeed;
import com.namvuive.regionalbreakspeed.mmoitems.LevelReader;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BreakSpeedListener implements Listener {

    private final RegionalBreakSpeed plugin;

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

        int level = LevelReader.getLevel(player.getInventory().getItemInMainHand());
        double speed = plugin.getRegistry().resolveSpeed(event.getBlock(), level);

        AttributeInstance instance = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (instance == null) {
            return;
        }
        if (Math.abs(instance.getBaseValue() - speed) < 1.0E-6) {
            return;
        }
        instance.setBaseValue(speed);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        RegionalBreakSpeed.resetSpeed(event.getPlayer());
    }
}
