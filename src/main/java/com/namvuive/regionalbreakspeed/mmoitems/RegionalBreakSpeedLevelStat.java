package com.namvuive.regionalbreakspeed.mmoitems;

import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.util.NumericStatFormula;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

public class RegionalBreakSpeedLevelStat extends DoubleStat {

    public static final String ID = "REGIONAL_BREAK_SPEED_LEVEL";

    public RegionalBreakSpeedLevelStat() {
        super(ID, Material.GOLDEN_PICKAXE, "Regional Break Speed Level",
                new String[]{
                        "Level used by RegionalBreakSpeed to set",
                        "block break speed inside configured regions.",
                        "Must be a whole number greater than 0."
                },
                new String[]{"tool"});
    }

    @Override
    public void whenInput(EditionInventory inv, String message, Object... info) {
        String trimmed = message.trim();
        int value;
        try {
            value = Integer.parseInt(trimmed);
        } catch (NumberFormatException exception) {
            value = -1;
        }
        if (!trimmed.matches("\\d+") || value <= 0) {
            throw new IllegalArgumentException("Value must be a whole number greater than 0.");
        }
        super.whenInput(inv, trimmed, info);
    }

    @Override
    public void whenApplied(ItemStackBuilder item, DoubleData data) {
        item.addItemTag(getAppliedNBT(data));
    }

    @Override
    public void whenPreviewed(ItemStackBuilder item, DoubleData data, NumericStatFormula formula) {
        whenApplied(item, data);
    }
}
