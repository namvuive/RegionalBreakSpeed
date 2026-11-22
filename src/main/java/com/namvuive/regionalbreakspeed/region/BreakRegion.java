package com.namvuive.regionalbreakspeed.region;


import java.util.Map;
import java.util.NavigableMap;

public record BreakRegion(
        String name,
        String worldName,
        int priority,
        int index,
        Area area,
        NavigableMap<Integer, Double> speeds,
        InactiveReason inactiveReason) {

    public boolean active() {
        return inactiveReason == null;
    }

    /**
     * @return the speed for the largest key that is less than or equal to {@code level},
     *         or {@code null} if no such key exists
     */
    public Double speedFor(int level) {
        Map.Entry<Integer, Double> entry = speeds.floorEntry(level);
        return entry == null ? null : entry.getValue();
    }
}
