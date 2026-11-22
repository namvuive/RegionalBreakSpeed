# RegionalBreakSpeed

A Spigot/Paper 1.21.x plugin that adjusts a player's `BLOCK_BREAK_SPEED` attribute per region, driven by custom MMOItems stats.

## Requirements

- Java 21
- Spigot / Paper / Purpur / Pufferfish **1.21.3+** (built against Spigot API 1.21.4)
- Required: **MMOItems**, **MythicLib**
- Optional: **WorldGuard**, **WorldEdit** or **FastAsyncWorldEdit**

## Installation

1. Build or download `RegionalBreakSpeed-1.0.0.jar`.
2. Drop the jar into the server's `plugins/` folder.
3. Start the server, then check the generated files:
   - `plugins/RegionalBreakSpeed/config.yml`
   - `plugins/RegionalBreakSpeed/messages.yml`

## How it works

When a player starts breaking a block (`BlockDamageEvent`):

1. The plugin reads the **group** and **level** stats from the item in the main hand.
2. It walks the active regions containing the block, ordered by group priority, then region priority, then file order.
3. The first region whose group matches the item's group and whose `break-speed` table has an entry for the level decides the speed. The table uses the **largest key `<= item level`**.
4. It sets the base value of the player's `BLOCK_BREAK_SPEED` attribute.

If no region matches, `default-speed` is used. Setting `default-speed: -1` disables the fallback: the plugin then only changes break speed while the player mines inside a matching region, and restores the vanilla value when they mine elsewhere.

Regions support two types:

- **Cuboid**: defined explicitly with `pos1` / `pos2`.
- **WorldGuard**: references a WorldGuard region in the same world.

`fall-through` controls overlap behavior. With `false` (default), the first region containing the block decides; if it yields no speed for the item, `default-speed` is used and lower-priority regions are not checked. With `true`, regions that contain the block but do not yield a speed for the item are skipped and lower-priority regions are checked.

## MMOItems stats

Two stats are registered:

| Stat | ID | YAML key | Written on |
|---|---|---|---|
| Regional Break Speed Level | `REGIONAL_BREAK_SPEED_LEVEL` | `regional-break-speed-level` | TOOL only |
| Regional Break Speed Group | `REGIONAL_BREAK_SPEED_GROUP` | `regional-break-speed-group` | TOOL only |

- Level must be a positive whole number; decimals are floored when read (e.g. `3.7` becomes `3`).
- Group must contain only letters, numbers, `_` and `-`, and is matched to config group names case-insensitively.
- Neither stat is ever shown in item lore.
- You can also set them manually in an MMOItems item file:

```yaml
regional-break-speed-level: 3
regional-break-speed-group: mining
```

## Region configuration

```yaml
default-speed: -1      # -1 disables the fallback; vanilla is 1.0
fall-through: false

groups:
  mining:              # group name, matched against the item's group stat
    priority: 1        # group priority (default 0)
    regions:
      spawn_mine:
        world: world
        pos1: [100, 60, 100]
        pos2: [140, 80, 140]
        break-speed:
          0: 0.3
          1: 1.0
          5: 4.0
        priority: 0

      wg_mine:
        world: world
        worldguard: mining_area
        break-speed:
          1: 2.0
        priority: 1

  farming:
    regions:
      farm:
        world: world
        worldguard: farm_area
        break-speed:
          0: 1.5
```

- `default-speed`: used when nothing matches; valid range `-1.0`–`1024.0`. `-1` disables the fallback.
- `fall-through`: when `true`, keep checking lower-priority regions if a containing region does not yield a speed for the item.
- `groups.<group>.priority`: group rank, default `0`.
- `regions.<name>.priority`: region rank inside the group, default `0`.
- `break-speed`: optional. If absent/empty, the region yields no speed and the fallback / fall-through applies.

Resolution order for a block: group priority (desc), region priority (desc), then declaration order in the file (asc). The first matching region whose group equals the item's group decides. An item with no group stat, a group missing from config, or a mismatched group is treated as having no match.

Region names must be unique within a group; the same name may appear in different groups. A WorldGuard region may be added to multiple groups.

A region is **inactive** (kept in the file but ignored) if, at load/reload time:

- its world is not loaded,
- WorldGuard is not installed (for WorldGuard regions),
- the WorldGuard region does not exist.

Active/inactive state is only recalculated on plugin startup, `/rgs reload`, or after `add` / `create` / `remove`. A top-level `regions:` section is ignored with a console warning; regions must live under `groups.<group>.regions`.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/rgs help` | Show the command list | `regionalbreakspeed.help` |
| `/rgs add <group> <world:region> [name]` | Add a WorldGuard region to a group | `regionalbreakspeed.add` |
| `/rgs create <group> <name>` | Create a cuboid region in a group from your WorldEdit selection | `regionalbreakspeed.create` |
| `/rgs remove <group> <name>` | Remove a region from a group | `regionalbreakspeed.remove` |
| `/rgs list [page]` | List regions (10 per page) | `regionalbreakspeed.list` |
| `/rgs reload` | Reload `config.yml` and `messages.yml` | `regionalbreakspeed.reload` |

`add` and `create` create the group with priority `0` if it does not exist yet; an existing group's priority is left untouched. Aliases: `/regionalbreakspeed`, `/rgs`.

## Build

```bash
mvn clean package
```

Output: `target/RegionalBreakSpeed-1.0.0.jar`.

## Notes

- Spigot API only; no Paper API or Adventure dependency.
- No shading; all dependencies use `provided` scope.
- Folia is not supported; everything runs on the main thread.
- No debug mode and no per-block logging.
