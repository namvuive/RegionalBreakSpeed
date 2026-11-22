package com.namvuive.regionalbreakspeed.hook;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import org.bukkit.entity.Player;

public class WorldEditHook {

    public CuboidSelection getSelection(Player player) throws SelectionException {
        com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
        World weWorld = session.getSelectionWorld();
        if (weWorld == null) {
            throw new SelectionException(SelectionError.NONE_OR_INCOMPLETE);
        }

        Region region;
        try {
            region = session.getSelection(weWorld);
        } catch (IncompleteRegionException exception) {
            throw new SelectionException(SelectionError.NONE_OR_INCOMPLETE);
        }

        if (!(region instanceof CuboidRegion cuboid)) {
            throw new SelectionException(SelectionError.NOT_CUBOID);
        }

        BlockVector3 pos1 = cuboid.getPos1();
        BlockVector3 pos2 = cuboid.getPos2();
        String worldName = BukkitAdapter.adapt(weWorld).getName();
        return new CuboidSelection(
                worldName,
                pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());
    }

    public enum SelectionError {
        NONE_OR_INCOMPLETE,
        NOT_CUBOID
    }

    public static class SelectionException extends Exception {

        private final SelectionError error;

        public SelectionException(SelectionError error) {
            super(error.name());
            this.error = error;
        }

        public SelectionError getError() {
            return error;
        }
    }
}
