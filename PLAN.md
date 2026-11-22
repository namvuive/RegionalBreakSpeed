# PLAN: Plugin RegionalBreakSpeed (Spigot/Paper 1.21.4)

## 0. Bối cảnh và mục tiêu

Viết plugin Minecraft tên **RegionalBreakSpeed**:
- Thêm một stat mới vào MMOItems tên **"Regional Break Speed Level"**. Key trong file YAML của MMOItems là `regional-break-speed-level`. Giá trị là số nguyên > 0. Stat chỉ dùng cho item loại **TOOL**.
- Khi người chơi **bắt đầu đập một block** (`BlockDamageEvent`), plugin xác định block đó nằm trong region nào (theo config). Sau đó plugin đọc level stat của item ở **tay chính** và set **base value** của attribute `BLOCK_BREAK_SPEED` cho người chơi.
- Region có 2 loại: **hình hộp theo tọa độ (cuboid)** hoặc **region của WorldGuard**.
- Có lệnh `/regionalbreakspeed` (alias `/rgs`) để thêm, tạo, xóa, liệt kê region và reload.

Bạn phải viết **toàn bộ code hoàn chỉnh, biên dịch được**, cho **từng file** liệt kê ở mục 2. Không để TODO, không viết code giả.

---

## 1. Ràng buộc bắt buộc (KHÔNG được vi phạm)

1. **Java 21**, build bằng **Maven**. Package gốc: `com.namvuive.regionalbreakspeed`.
2. Biên dịch với **Spigot API 1.21.4** (`org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT`). **CHỈ dùng API Bukkit/Spigot.** Plugin phải chạy được trên Spigot, Paper, Purpur, Pufferfish từ 1.21.3 trở lên.
   - KHÔNG dùng Paper API, Adventure/MiniMessage/Component, Brigadier, `paper-plugin.yml`, hay bất kỳ class nào trong `io.papermc.*` hoặc `net.kyori.*`.
   - Màu chữ dùng mã `&`, chuyển bằng `ChatColor.translateAlternateColorCodes('&', ...)`.
   - Dùng file `plugin.yml` thường.
3. Không hỗ trợ Folia. Mọi thứ chạy trên main thread, không dùng async.
4. Các plugin phụ thuộc (MMOItems, MythicLib, WorldGuard, WorldEdit) đều để scope `provided`. KHÔNG shade (đóng gói) bất kỳ thư viện nào vào jar.
5. **MMOItems** và **MythicLib** là bắt buộc (`depend`). **WorldGuard**, **WorldEdit** và **FastAsyncWorldEdit** là không bắt buộc (`softdepend`).
6. **Cách ly hook:** mọi code tham chiếu class của WorldGuard hoặc WorldEdit (`com.sk89q.*`) CHỈ được nằm trong `hook/WorldGuardHook.java` và `hook/WorldEditHook.java`.
   - Các public method của 2 class này chỉ được dùng kiểu Java/Bukkit trong tham số và kiểu trả về (`World`, `Player`, `String`, `int`, `List<String>`, record của plugin). Không lộ kiểu `com.sk89q.*` ra ngoài.
   - 2 class này chỉ được khởi tạo (`new`) khi plugin tương ứng có mặt và đang bật.
   - Mục đích: server không có WorldGuard/WorldEdit thì plugin không bị lỗi `NoClassDefFoundError`.
7. **Không có chế độ debug**, không in log mỗi lần đào. Chỉ log: tóm tắt khi load, cảnh báo config sai hoặc region inactive, trạng thái hook, lỗi nghiêm trọng.
8. Stat **KHÔNG được hiện trên lore** của item.
9. Tin nhắn cho người chơi bằng **tiếng Anh**, lấy từ `messages.yml`.
10. Không thêm tính năng nào ngoài plan này.

---

## 2. Cấu trúc project

```
RegionalBreakSpeed/
├── pom.xml
└── src/main/
    ├── java/com/namvuive/regionalbreakspeed/
    │   ├── RegionalBreakSpeed.java          # main class
    │   ├── config/
    │   │   ├── ConfigManager.java           # load/save config.yml (tự quản lý, KHÔNG dùng getConfig())
    │   │   └── Messages.java                # load messages.yml + format + gửi tin nhắn
    │   ├── region/
    │   │   ├── Area.java                    # sealed interface
    │   │   ├── CuboidArea.java              # record
    │   │   ├── WorldGuardArea.java          # record
    │   │   ├── InactiveReason.java          # enum
    │   │   ├── BreakRegion.java             # record
    │   │   ├── LoadResult.java              # record
    │   │   └── RegionRegistry.java          # parse config, lưu và tra cứu region
    │   ├── hook/
    │   │   ├── WorldGuardHook.java
    │   │   ├── WorldEditHook.java
    │   │   └── CuboidSelection.java         # record (chỉ kiểu Java)
    │   ├── mmoitems/
    │   │   ├── RegionalBreakSpeedLevelStat.java
    │   │   └── LevelReader.java
    │   ├── listener/
    │   │   └── BreakSpeedListener.java
    │   └── command/
    │       └── RgsCommand.java              # CommandExecutor + TabCompleter
    └── resources/
        ├── plugin.yml
        ├── config.yml
        └── messages.yml
```

---

## 3. pom.xml

- groupId `com.namvuive`, artifactId `RegionalBreakSpeed`, version `1.0.0`, packaging `jar`.
- Properties: `java.version=21`, `project.build.sourceEncoding=UTF-8`.
- `maven-compiler-plugin` 3.13.0 với `<release>21</release>`.
- Resource filtering: CHỈ filter `plugin.yml` (để thay `${project.version}`). `config.yml` và `messages.yml` KHÔNG filter. Làm bằng 2 khối `<resource>`: một khối `filtering=true` với `<include>plugin.yml</include>`, một khối `filtering=false` với `<exclude>plugin.yml</exclude>`.
- `<finalName>RegionalBreakSpeed-${project.version}</finalName>`.

