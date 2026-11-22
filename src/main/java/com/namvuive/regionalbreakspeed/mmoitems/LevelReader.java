package com.namvuive.regionalbreakspeed.mmoitems;

import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.inventory.ItemStack;

public final class LevelReader {

    public static final String NBT_KEY = "MMOITEMS_REGIONAL_BREAK_SPEED_LEVEL";

    private LevelReader() {
    }

    public static int getLevel(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasTag(NBT_KEY)) {
            return 0;
        }
        int level = (int) Math.floor(nbt.getDouble(NBT_KEY));
        return Math.max(level, 0);
    }
}
