# RegionalBreakSpeed

A Spigot/Paper 1.21.x plugin that adjusts a player's `BLOCK_BREAK_SPEED` attribute per region, driven by a custom MMOItems stat.

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

1. The plugin finds the region containing the block, if any.
2. It reads the `Regional Break Speed Level` stat from the item in the main hand.
3. It looks up the region's `break-speed` table using the **largest key `<= item level`**.
4. It sets the base value of the player's `BLOCK_BREAK_SPEED` attribute.

If the block is outside every active region, or no entry matches the level, `default-speed` is used.

Regions support two types:

- **Cuboid**: defined explicitly with `pos1` / `pos2`.
- **WorldGuard**: references a WorldGuard region in the same world.

When regions overlap, the higher `priority` wins; on ties, the region declared first in the file wins. If the winning region has no matching level, `default-speed` is used (lower-priority regions are not checked).

## MMOItems stat

The stat ID is `REGIONAL_BREAK_SPEED_LEVEL` (YAML key `regional-break-speed-level`, NBT `MMOITEMS_REGIONAL_BREAK_SPEED_LEVEL`).

- Only available on **TOOL** items.
- Value must be a positive whole number.
- The stat is **never shown in item lore**.
- You can also set it manually in an MMOItems item file, for example:

```yaml
regional-break-speed-level: 3
```

Decimal values are floored when read (e.g. `3.7` becomes `3`).

## Region configuration

```yaml
default-speed: 1.0

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
```

- `default-speed`: used outside regions or when no level matches; valid range `0.0`–`1024.0`.
- `break-speed`: optional. If absent/empty, the region falls back to `default-speed`.
- `priority`: defaults to `0`.

A region is **inactive** (kept in the file but ignored) if, at load/reload time:

- its world is not loaded,
- WorldGuard is not installed (for WorldGuard regions),
- the WorldGuard region does not exist.

Active/inactive state is only recalculated on plugin startup, `/rgs reload`, or after `add` / `create` / `remove`.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/rgs help` | Show the command list | `regionalbreakspeed.help` |
| `/rgs add <world:region> [name]` | Add a WorldGuard region | `regionalbreakspeed.add` |
| `/rgs create <name>` | Create a cuboid region from your WorldEdit selection | `regionalbreakspeed.create` |
| `/rgs remove <name>` | Remove a region | `regionalbreakspeed.remove` |
| `/rgs list [page]` | List regions (10 per page) | `regionalbreakspeed.list` |
| `/rgs reload` | Reload `config.yml` and `messages.yml` | `regionalbreakspeed.reload` |

Aliases: `/regionalbreakspeed`, `/rgs`.

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
