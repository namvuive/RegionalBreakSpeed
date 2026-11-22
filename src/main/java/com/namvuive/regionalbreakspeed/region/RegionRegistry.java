package com.namvuive.regionalbreakspeed.region;

import com.namvuive.regionalbreakspeed.hook.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class RegionRegistry {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    private double defaultSpeed = -1.0;
    private boolean fallThrough = false;
    private List<BreakRegion> recorded = new ArrayList<>();
    private Map<String, List<BreakRegion>> activeByWorld = new HashMap<>();

    public LoadResult load(YamlConfiguration cfg, Logger log, WorldGuardHook wg) {
        double newDefaultSpeed = -1.0;
        boolean newFallThrough = false;
        List<BreakRegion> newRecorded = new ArrayList<>();
        Map<String, List<BreakRegion>> newActiveByWorld = new HashMap<>();

        int active = 0;
        int inactive = 0;
        int skipped = 0;
        int activeCuboid = 0;
        int activeWorldGuard = 0;

        Object defaultObj = cfg.get("default-speed");
        if (defaultObj == null) {
            newDefaultSpeed = -1.0;
        } else if (defaultObj instanceof Number number) {
            double value = number.doubleValue();
            if (value >= -1.0 && value <= 1024.0) {
                newDefaultSpeed = value;
            } else {
                log.warning("Invalid default-speed, using -1.0 (disabled)");
                newDefaultSpeed = -1.0;
            }
        } else {
            log.warning("Invalid default-speed, using -1.0 (disabled)");
            newDefaultSpeed = -1.0;
        }

        Object fallThroughObj = cfg.get("fall-through");
        if (fallThroughObj == null) {
            newFallThrough = false;
        } else if (fallThroughObj instanceof Boolean value) {
            newFallThrough = value;
        } else {
            log.warning("Invalid fall-through, using false");
            newFallThrough = false;
        }

        if (cfg.contains("regions")) {
            log.warning("Top-level 'regions' is no longer supported and is ignored; "
                    + "move regions under 'groups.<group>.regions'.");
        }

        ConfigurationSection groupsSection = cfg.getConfigurationSection("groups");
        if (groupsSection != null) {
            int index = 0;
            for (String groupName : groupsSection.getKeys(false)) {
                ConfigurationSection groupSection = groupsSection.getConfigurationSection(groupName);
                if (groupSection == null) {
                    log.warning("Group '" + groupName + "' skipped: not a section");
                    continue;
                }
                if (!NAME_PATTERN.matcher(groupName).matches()) {
                    log.warning("Group '" + groupName + "' skipped: invalid name");
                    continue;
                }

                int groupPriority = parsePriority(groupSection.get("priority"),
                        "Group '" + groupName + "'", log);

                ConfigurationSection regionsSection = groupSection.getConfigurationSection("regions");
                if (regionsSection == null) {
                    continue;
                }

                Set<String> seenNames = new HashSet<>();
                Set<String> seenWorldGuard = new HashSet<>();

                for (String name : regionsSection.getKeys(false)) {
                    String label = "Region '" + groupName + ":" + name + "'";
                    int currentIndex = index;
                    index++;

                    ConfigurationSection sec = regionsSection.getConfigurationSection(name);
                    if (sec == null) {
                        skipped++;
                        log.warning(label + " skipped: not a section");
                        continue;
                    }

                    if (!NAME_PATTERN.matcher(name).matches()) {
                        skipped++;
                        log.warning(label + " skipped: invalid name");
                        continue;
                    }
                    if (!seenNames.add(name.toLowerCase(Locale.ROOT))) {
                        skipped++;
                        log.warning(label + " skipped: duplicate name");
                        continue;
                    }

                    String worldName = sec.getString("world");
                    if (worldName == null || worldName.isBlank()) {
                        skipped++;
                        log.warning(label + " skipped: missing world");
                        continue;
                    }

                    boolean hasPos1 = sec.isSet("pos1");
                    boolean hasPos2 = sec.isSet("pos2");
                    boolean hasWorldGuard = sec.isSet("worldguard");
                    boolean cuboid = hasPos1 || hasPos2;

                    if (hasWorldGuard && cuboid) {
                        skipped++;
                        log.warning(label + " skipped: both pos1/pos2 and worldguard are set");
                        continue;
                    }
                    if (!hasWorldGuard && !cuboid) {
                        skipped++;
                        log.warning(label + " skipped: no pos1/pos2 or worldguard");
                        continue;
                    }

                    Area area;
                    if (cuboid) {
                        Integer[] pos1 = parsePosition(sec, "pos1");
                        Integer[] pos2 = parsePosition(sec, "pos2");
                        if (pos1 == null || pos2 == null) {
                            skipped++;
                            log.warning(label + " skipped: pos1/pos2 must be a list of 3 integers");
                            continue;
                        }
                        area = CuboidArea.of(pos1[0], pos1[1], pos1[2], pos2[0], pos2[1], pos2[2]);
                    } else {
                        String regionId = sec.getString("worldguard");
                        if (regionId == null || regionId.isBlank()) {
                            skipped++;
                            log.warning(label + " skipped: empty worldguard");
                            continue;
                        }
                        String duplicateKey = (worldName + "\u0000" + regionId).toLowerCase(Locale.ROOT);
                        if (!seenWorldGuard.add(duplicateKey)) {
                            skipped++;
                            log.warning(label + " skipped: WorldGuard region already added in this group");
                            continue;
                        }
                        area = new WorldGuardArea(regionId, wg);
                    }

                    int priority = parsePriority(sec.get("priority"), label, log);

                    TreeMap<Integer, Double> speeds = new TreeMap<>();
                    ConfigurationSection speedSection = sec.getConfigurationSection("break-speed");
                    if (speedSection != null) {
                        for (String levelKey : speedSection.getKeys(false)) {
                            int level;
                            try {
                                level = Integer.parseInt(levelKey);
                            } catch (NumberFormatException exception) {
                                log.warning(label + " break-speed key '" + levelKey
                                        + "' is not an integer, skipping entry");
                                continue;
                            }
                            if (level < 0) {
                                log.warning(label + " break-speed key '" + levelKey
                                        + "' must be >= 0, skipping entry");
                                continue;
                            }
                            Object speedObj = speedSection.get(levelKey);
                            if (!(speedObj instanceof Number speedNumber)) {
                                log.warning(label + " break-speed value for '" + levelKey
                                        + "' is not a number, skipping entry");
                                continue;
                            }
                            double speed = speedNumber.doubleValue();
                            if (speed < 0.0 || speed > 1024.0) {
                                log.warning(label + " break-speed value for '" + levelKey
                                        + "' must be between 0.0 and 1024.0, skipping entry");
                                continue;
                            }
                            speeds.put(level, speed);
                        }
                    }

                    World bukkitWorld = Bukkit.getWorld(worldName);
                    InactiveReason reason = null;
                    String worldguardRegionId = area instanceof WorldGuardArea wga ? wga.regionId() : null;

                    if (bukkitWorld == null) {
                        reason = InactiveReason.WORLD_NOT_LOADED;
                    } else if (worldguardRegionId != null) {
                        if (wg == null) {
                            reason = InactiveReason.WORLDGUARD_MISSING;
                        } else if (!wg.regionExists(bukkitWorld, worldguardRegionId)) {
                            reason = InactiveReason.WORLDGUARD_REGION_NOT_FOUND;
                        }
                    }

                    BreakRegion region = new BreakRegion(groupName, groupPriority, name, worldName,
                            priority, currentIndex, area, speeds, reason);
                    newRecorded.add(region);

                    if (reason == null) {
                        active++;
                        newActiveByWorld.computeIfAbsent(bukkitWorld.getName(), key -> new ArrayList<>()).add(region);
                        if (area instanceof CuboidArea) {
                            activeCuboid++;
                        } else {
                            activeWorldGuard++;
                        }
                    } else {
                        inactive++;
                        if (reason == InactiveReason.WORLD_NOT_LOADED) {
                            log.warning(label + " is inactive: " + reason.consoleMessage(worldName));
                        } else if (reason == InactiveReason.WORLDGUARD_REGION_NOT_FOUND) {
                            log.warning(label + " is inactive: "
                                    + reason.consoleMessage(worldguardRegionId, worldName));
                        } else {
                            log.warning(label + " is inactive: " + reason.consoleMessage());
                        }
                    }
                }
            }
        }

        Comparator<BreakRegion> comparator = Comparator.comparingInt(BreakRegion::groupPriority).reversed()
                .thenComparing(Comparator.comparingInt(BreakRegion::priority).reversed())
                .thenComparingInt(BreakRegion::index);
        for (List<BreakRegion> list : newActiveByWorld.values()) {
            list.sort(comparator);
        }

        this.defaultSpeed = newDefaultSpeed;
        this.fallThrough = newFallThrough;
        this.recorded = newRecorded;
        this.activeByWorld = newActiveByWorld;

        log.info("Loaded " + (active + inactive) + " region(s): " + active + " active ("
                + activeCuboid + " cuboid, " + activeWorldGuard + " worldguard), "
                + inactive + " inactive, " + skipped + " skipped.");

        return new LoadResult(active, inactive, skipped, activeCuboid, activeWorldGuard);
    }

    private static int parsePriority(Object obj, String context, Logger log) {
        if (obj == null) {
            return 0;
        }
        if (obj instanceof Integer integerPriority) {
            return integerPriority;
        }
        if (obj instanceof Long longPriority) {
            if (longPriority >= Integer.MIN_VALUE && longPriority <= Integer.MAX_VALUE) {
                return longPriority.intValue();
            }
        } else if (obj instanceof Number number) {
            double value = number.doubleValue();
            if (value == Math.floor(value)
                    && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                return (int) value;
            }
        }
        log.warning(context + " has an invalid priority, using 0");
        return 0;
    }

    private static Integer[] parsePosition(ConfigurationSection sec, String key) {
        List<?> list = sec.getList(key);
        if (list == null || list.size() != 3) {
            return null;
        }
        Integer[] result = new Integer[3];
        for (int i = 0; i < 3; i++) {
            Object value = list.get(i);
            if (value instanceof Integer integerValue) {
                result[i] = integerValue;
            } else if (value instanceof Long longValue) {
                if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
                    return null;
                }
                result[i] = longValue.intValue();
            } else if (value instanceof Double doubleValue) {
                if (doubleValue != Math.floor(doubleValue)
                        || doubleValue < Integer.MIN_VALUE || doubleValue > Integer.MAX_VALUE) {
                    return null;
                }
                result[i] = doubleValue.intValue();
            } else {
                return null;
            }
        }
        return result;
    }

    /**
     * @param group the tool's group name, or an empty string if it has none
     * @return the speed to apply, or {@code null} when the plugin should leave the base value alone
     */
    public Double resolveSpeed(Block block, String group, int level) {
        List<BreakRegion> list = activeByWorld.get(block.getWorld().getName());
        if (list == null) {
            return fallback();
        }
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        for (BreakRegion region : list) {
            if (!region.area().contains(block.getWorld(), x, y, z)) {
                continue;
            }
            if (group != null && !group.isEmpty() && region.group().equalsIgnoreCase(group)) {
                Double speed = region.speedFor(level);
                if (speed != null) {
                    return speed;
                }
            }
            if (!fallThrough) {
                return fallback();
            }
        }
        return fallback();
    }

    private Double fallback() {
        return defaultSpeed < 0.0 ? null : defaultSpeed;
    }

    public BreakRegion findWorldGuardDuplicate(String group, String world, String regionId) {
        for (BreakRegion region : recorded) {
            if (region.group().equalsIgnoreCase(group)
                    && region.area() instanceof WorldGuardArea area
                    && region.worldName().equals(world)
                    && area.regionId().equalsIgnoreCase(regionId)) {
                return region;
            }
        }
        return null;
    }

    public List<BreakRegion> getRecorded() {
        return recorded;
    }
}
