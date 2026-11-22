package com.namvuive.regionalbreakspeed.region;

import org.bukkit.World;

public record CuboidArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) implements Area {

    public static CuboidArea of(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new CuboidArea(
                Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    @Override
    public boolean contains(World world, int x, int y, int z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}
