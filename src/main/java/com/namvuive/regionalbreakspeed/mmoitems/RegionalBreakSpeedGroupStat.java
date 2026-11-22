package com.namvuive.regionalbreakspeed.mmoitems;

import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.type.StringStat;
import org.bukkit.Material;

public class RegionalBreakSpeedGroupStat extends StringStat {

    public static final String ID = "REGIONAL_BREAK_SPEED_GROUP";

    private static final String NAME_PATTERN = "[A-Za-z0-9_-]+";

    public RegionalBreakSpeedGroupStat() {
        super(ID, Material.NAME_TAG, "Regional Break Speed Group",
                new String[]{
                        "Group used by RegionalBreakSpeed to decide",
                        "which region break speeds apply to this tool.",
                        "Only letters, numbers, '_' and '-' are allowed."
                },
                new String[]{"tool"});
    }

    @Override
    public void whenInput(EditionInventory inv, String message, Object... info) {
        String trimmed = message.trim();
        if (!trimmed.matches(NAME_PATTERN)) {
            throw new IllegalArgumentException("Group name may only contain letters, numbers, '_' and '-'.");
        }
        super.whenInput(inv, trimmed, info);
    }

    @Override
    public void whenApplied(ItemStackBuilder item, StringData data) {
        item.addItemTag(getAppliedNBT(data));
    }
}
