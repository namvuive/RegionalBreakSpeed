package com.namvuive.regionalbreakspeed.region;

import com.namvuive.regionalbreakspeed.hook.WorldGuardHook;
import org.bukkit.World;

public record WorldGuardArea(String regionId, WorldGuardHook hook) implements Area {

    @Override
    public boolean contains(World world, int x, int y, int z) {
        return hook != null && hook.contains(world, regionId, x, y, z);
    }
}