**Repositories:**
```xml
<repository><id>spigotmc-repo</id><url>https://hub.spigotmc.org/nexus/content/repositories/snapshots/</url></repository>
<repository><id>sonatype</id><url>https://oss.sonatype.org/content/repositories/snapshots/</url></repository>
<repository><id>sonatype-central-snapshots</id><url>https://central.sonatype.com/repository/maven-snapshots/</url></repository>
<repository><id>enginehub</id><url>https://maven.enginehub.org/repo/</url></repository>
<repository><id>phoenix</id><url>https://nexus.phoenixdevt.fr/repository/maven-public/</url></repository>
```

**Dependencies** (tất cả `provided`):

| groupId | artifactId | version | ghi chú |
|---|---|---|---|
| org.spigotmc | spigot-api | 1.21.4-R0.1-SNAPSHOT | khai báo ĐẦU TIÊN |
| net.Indyuce | MMOItems-API | 6.10-SNAPSHOT | exclusion `*:*` |
| io.lumine | MythicLib-dist | 1.7.1-SNAPSHOT | exclusion `*:*` |
| com.sk89q.worldedit | worldedit-core | 7.2.15 | exclusion `*:*` |
| com.sk89q.worldedit | worldedit-bukkit | 7.2.15 | exclusion `*:*` |
| com.sk89q.worldguard | worldguard-core | 7.0.9 | exclusion `*:*` |
| com.sk89q.worldguard | worldguard-bukkit | 7.0.9 | exclusion `*:*` |

Exclusion `*:*` viết như sau:
```xml
<exclusions><exclusion><groupId>*</groupId><artifactId>*</artifactId></exclusion></exclusions>
```

Lý do chọn version: biên dịch với WorldEdit 7.2 / WorldGuard 7.0.9 thì chạy được trên WorldEdit 7.2–7.3, FAWE 2.x và WorldGuard 7.0.9–7.0.13+.

**Phương án dự phòng nếu không tải được dependency:**
- Version MMOItems-API hoặc MythicLib-dist không có trên nexus: vào `https://nexus.phoenixdevt.fr/#browse/browse:maven-public` tìm bản 6.10.x / 1.7.x gần nhất. Nếu vẫn không có, cài jar từ thư mục `plugins/` của server bằng `mvn install:install-file -Dfile=... -DgroupId=... -DartifactId=... -Dversion=... -Dpackaging=jar`.
- Biên dịch báo thiếu class của WorldEdit/WorldGuard do exclusion `*:*`: bỏ exclusion `*:*` của 4 artifact sk89q. Thay bằng exclusion cụ thể `org.bukkit:bukkit`, `org.spigotmc:spigot-api`, `io.papermc.paper:paper-api`, `com.destroystokyo.paper:paper-api`, `org.bstats:*`.

---

## 4. plugin.yml (nội dung chính xác)

```yaml
name: RegionalBreakSpeed
version: ${project.version}
main: com.namvuive.regionalbreakspeed.RegionalBreakSpeed
api-version: '1.21'
author: namvuive
description: Sets block break speed in configured regions based on an MMOItems stat.
depend: [MythicLib, MMOItems]
softdepend: [WorldGuard, WorldEdit, FastAsyncWorldEdit]

commands:
  regionalbreakspeed:
    description: RegionalBreakSpeed main command.
    usage: /<command> help
    aliases: [rgs]

permissions:
  regionalbreakspeed.*:
    description: Grants all RegionalBreakSpeed permissions.
    default: op
    children:
      regionalbreakspeed.help: true
      regionalbreakspeed.add: true
      regionalbreakspeed.create: true
      regionalbreakspeed.remove: true
      regionalbreakspeed.list: true
      regionalbreakspeed.reload: true
  regionalbreakspeed.help:
    description: Use /rgs help.
    default: op
  regionalbreakspeed.add:
    description: Use /rgs add.
    default: op
  regionalbreakspeed.create:
    description: Use /rgs create.
    default: op
  regionalbreakspeed.remove:
    description: Use /rgs remove.
    default: op
  regionalbreakspeed.list:
    description: Use /rgs list.
    default: op
  regionalbreakspeed.reload:
    description: Use /rgs reload.
    default: op
```
KHÔNG đặt `permission:` cho chính lệnh. Quyền được kiểm tra theo từng lệnh con.

---

## 5. config.yml mặc định (nội dung chính xác)

```yaml
# RegionalBreakSpeed configuration
#
# default-speed: block_break_speed applied when the block being mined is NOT inside any active region,
#                or when the item's level matches no entry in the region's break-speed.
#                Vanilla value is 1.0. Range: 0.0 - 1024.0
#
# regions:
#   <name>:                      # unique name (letters, numbers, '_' and '-'; case-insensitive unique)
#     world: <world name>        # required
#     # --- choose ONE type ---
#     pos1: [x, y, z]            # cuboid corner 1 (inclusive)
#     pos2: [x, y, z]            # cuboid corner 2 (inclusive)
#     # worldguard: <region id>  # OR a WorldGuard region id in that world
#     break-speed:               # <level>: <speed>
#       0: 0.3                   # optional: items without the stat / below the lowest level
#       1: 1.0
#       5: 4.0
#     priority: 0                # optional, default 0. Higher wins when regions overlap.
#                                # Same priority: the region declared first in this file wins.
#
# Level matching: an item with level N uses the entry with the LARGEST key <= N.
#   Example with keys 0,1,5: level 0 -> key 0, level 3 -> key 1, level 9 -> key 5.
#   If no key <= N exists, default-speed is used.
# The highest-priority region containing the block decides; lower-priority regions are NOT checked.
#
# A region is INACTIVE (kept in this file but ignored) if, at load/reload time, its world is not loaded,
# WorldGuard is not installed (worldguard type), or the WorldGuard region does not exist.
# Inactive regions are re-checked on every /rgs reload.

default-speed: 1.0

regions: {}
```

