package com.namvuive.regionalbreakspeed.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class WorldGuardHook {

    private RegionManager regionManager(World world) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
    }

    public boolean regionExists(World world, String id) {
        RegionManager manager = regionManager(world);
        return manager != null && manager.getRegion(id) != null;
    }

    public boolean contains(World world, String id, int x, int y, int z) {
        RegionManager manager = regionManager(world);
        if (manager == null) {
            return false;
        }
        ProtectedRegion region = manager.getRegion(id);
        return region != null && region.contains(x, y, z);
    }

    public String getRegionId(World world, String id) {
        RegionManager manager = regionManager(world);
        if (manager == null) {
            return null;
        }
        ProtectedRegion region = manager.getRegion(id);
        return region == null ? null : region.getId();
    }

    public List<String> getAllRegionKeys() {
        List<String> result = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = regionManager(world);
            if (manager == null) {
                continue;
            }
            for (String id : manager.getRegions().keySet()) {
                if (id.equals("__global__")) {
                    continue;
                }
                result.add(world.getName() + ":" + id);
            }
        }
        return result;
    }
}
