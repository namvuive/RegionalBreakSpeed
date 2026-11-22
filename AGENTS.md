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
- Register both MMOItems stats in `onLoad()` (MMOItems loads item templates in its `onEnable`), not `onEnable()`.
- Never use `JavaPlugin#getConfig()` / `saveConfig()`. `ConfigManager` owns its own `YamlConfiguration` with `options().parseComments(true)`.
- Set the **base value** of `Attribute.BLOCK_BREAK_SPEED` directly. No `AttributeModifier`, no `BlockBreakEvent`, no async/Folia.
- Neither stat may add lore. `RegionalBreakSpeedLevelStat#whenApplied` only calls `item.addItemTag(getAppliedNBT(data))` (`whenPreviewed` delegates to it); `RegionalBreakSpeedGroupStat#whenApplied` does the same and overrides nothing else lore-related. `StringStat` has no `whenPreviewed` in this version.
- `/rgs add` and `/rgs create` intentionally do **not** write `break-speed:`/`break-speed: {}`. Do not re-add it, and do not auto-delete existing empty `break-speed` sections (user rejected both).
- `@Nullable` annotations were removed because JetBrains annotations are not on the compile classpath; do not reintroduce them without adding a dependency.

## Architecture map

- `RegionalBreakSpeed` — entrypoint: `onLoad` registers both stats; `onEnable` detects hooks, loads config/messages/registry, registers listener + command; `onDisable` resets tracked speeds only and clears tracking (never touches untracked players).
- `region/RegionRegistry` — parses `default-speed`, `fall-through` and `groups.*`, records active+inactive regions, indexes active ones by `World#getName()` sorted group priority desc, region priority desc, declaration index asc. `resolveSpeed(block, group, level)` returns `Double` (`null` = leave base value alone). `BreakRegion#speedFor` uses `floorEntry`.
- `listener/BreakSpeedListener` — `BlockDamageEvent` at `MONITOR`, skips creative/spectator, reads main-hand group+level via `LevelReader#read` (single NBT parse), tracks plugin-set speeds in `Map<UUID, Double>`, resets once when a `null` result arrives; on quit it resets only if the player was tracked. `resetAll()` resets tracked players and clears the map.
- `config/ConfigManager` — own `YamlConfiguration`; on syntax error keeps the previous config and sets `broken`, which blocks `add`/`create`/`remove`. Group/region lookup helpers are group-scoped (`findGroupKey`, `getGroupKeys`, `findRegionKey(group, name)`, `getRegionKeys(group)`).
- `mmoitems/LevelReader` — `read(ItemStack)` parses NBT once and returns `ToolData(level, group)`: level from `MMOITEMS_REGIONAL_BREAK_SPEED_LEVEL` (floored, clamped `>= 0`), group from `MMOITEMS_REGIONAL_BREAK_SPEED_GROUP` (trimmed + lowercased; `""` when absent/blank).
- `command/RgsCommand` — 6 subcommands + tab completion; each subcommand checks `regionalbreakspeed.<sub>`. `add`/`create`/`remove` take a group argument; tab completion suggests groups in arg 2, WG keys in arg 3 for `add`, and group region names in arg 3 for `remove`.

## Behavior rules that are easy to get wrong

- Region active/inactive is recomputed **only** in `RegionRegistry.load(...)`: startup, `/rgs reload`, and after `add`/`create`/`remove`. Inactive regions stay in config but are never queried while mining. Every reload resets tracked break speeds (`BreakSpeedListener#resetAll`).
- Region ordering (precomputed once in `load`): group priority desc → region priority desc → declaration index asc. The first containing region whose group matches the item's group and whose `break-speed` has a matching level wins.
- Group names are normalized to lowercase end to end: the stat lowercases input before saving NBT, `LevelReader` lowercases the read value, and `RegionRegistry` compares lowercased group names. Two group keys that differ only by case (e.g. `Mine`/`mine`) are a collision — the later one is skipped with a warning. Region names and WorldGuard duplicates are only blocked **within the same group**; across groups the same name/region may coexist.
- `fall-through: false` (default): the first containing region ends the search; if it yields no speed, use the fallback. `true`: keep checking lower-priority containing regions.
- Fallback: `default-speed < 0` → return `null` (do not touch the attribute); otherwise return `default-speed`.
- `default-speed` range is `[-1, 1024]`; invalid → console warning and `-1`. `fall-through` must be boolean; otherwise warning and `false`.
- A top-level `regions:` section is logged and ignored (no auto-migration).
- `break-speed` matching uses the largest key `<= item level` (e.g. keys `0,1,5`: level 3 → key 1).
- `null` speed handling: if the player is tracked as plugin-modified, reset base to `getDefaultValue()` once and untrack; if not tracked, do nothing. `default-speed: -1` therefore leaves vanilla behavior intact until a match was applied.
- `Messages#send` prepends the prefix; `sendRaw` does not. `help-header`, `help-line`, `list-header`, and `list-entry-*` are sent raw; `status-*`/`reason-*` are embedded into other messages.
- `WorldGuardArea` deliberately stores a `WorldGuardHook` without importing any `com.sk89q.*` type.

## Dependencies

- MMOItems `6.10-SNAPSHOT`, MythicLib `1.7.1-SNAPSHOT` from `https://nexus.phoenixdevt.fr/repository/maven-public/`; WorldEdit `7.2.15`, WorldGuard `7.0.9`.
- MMOItems method signatures were verified with `javap` against the resolved jars. If you bump the MMOItems/MythicLib version, re-inspect `DoubleStat`, `StringStat`, `StringData`, `whenInput`, `whenApplied`, `whenPreviewed`, `getAppliedNBT`, and `ItemStackBuilder#addItemTag` before editing the stat classes.
- `target/` is gitignored; do not commit build output.