---

## 6. messages.yml mặc định (nội dung chính xác)

Placeholder có dạng `{key}`. `{label}` là tên lệnh người dùng vừa gõ (`rgs` hoặc `regionalbreakspeed`). Mọi tin nhắn đều có `prefix` ở đầu, TRỪ `help-header`, `help-line`, `list-header`, `list-entry-*`, `status-*` và `reason-*`.

```yaml
prefix: "&8[&6RBS&8] &r"

no-permission: "&cYou don't have permission to use this command."
unknown-command: "&cUnknown subcommand. Use &e/{label} help&c."
players-only: "&cThis command can only be used by players."
config-broken: "&cconfig.yml has a syntax error. Fix it and run &e/{label} reload&c first."
save-failed: "&cFailed to save config.yml. Check the console."

usage-add: "&cUsage: &e/{label} add <world:region> [name]"
usage-create: "&cUsage: &e/{label} create <name>"
usage-remove: "&cUsage: &e/{label} remove <name>"
usage-list: "&cUsage: &e/{label} list [page]"

worldguard-missing: "&cWorldGuard is not installed. This command is unavailable."
worldedit-missing: "&cWorldEdit/FastAsyncWorldEdit is not installed. This command is unavailable."

invalid-format: "&cInvalid format. Use &eworld:region&c."
world-not-found: "&cWorld &e{world}&c not found."
wg-region-not-found: "&cWorldGuard region &e{region}&c not found in world &e{world}&c."
wg-region-already-added: "&cWorldGuard region &e{world}:{region}&c is already added as &e{name}&c."
invalid-name: "&cInvalid name &e{name}&c. Only letters, numbers, '_' and '-' are allowed."
name-exists: "&cA region named &e{name}&c already exists in config.yml."
region-not-found: "&cRegion &e{name}&c not found in config.yml."
no-selection: "&cYou don't have a complete WorldEdit selection."
selection-not-cuboid: "&cYour WorldEdit selection must be a cuboid."
invalid-page: "&cInvalid page. Pages: &e1-{pages}&c."

added: "&aAdded WorldGuard region &e{world}:{region}&a as &e{name}&a. Edit its &ebreak-speed&a in config.yml, then run &e/{label} reload&a."
created: "&aCreated region &e{name}&a in &e{world}&a: &e({x1}, {y1}, {z1}) -> ({x2}, {y2}, {z2})&a. Edit its &ebreak-speed&a in config.yml, then run &e/{label} reload&a."
removed: "&aRemoved region &e{name}&a."
reloaded: "&aReloaded. Active: &e{active}&a, inactive: &e{inactive}&a, skipped: &e{skipped}&a."
reload-warnings: "&eSome regions are inactive or skipped. Check the console for details."
reload-failed: "&cconfig.yml has a syntax error and was NOT reloaded (previous settings are still in use): &7{error}"

help-header: "&6&lRegionalBreakSpeed &7- Commands"
help-line: "&e{usage} &8- &7{description}"
help-entries:
  help: "Show this list of commands"
  add: "Add a WorldGuard region"
  create: "Create a cuboid region from your WorldEdit selection"
  remove: "Remove a region"
  list: "List all regions"
  reload: "Reload config.yml and messages.yml"

list-header: "&6Regions &7(page {page}/{pages}, total {total}):"
list-entry-cuboid: "&e{name} &8| {status} &8| &7cuboid &8| &f{world} &7({x1}, {y1}, {z1}) -> ({x2}, {y2}, {z2}) &8| &7priority &f{priority} &8| &7levels &f{levels}"
list-entry-worldguard: "&e{name} &8| {status} &8| &7worldguard &8| &f{world}:{region} &8| &7priority &f{priority} &8| &7levels &f{levels}"
list-empty: "&7No regions configured."

status-active: "&aactive"
status-inactive: "&cinactive &7({reason})"
reason-world-not-loaded: "world not loaded"
reason-worldguard-missing: "WorldGuard not installed"
reason-worldguard-region-not-found: "WorldGuard region not found"
```

**Messages.java:**
- Load file `messages.yml` trong data folder, rồi `setDefaults` bằng bản `messages.yml` trong jar (đọc qua `getResource("messages.yml")` bằng `InputStreamReader` UTF-8). Nhờ vậy thiếu key nào thì lấy giá trị mặc định.
- File lỗi cú pháp YAML: log WARNING kèm nội dung lỗi và dùng toàn bộ giá trị mặc định trong jar.
- Các method:
  - `String get(String key, String... placeholders)`: placeholders là cặp key/value, thay `{key}` bằng value, rồi translate mã `&`.
  - `void send(CommandSender s, String key, String... placeholders)`: tự thêm prefix.
  - `void sendRaw(CommandSender s, String key, String... placeholders)`: không thêm prefix.
  - `String helpDescription(String sub)`: đọc `help-entries.<sub>`.

---

## 7. Mô hình dữ liệu region

