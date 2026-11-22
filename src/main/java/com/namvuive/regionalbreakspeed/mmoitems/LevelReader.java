package com.namvuive.regionalbreakspeed.mmoitems;

import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public final class LevelReader {

    public static final String NBT_KEY = "MMOITEMS_REGIONAL_BREAK_SPEED_LEVEL";
    public static final String GROUP_NBT_KEY = "MMOITEMS_REGIONAL_BREAK_SPEED_GROUP";

    private LevelReader() {
    }

    /**
     * Parses the level and group from the item's NBT in a single pass.
     *
     * @return the tool data, never {@code null}; empty item yields level 0 and an empty group
     */
    public static ToolData read(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return new ToolData(0, "");
        }
        NBTItem nbt = NBTItem.get(item);
        int level = 0;
        if (nbt.hasTag(NBT_KEY)) {
            level = Math.max((int) Math.floor(nbt.getDouble(NBT_KEY)), 0);
        }
        String group = "";
        if (nbt.hasTag(GROUP_NBT_KEY)) {
            String raw = nbt.getString(GROUP_NBT_KEY);
            if (raw != null) {
                group = raw.trim().toLowerCase(Locale.ROOT);
            }
        }
        return new ToolData(level, group);
    }

    public record ToolData(int level, String group) {
    }
}
