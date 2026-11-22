package com.namvuive.regionalbreakspeed.mmoitems;

import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.inventory.ItemStack;

public final class LevelReader {

    public static final String NBT_KEY = "MMOITEMS_REGIONAL_BREAK_SPEED_LEVEL";
    public static final String GROUP_NBT_KEY = "MMOITEMS_REGIONAL_BREAK_SPEED_GROUP";

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

    /**
     * @return the tool's configured group name, or an empty string if it has none
     */
    public static String getGroup(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasTag(GROUP_NBT_KEY)) {
            return "";
        }
        String group = nbt.getString(GROUP_NBT_KEY);
        if (group == null) {
            return "";
        }
        return group.trim();
    }
}