```java
public sealed interface Area permits CuboidArea, WorldGuardArea {
    boolean contains(World world, int x, int y, int z);
}

public record CuboidArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) implements Area {
    // static CuboidArea of(int x1,int y1,int z1,int x2,int y2,int z2) -> tự tính min/max
    // contains: so sánh bao gồm cả 2 đầu (>= min && <= max) cho cả 3 trục; bỏ qua tham số world
}

public record WorldGuardArea(String regionId, @Nullable WorldGuardHook hook) implements Area {
    // contains: hook == null -> false; ngược lại return hook.contains(world, regionId, x, y, z);
    // hook có thể null khi WorldGuard không cài (khi đó region luôn inactive và không bao giờ được tra)
}

public enum InactiveReason {
    WORLD_NOT_LOADED("reason-world-not-loaded", "world '%s' is not loaded"),
    WORLDGUARD_MISSING("reason-worldguard-missing", "WorldGuard is not installed"),
    WORLDGUARD_REGION_NOT_FOUND("reason-worldguard-region-not-found", "WorldGuard region '%s' not found in world '%s'");
    // field: messageKey (key trong messages.yml), consoleFormat (dùng cho log console)
}

public record BreakRegion(
    String name,                         // tên gốc như trong config
    String worldName,
    int priority,
    int index,                           // thứ tự khai báo trong file (0,1,2,...)
    Area area,
    NavigableMap<Integer, Double> speeds,// TreeMap, có thể rỗng
    @Nullable InactiveReason inactiveReason // null = active
) {
    public boolean active() { return inactiveReason == null; }

    /** @return tốc độ theo key lớn nhất <= level, hoặc null nếu không có */
    public Double speedFor(int level) {
        Map.Entry<Integer, Double> e = speeds.floorEntry(level);
        return e == null ? null : e.getValue();
    }
}

public record LoadResult(int active, int inactive, int skipped, int activeCuboid, int activeWorldGuard) {}
```
Lưu ý: `WorldGuardArea` giữ tham chiếu tới `WorldGuardHook`, nhưng bản thân nó không import class `com.sk89q.*` nào.

---

## 8. ConfigManager và RegionRegistry

### 8.1 ConfigManager
- Tự quản lý `File configFile = new File(dataFolder, "config.yml")` và một `YamlConfiguration config`. KHÔNG dùng `JavaPlugin#getConfig()`.
- `saveDefault()`: nếu file chưa có thì gọi `plugin.saveResource("config.yml", false)`.
- `@Nullable String load()`: trả về `null` nếu thành công, hoặc chuỗi mô tả lỗi nếu thất bại.
  - Tạo `YamlConfiguration` mới, gọi `options().parseComments(true)`, rồi `load(configFile)`.
  - Thành công: thay `config` bằng bản mới, set `broken = false`, return null.
  - Lỗi `IOException | InvalidConfigurationException e`:
    - Log SEVERE ra console: `config.yml has a syntax error and was not loaded:` cùng **toàn bộ** `e.getMessage()`.
    - GIỮ NGUYÊN `config` cũ (nếu chưa có thì tạo `YamlConfiguration` rỗng) và set `broken = true`.
    - Return bản rút gọn của lỗi để gửi cho người dùng: dòng đầu tiên không rỗng của `e.getMessage()`, cắt tối đa 200 ký tự. Message null thì dùng `e.getClass().getSimpleName()`.
- `boolean isBroken()`.
- `boolean save()`: gọi `config.save(configFile)`. Lỗi `IOException` thì log SEVERE và return false.
- `YamlConfiguration get()`.
- `@Nullable String findRegionKey(String name)`: tìm key trong section `regions` khớp `name` (không phân biệt hoa thường). Trả về key gốc hoặc null. Dùng để kiểm tra trùng tên và cho lệnh `remove`, bao gồm cả region bị skipped.
- `List<String> getRegionKeys()`: danh sách key trong section `regions` (rỗng nếu không có section).

**Quy tắc an toàn:** khi `isBroken()` là true, các lệnh `add`, `create`, `remove` phải từ chối và gửi `config-broken`. Lý do: tránh ghi đè làm mất file config đang bị lỗi cú pháp.

### 8.2 RegionRegistry
Các field:
- `double defaultSpeed`
- `List<BreakRegion> recorded`: mọi region được ghi nhận (active + inactive), theo thứ tự khai báo. Dùng cho `list` và kiểm tra trùng region WorldGuard.
- `Map<String, List<BreakRegion>> activeByWorld`: CHỈ region active. Key là tên world (phân biệt hoa thường, đúng như `World#getName()`). Mỗi list đã sắp xếp **priority giảm dần, cùng priority thì index tăng dần**.

Method `LoadResult load(YamlConfiguration cfg, Logger log, @Nullable WorldGuardHook wg)` dựng lại toàn bộ dữ liệu từ đầu.

1. **`default-speed`:** nếu là số và nằm trong khoảng [0, 1024] thì dùng. Nếu không: log WARNING `Invalid default-speed, using 1.0` và dùng 1.0.

