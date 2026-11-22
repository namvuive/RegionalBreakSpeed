package com.namvuive.regionalbreakspeed.region;

import org.bukkit.World;

public sealed interface Area permits CuboidArea, WorldGuardArea {

    boolean contains(World world, int x, int y, int z);
}
