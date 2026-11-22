# AGENTS.md

## Project

RegionalBreakSpeed — Spigot/Paper 1.21.x plugin (Java 21, Maven). Sets the `BLOCK_BREAK_SPEED` attribute from an MMOItems stat when a player starts mining a block inside a configured region.

## Commands

- Build/verify: `mvn clean package` → `target/RegionalBreakSpeed-1.0.0.jar`
- No tests, lint, formatter, or CI exist. Compilation is the only automated check.
- Real verification needs a 1.21.3+ server with **MMOItems + MythicLib**; WorldGuard/WorldEdit/FAWE are optional.

## Hard constraints (intentional — do not "improve")

- **Spigot API only**: no Paper API, Adventure/MiniMessage/Component, Brigadier, `io.papermc.*`, `net.kyori.*`, or `paper-plugin.yml`. Colors use `&` + `ChatColor.translateAlternateColorCodes`.
- `com.sk89q.*` imports live **only** in `hook/WorldGuardHook.java` and `hook/WorldEditHook.java`. Instantiate them only when the respective plugin is present and enabled, inside `onEnable`. Their public APIs may only use Bukkit/Java/plugin types.
- All dependencies are `provided`; never shade. Only `plugin.yml` is resource-filtered; `config.yml` and `messages.yml` must stay unfiltered.
- Register the MMOItems stat in `onLoad()` (MMOItems loads item templates in its `onEnable`), not `onEnable()`.
- Never use `JavaPlugin#getConfig()` / `saveConfig()`. `ConfigManager` owns its own `YamlConfiguration` with `options().parseComments(true)`.
- Set the **base value** of `Attribute.BLOCK_BREAK_SPEED` directly. No `AttributeModifier`, no `BlockBreakEvent`, no async/Folia.
- The stat must never add lore. `RegionalBreakSpeedLevelStat#whenApplied` only calls `item.addItemTag(getAppliedNBT(data))`; `whenPreviewed` delegates to it.
- **Divergence from `PLAN.md`**: `/rgs add` and `/rgs create` intentionally do **not** write `break-speed:`/`break-speed: {}`. Do not re-add it, and do not auto-delete existing empty `break-speed` sections (user rejected both).
- `@Nullable` annotations were removed because JetBrains annotations are not on the compile classpath; do not reintroduce them without adding a dependency.

## Architecture map

- `RegionalBreakSpeed` — entrypoint: `onLoad` registers stat; `onEnable` detects hooks, loads config/messages/registry, registers listener + command; `onDisable` resets speed for online players.
- `region/RegionRegistry` — parses `config.yml`, records active+inactive regions, indexes active ones by `World#getName()` sorted priority desc then declaration index. `BreakRegion#speedFor` uses `floorEntry`.
- `listener/BreakSpeedListener` — `BlockDamageEvent` at `MONITOR`, skips creative/spectator, reads main-hand level, sets base value.
- `config/ConfigManager` — own `YamlConfiguration`; on syntax error keeps the previous config and sets `broken`, which blocks `add`/`create`/`remove`.
- `mmoitems/LevelReader` — reads NBT `MMOITEMS_REGIONAL_BREAK_SPEED_LEVEL`, floors to int, clamps to `>= 0`.
- `command/RgsCommand` — 6 subcommands + tab completion; each subcommand checks `regionalbreakspeed.<sub>`.

## Behavior rules that are easy to get wrong

- Region active/inactive is recomputed **only** in `RegionRegistry.load(...)`: startup, `/rgs reload`, and after `add`/`create`/`remove`. Inactive regions stay in config but are never queried while mining.
- Region resolution: highest `priority` wins; ties go to the region declared first in the file. If the winning region has no matching `break-speed` key, use `default-speed` — do **not** fall through to lower-priority regions.
- `break-speed` matching uses the largest key `<= item level` (e.g. keys `0,1,5`: level 3 → key 1).
- `Messages#send` prepends the prefix; `sendRaw` does not. `help-header`, `help-line`, `list-header`, and `list-entry-*` are sent raw; `status-*`/`reason-*` are embedded into other messages.
- `WorldGuardArea` deliberately stores a `WorldGuardHook` without importing any `com.sk89q.*` type.

## Dependencies

- MMOItems `6.10-SNAPSHOT`, MythicLib `1.7.1-SNAPSHOT` from `https://nexus.phoenixdevt.fr/repository/maven-public/`; WorldEdit `7.2.15`, WorldGuard `7.0.9`.
- MMOItems method signatures were verified with `javap` against the resolved jars. If you bump the MMOItems/MythicLib version, re-inspect `DoubleStat`, `whenInput`, `whenApplied`, `whenPreviewed`, `getAppliedNBT`, and `ItemStackBuilder#addItemTag` before editing the stat class.
- `target/` is gitignored; do not commit build output.