2. **Section `regions`:** null hoặc không phải section thì coi như không có region. Duyệt `getKeys(false)` theo thứ tự trong file, `index` tăng dần.

   **(A) Lỗi cú pháp: SKIPPED.** Không ghi nhận, `skipped++`, log WARNING `Region '<name>' skipped: <lý do>`. Các trường hợp:
   - `name` không khớp regex `^[A-Za-z0-9_-]+$`.
   - Trùng tên với region trước đó (không phân biệt hoa thường).
   - `world` thiếu hoặc rỗng.
   - Có cả `pos1`/`pos2` lẫn `worldguard`, hoặc không có cái nào.
   - Cuboid: `pos1` hoặc `pos2` không phải list có đúng 3 số nguyên. Chấp nhận `Integer`/`Long`, và `Double` nếu không có phần lẻ. Plugin phải đọc được cả 2 cách viết list của YAML: `[1, 2, 3]` và dạng mỗi phần tử một dòng `- 1`.
   - `worldguard` rỗng.

   **(B) Các field phụ** (sai thì chỉ cảnh báo, region vẫn giữ):
   - `priority`: không có thì 0. Có nhưng không phải số nguyên thì WARNING và dùng 0.
   - `break-speed`: không có hoặc không phải section thì map rỗng, KHÔNG cảnh báo. Với từng key: parse sang `int`, phải ≥ 0. Value phải là số trong [0, 1024]. Entry nào sai thì WARNING và bỏ qua entry đó.

   **(C) Kiểm tra trạng thái**, theo thứ tự, dừng ở lỗi đầu tiên:
   1. `Bukkit.getWorld(world) == null` → `WORLD_NOT_LOADED`
   2. Loại worldguard và `wg == null` → `WORLDGUARD_MISSING`
   3. Loại worldguard và `!wg.regionExists(bukkitWorld, id)` → `WORLDGUARD_REGION_NOT_FOUND`
   4. Không lỗi nào → active (`inactiveReason = null`)

   Region inactive: VẪN ghi nhận vào `recorded`, KHÔNG đưa vào `activeByWorld`, `inactive++`, log WARNING `Region '<name>' is inactive: <consoleFormat>`. Region active: thêm vào cả `recorded` và `activeByWorld`, `active++`, rồi tăng `activeCuboid` hoặc `activeWorldGuard`.

   Area: cuboid thì `CuboidArea.of(...)`. Worldguard thì `WorldGuardArea(id, wg)`, `wg` có thể null.

3. Sắp xếp từng list trong `activeByWorld` bằng `Comparator.comparingInt(BreakRegion::priority).reversed().thenComparingInt(BreakRegion::index)`.

4. Log INFO: `Loaded <active+inactive> region(s): <active> active (<activeCuboid> cuboid, <activeWorldGuard> worldguard), <inactive> inactive, <skipped> skipped.`

5. Return `LoadResult`.

**Quy tắc:** region inactive KHÔNG BAO GIỜ được tra khi đào. Trạng thái active/inactive CHỈ được tính lại khi gọi `load(...)`: lúc bật plugin, `/rgs reload`, và sau `add`/`create`/`remove`. Không tự kiểm tra lại khi world được load hay khi region WorldGuard được tạo.

Các method tra cứu (chỉ dùng region active):
```java
@Nullable BreakRegion findRegion(Block block) {
    List<BreakRegion> list = activeByWorld.get(block.getWorld().getName());
    if (list == null) return null;
    int x = block.getX(), y = block.getY(), z = block.getZ();
    for (BreakRegion r : list) {
        if (r.area().contains(block.getWorld(), x, y, z)) return r;  // region đầu tiên = priority cao nhất
    }
    return null;
}

double resolveSpeed(Block block, int level) {
    BreakRegion r = findRegion(block);
    if (r == null) return defaultSpeed;
    Double s = r.speedFor(level);
    return s == null ? defaultSpeed : s;   // KHÔNG xét tiếp region priority thấp hơn
}
```
Các method hỗ trợ lệnh:
- `@Nullable BreakRegion findWorldGuardDuplicate(String world, String regionId)`: tìm trong `recorded` (cả active lẫn inactive) region loại worldguard có cùng world (bằng `equals`) và cùng id (`equalsIgnoreCase`).
- `List<BreakRegion> getRecorded()`.

---

## 9. Hooks

### 9.1 WorldGuardHook (CHỈ class này được import `com.sk89q.worldguard.*`)
Dùng `WorldGuard.getInstance().getPlatform().getRegionContainer()` và `com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(World)`.
- `boolean regionExists(World world, String id)`: lấy RegionManager theo world, null thì false. `getRegion(id) != null`.
- `boolean contains(World world, String id, int x, int y, int z)`: lấy RegionManager, null thì false. `ProtectedRegion r = rm.getRegion(id)`, null thì false (region bị xóa bên WorldGuard sau khi load thì coi như không chứa). `return r.contains(x, y, z)`.
- `@Nullable String getRegionId(World world, String id)`: trả về `r.getId()` (id chuẩn hóa của WorldGuard) hoặc null.
- `List<String> getAllRegionKeys()`: với mỗi world trong `Bukkit.getWorlds()`, lấy mọi id trong `rm.getRegions().keySet()` trừ `__global__`. Trả về dạng `"<worldName>:<id>"`.

Để tránh trùng tên với `RegionManager` của WorldGuard, dùng tên đầy đủ (fully-qualified) hoặc đặt tên biến rõ ràng.

### 9.2 WorldEditHook (CHỈ class này và WorldGuardHook được import `com.sk89q.worldedit.*`)
```java
public record CuboidSelection(String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {}

public enum SelectionError { NONE_OR_INCOMPLETE, NOT_CUBOID }

// trả về CuboidSelection, hoặc ném SelectionException(SelectionError) do plugin tự định nghĩa
// (class lồng tĩnh, kế thừa Exception)
public CuboidSelection getSelection(Player player) throws SelectionException
```
Cách lấy vùng chọn:
1. `com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);`
2. `LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);`
3. `com.sk89q.worldedit.world.World weWorld = session.getSelectionWorld();`. Nếu null thì ném `NONE_OR_INCOMPLETE`.
4. `Region region = session.getSelection(weWorld);`. Bắt `IncompleteRegionException` và ném `NONE_OR_INCOMPLETE`.
5. `if (!(region instanceof CuboidRegion cuboid))` thì ném `NOT_CUBOID`.
6. `BlockVector3 p1 = cuboid.getPos1(), p2 = cuboid.getPos2();`. Lấy tọa độ bằng `getBlockX()/getBlockY()/getBlockZ()`. KHÔNG dùng `x()/y()/z()` vì các hàm này không có ở WorldEdit 7.2.
7. Tên world: `BukkitAdapter.adapt(weWorld).getName()`.

Hoạt động với cả WorldEdit lẫn FastAsyncWorldEdit, vì FAWE cung cấp cùng API.

### 9.3 Phát hiện hook (CHỈ trong onEnable, reload không phát hiện lại)
```java
Plugin wgPlugin = pm.getPlugin("WorldGuard");
boolean hasWG = wgPlugin != null && wgPlugin.isEnabled();
Plugin wePlugin = pm.getPlugin("WorldEdit");
if (wePlugin == null) wePlugin = pm.getPlugin("FastAsyncWorldEdit");
boolean hasWE = wePlugin != null && wePlugin.isEnabled();
```
- `hasWG && hasWE` thì `worldGuardHook = new WorldGuardHook()`, bọc trong try/catch `Throwable`. Lỗi thì log WARNING và để null. WorldGuard luôn cần WorldEdit nên kiểm tra cả hai.
- `hasWE` thì `worldEditHook = new WorldEditHook()`, cũng bọc try/catch `Throwable`.
- Log INFO: `WorldGuard hook: enabled/disabled`, `WorldEdit hook: enabled (<tên plugin>)/disabled`.

---

## 10. MMOItems

### 10.1 RegionalBreakSpeedLevelStat (kế thừa `net.Indyuce.mmoitems.stat.type.DoubleStat`)
- `public static final String ID = "REGIONAL_BREAK_SPEED_LEVEL";`
  → MMOItems tự sinh key YAML `regional-break-speed-level` và NBT path `MMOITEMS_REGIONAL_BREAK_SPEED_LEVEL`.
- Constructor:
  ```java
  super(ID, Material.GOLDEN_PICKAXE, "Regional Break Speed Level",
        new String[]{"Level used by RegionalBreakSpeed to set", "block break speed inside configured regions.", "Must be a whole number greater than 0."},
        new String[]{"tool"});
  ```
- **Chặn nhập trong editor:** override `whenInput(EditionInventory inv, String message, Object... info)`:
  ```java
  String m = message.trim();
  int v;
  try { v = Integer.parseInt(m); } catch (NumberFormatException e) { v = -1; }
  if (!m.matches("\\d+") || v <= 0)
      throw new IllegalArgumentException("Value must be a whole number greater than 0.");
  super.whenInput(inv, m, info);
  ```
  MMOItems bắt `IllegalArgumentException`, báo lỗi cho người nhập và cho nhập lại.
- **Không hiện lore:** override `whenApplied(ItemStackBuilder item, DoubleData data)` để CHỈ ghi NBT:
  `item.addItemTag(getAppliedNBT(data));`. KHÔNG gọi `super`, KHÔNG đụng tới lore.
- Nếu class cha có `whenPreviewed(...)`: override sao cho chỉ ghi NBT của giá trị hiện tại, không thêm lore. Ví dụ gọi `whenApplied(item, currentData)` khi `currentData` là `DoubleData`.
- **BẮT BUỘC:** trước khi viết, mở jar MMOItems 6.10 (decompile hoặc xem source) để kiểm tra đúng chữ ký (signature) của constructor `DoubleStat`, `whenInput`, `whenApplied`, `whenPreviewed`, `getAppliedNBT` và `ItemStackBuilder#addItemTag`. Viết theo đúng chữ ký thực tế. Các chữ ký ở trên là dự kiến.

### 10.2 Đăng ký stat (trong `onLoad()` của main class)
```java
try {
    MMOItems.plugin.getStats().register(new RegionalBreakSpeedLevelStat());
} catch (Throwable t) {
    getLogger().log(Level.SEVERE, "Failed to register MMOItems stat", t);
}
```
Phải đăng ký ở `onLoad()`, vì MMOItems load item template trong `onEnable` của nó.

### 10.3 LevelReader
```java
public static final String NBT_KEY = "MMOITEMS_REGIONAL_BREAK_SPEED_LEVEL";

public static int getLevel(@Nullable ItemStack item) {
    if (item == null || item.getType().isAir()) return 0;
    NBTItem nbt = NBTItem.get(item);           // io.lumine.mythic.lib.api.item.NBTItem
    if (!nbt.hasTag(NBT_KEY)) return 0;
    int level = (int) Math.floor(nbt.getDouble(NBT_KEY));   // số lẻ -> làm tròn xuống
    return Math.max(level, 0);
}
```
Ở bước đọc KHÔNG kiểm tra loại item. Chỉ item TOOL (hoặc loại con của TOOL) mới gắn được stat này.

---

## 11. BreakSpeedListener

```java
@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
public void onBlockDamage(BlockDamageEvent event) {
    Player player = event.getPlayer();
    GameMode gm = player.getGameMode();
    if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;

    int level = LevelReader.getLevel(player.getInventory().getItemInMainHand());
    double speed = plugin.getRegistry().resolveSpeed(event.getBlock(), level);

    AttributeInstance inst = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
    if (inst == null) return;
    if (Math.abs(inst.getBaseValue() - speed) < 1.0E-6) return;   // trùng thì không set
    inst.setBaseValue(speed);
}

@EventHandler
public void onQuit(PlayerQuitEvent event) {
    RegionalBreakSpeed.resetSpeed(event.getPlayer());
}
```
`resetSpeed(Player)` là static helper: lấy AttributeInstance, nếu khác null thì `setBaseValue(inst.getDefaultValue())` (giá trị vanilla 1.0).

- Listener luôn lấy registry qua `plugin.getRegistry()` mỗi lần, KHÔNG lưu cứng registry cũ trong listener.
- `Attribute.BLOCK_BREAK_SPEED` là `org.bukkit.attribute.Attribute.BLOCK_BREAK_SPEED` (có từ 1.21.3).

---

## 12. Lệnh: RgsCommand (implements `CommandExecutor`, `TabCompleter`)

Đăng ký: `getCommand("regionalbreakspeed").setExecutor(cmd); ...setTabCompleter(cmd);`

Thứ tự hiển thị lệnh con: `help, add, create, remove, list, reload`. Permission của mỗi lệnh là `regionalbreakspeed.<sub>`. Sau đây "gửi `X`" nghĩa là `messages.send(sender, "X", "label", label, ...)`.

Helper `reloadRegistry()` = `registry.load(configManager.get(), getLogger(), worldGuardHook)`.

**Chung:**
- Không có tham số: xử lý như `help`.
- Lệnh con không có trong danh sách: gửi `unknown-command`.
- Không có quyền lệnh con: gửi `no-permission`.
- Tên lệnh con không phân biệt hoa thường.

**`help`:** gửi `help-header` (raw). Với mỗi lệnh con mà sender CÓ quyền, gửi `help-line` (raw) với `usage` và `description = messages.helpDescription(sub)`. Usage:
- help: `/{label} help`
- add: `/{label} add <world:region> [name]`
- create: `/{label} create <name>`
- remove: `/{label} remove <name>`
- list: `/{label} list [page]`
- reload: `/{label} reload`

**`add <world:region> [name]`:**
1. `worldGuardHook == null`: gửi `worldguard-missing`.
2. Config đang lỗi: gửi `config-broken`.
3. Thiếu tham số: gửi `usage-add`.
4. Tách tham số tại dấu `:` **đầu tiên**. Không có `:` hoặc một bên rỗng: gửi `invalid-format`.
5. `Bukkit.getWorld(world) == null`: gửi `world-not-found`.
6. `id = wg.getRegionId(world, regionArg)`. Null thì gửi `wg-region-not-found`.
7. `registry.findWorldGuardDuplicate(world, id) != null`: gửi `wg-region-already-added` kèm tên region đã có.
8. `name` = tham số 3 nếu có, không thì lấy `id`. Không khớp `^[A-Za-z0-9_-]+$`: gửi `invalid-name`. `configManager.findRegionKey(name) != null`: gửi `name-exists`.
9. Ghi vào config:
   - `regions.<name>.world = world`
   - `regions.<name>.worldguard = id`
   - `regions.<name>.break-speed` = section rỗng (`createSection`)
   - `regions.<name>.priority = 0`
10. `configManager.save()`. Thất bại thì gửi `save-failed` và return.
11. `reloadRegistry()`, rồi gửi `added`.

**`create <name>`:**
1. `worldEditHook == null`: gửi `worldedit-missing`.
2. Sender không phải `Player`: gửi `players-only`.
3. Config đang lỗi: gửi `config-broken`.
4. Thiếu tham số: gửi `usage-create`.
5. Tên không hợp lệ: gửi `invalid-name`. `findRegionKey(name) != null`: gửi `name-exists`.
6. `worldEditHook.getSelection(player)`:
   - `NONE_OR_INCOMPLETE`: gửi `no-selection`.
   - `NOT_CUBOID`: gửi `selection-not-cuboid`.
7. Ghi vào config:
   - `world`
   - `pos1 = List.of(x1, y1, z1)`, `pos2 = List.of(x2, y2, z2)` (giữ nguyên thứ tự pos1/pos2 của WorldEdit)
   - `break-speed` = section rỗng
   - `priority = 0`
8. Lưu (thất bại thì gửi `save-failed`), `reloadRegistry()`, gửi `created` kèm tọa độ.

**`remove <name>`:**
1. Config đang lỗi: gửi `config-broken`.
2. Thiếu tham số: gửi `usage-remove`.
3. `key = configManager.findRegionKey(name)`. Null thì gửi `region-not-found`. Xóa được cả region active, inactive và skipped.
4. `config.set("regions." + key, null)`.
5. Lưu (thất bại thì gửi `save-failed`), `reloadRegistry()`, gửi `removed` với `name = key`.

**`list [page]`:**
- Mỗi trang 10 region, lấy từ `registry.getRecorded()` (active + inactive, theo thứ tự trong file).
- Không có region nào: gửi `list-empty`.
- `page` không phải số, hoặc nằm ngoài khoảng 1..pages: gửi `invalid-page`.
- Gửi `list-header` (raw), rồi mỗi region một dòng:
  - Cuboid: `list-entry-cuboid` với min/max của CuboidArea trong vai trò x1..z2.
  - WorldGuard: `list-entry-worldguard`.
  - `{status}`: active thì `messages.get("status-active")`. Inactive thì `messages.get("status-inactive", "reason", messages.get(reason.messageKey()))`.
  - `levels` là danh sách key cách nhau bằng dấu phẩy, ví dụ `0,1,5`, hoặc `-` nếu rỗng.

**`reload`:**
1. `String error = configManager.load();`
2. `error != null`: gửi `reload-failed` với `error`, giữ nguyên registry cũ, vẫn reload messages, return. Console đã có log SEVERE từ ConfigManager.
3. Thành công: `messages.load()`, `LoadResult r = reloadRegistry()`, gửi `reloaded` với `active`, `inactive`, `skipped`. Nếu `r.inactive() + r.skipped() > 0` thì gửi thêm `reload-warnings`.

**Tab completion** (luôn trả về `List<String>`, KHÔNG trả về `null` vì null sẽ gợi ý tên người chơi; lọc theo `startsWith`, không phân biệt hoa thường):
- Tham số 1: các lệnh con mà sender có quyền.
- `add`, tham số 2: nếu có quyền add và `worldGuardHook != null` thì trả về `wg.getAllRegionKeys()`.
- `remove`, tham số 2: nếu có quyền remove thì trả về `configManager.getRegionKeys()`.
- `list`, tham số 2: các số trang `1..pages`.
- Mọi trường hợp khác: list rỗng.

---

## 13. Main class: RegionalBreakSpeed

- `onLoad()`: đăng ký stat MMOItems (mục 10.2).
- `onEnable()`:
  1. Tạo data folder, `configManager.saveDefault()`, và nếu chưa có `messages.yml` thì `saveResource("messages.yml", false)`.
  2. Phát hiện hook (mục 9.3).
  3. `messages.load()`.
  4. `configManager.load()`, rồi `registry.load(configManager.get(), getLogger(), worldGuardHook)`.
     - Nếu config lỗi cú pháp ngay từ đầu: ConfigManager đã log SEVERE. Log thêm SEVERE `Running with no regions until config.yml is fixed and /rgs reload is executed.` Registry khi đó rỗng, `defaultSpeed = 1.0`.
  5. Đăng ký `BreakSpeedListener`.
  6. Đăng ký command executor và tab completer.
- `onDisable()`: với mọi người chơi online, gọi `resetSpeed(player)`.
- Có getter: `getRegistry()`, `getMessages()`, `getConfigManager()`, `getWorldGuardHook()`, `getWorldEditHook()`.

---

## 14. Checklist test (sau khi build)

1. Server chỉ có MMOItems và MythicLib (không có WorldGuard/WorldEdit): plugin bật không lỗi. `/rgs add` báo `worldguard-missing`, `/rgs create` báo `worldedit-missing`. Region loại worldguard trong config là **inactive (WorldGuard not installed)**, vẫn hiện trong `/rgs list`.
2. MMOItems editor, item TOOL: thấy stat "Regional Break Speed Level". Nhập `2.5`, `abc`, `0` thì bị từ chối và được nhập lại. Nhập `3` thì nhận. Item tạo ra KHÔNG có dòng lore của stat.
3. Sửa tay TOOL.yml thành `regional-break-speed-level: 3.7`: item hoạt động như level 3.
4. Region cuboid có `break-speed: {1: 2.0, 5: 4.0}`:
   - Không cầm cúp: `default-speed`
   - Level 1–4: 2.0
   - Level 5 trở lên: 4.0
   - Đào ngoài region: `default-speed`
5. Thêm key `0: 0.3`: không cầm cúp có stat thì tốc độ là 0.3.
6. Hai region chồng nhau: priority cao hơn thắng. Cùng priority: region khai báo trước thắng. Region thắng không khớp level thì dùng `default-speed` (KHÔNG xét region thấp hơn).
7. Region có world chưa load, hoặc region WorldGuard chưa tạo:
   - Console có WARNING. `/rgs list` hiện inactive kèm lý do. Đào ở đó KHÔNG có tác dụng.
   - Tạo region WorldGuard (hoặc load world) rồi `/rgs reload`: region thành active và có tác dụng.
   - Chưa reload thì vẫn inactive.
8. `/rgs add world:abc`, `/rgs add world:abc custom_name`: config được ghi đúng, comment trong file còn nguyên. Thêm trùng region WorldGuard thì báo lỗi. Tên trùng thì báo lỗi, kể cả trùng với region inactive hoặc skipped.
9. `/rgs create test` với vùng chọn cuboid thì thành công. Vùng chọn poly thì báo `selection-not-cuboid`. Chưa chọn thì báo `no-selection`. Test với cả FAWE.
10. `/rgs remove` xóa được cả region inactive và skipped. `/rgs list` phân trang đúng.
11. Làm hỏng cú pháp config.yml rồi `/rgs reload`:
    - Console có SEVERE kèm chi tiết lỗi.
    - Người chạy lệnh nhận `reload-failed` kèm dòng lỗi.
    - Plugin vẫn dùng dữ liệu cũ. `add`, `create`, `remove` bị chặn bằng `config-broken`.
    - Sửa file rồi reload thì mọi thứ bình thường lại.
12. Người chơi thoát game hoặc tắt server: base value trả về 1.0.
13. Tab completion đúng như mục 12. Người không có quyền không thấy lệnh con tương ứng.
14. Chạy jar trên Spigot 1.21.4: không có lỗi `NoClassDefFoundError` hay `NoSuchMethodError`.

---

## 15. Những điều KHÔNG làm

- Không dùng Paper API hoặc Adventure. Không dùng `JavaPlugin#getConfig()` hay `saveConfig()`.
- Không dùng AttributeModifier. Set thẳng base value.
- Không thêm chế độ debug hay log mỗi lần đào. Không thêm lệnh `check`. Không gửi thông báo action bar hay title.
- Không dùng `BlockBreakEvent` để set tốc độ.
- Không xét region có priority thấp hơn khi region thắng không khớp level.
- Không ưu tiên region theo thể tích.
- Không tra region inactive khi đào. Không tự kiểm tra lại trạng thái inactive ngoài lúc `load(...)`.
- Không shade thư viện. Không chạy async.

## 16. Kết quả cần giao

Nội dung đầy đủ của TẤT CẢ các file ở mục 2, kèm lệnh build: `mvn clean package`. File output nằm ở `target/RegionalBreakSpeed-1.0.0.jar`.
