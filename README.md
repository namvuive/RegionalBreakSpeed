# RegionalBreakSpeed

Plugin Spigot/Paper 1.21.x dùng để điều chỉnh tốc độ đập block (`BLOCK_BREAK_SPEED`) theo từng region, dựa trên một stat tuỳ chỉnh của MMOItems.

## Yêu cầu

- Java 21
- Server Spigot / Paper / Purpur / Pufferfish **1.21.3+** (build với Spigot API 1.21.4)
- Bắt buộc: **MMOItems**, **MythicLib**
- Không bắt buộc: **WorldGuard**, **WorldEdit** hoặc **FastAsyncWorldEdit**

## Cài đặt

1. Build hoặc tải `RegionalBreakSpeed-1.0.0.jar`.
2. Chép jar vào thư mục `plugins/`.
3. Khởi động server, sau đó kiểm tra 2 file được tạo:
   - `plugins/RegionalBreakSpeed/config.yml`
   - `plugins/RegionalBreakSpeed/messages.yml`

## Cách hoạt động

Khi người chơi bắt đầu đập một block (`BlockDamageEvent`):

1. Plugin xác định block nằm trong region nào (nếu có).
2. Đọc stat `Regional Break Speed Level` trên item ở tay chính.
3. Tra bảng `break-speed` của region theo quy tắc: **level lớn nhất <= level của item**.
4. Đặt base value của attribute `BLOCK_BREAK_SPEED` cho người chơi.

Nếu block không nằm trong region active, hoặc level không khớp entry nào, plugin dùng `default-speed`.

Region hỗ trợ 2 loại:

- **Cuboid**: khai báo trực tiếp bằng `pos1` / `pos2`.
- **WorldGuard**: tham chiếu tới region WorldGuard trong cùng world.

Khi nhiều region chồng nhau: region có `priority` cao hơn thắng; cùng priority thì region khai báo trước trong file thắng. Region thắng không khớp level sẽ dùng `default-speed` (không xét region priority thấp hơn).

## Stat MMOItems

Stat có ID `REGIONAL_BREAK_SPEED_LEVEL` (key YAML `regional-break-speed-level`, NBT `MMOITEMS_REGIONAL_BREAK_SPEED_LEVEL`).

- Chỉ dùng cho item loại **TOOL**.
- Giá trị là số nguyên dương.
- Stat **không hiển thị trên lore** của item.
- Có thể sửa tay trong file item của MMOItems, ví dụ:

```yaml
regional-break-speed-level: 3
```

Số lẻ khi đọc (ví dụ `3.7`) sẽ được làm tròn xuống (`3`).

## Cấu hình region

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

- `default-speed`: áp dụng ngoài region hoặc khi level không khớp, hợp lệ trong khoảng `0.0 - 1024.0`.
- `break-speed`: tuỳ chọn. Nếu bỏ trống/không có, region dùng `default-speed`.
- `priority`: mặc định `0`.

Region được coi là **inactive** (vẫn giữ trong file nhưng bị bỏ qua) nếu lúc load/reload:

- world chưa được load,
- WorldGuard chưa cài (với region loại WorldGuard),
- region WorldGuard chưa tồn tại.

Trạng thái active/inactive chỉ được tính lại khi plugin bật, khi chạy `/rgs reload`, hoặc sau `add` / `create` / `remove`.

## Lệnh

| Lệnh | Mô tả | Quyền |
|---|---|---|
| `/rgs help` | Danh sách lệnh | `regionalbreakspeed.help` |
| `/rgs add <world:region> [name]` | Thêm region WorldGuard | `regionalbreakspeed.add` |
| `/rgs create <name>` | Tạo region cuboid từ vùng chọn WorldEdit | `regionalbreakspeed.create` |
| `/rgs remove <name>` | Xoá region | `regionalbreakspeed.remove` |
| `/rgs list [page]` | Liệt kê region (10 region/trang) | `regionalbreakspeed.list` |
| `/rgs reload` | Reload `config.yml` và `messages.yml` | `regionalbreakspeed.reload` |

Alias: `/regionalbreakspeed`, `/rgs`.

## Build

```bash
mvn clean package
```

Output: `target/RegionalBreakSpeed-1.0.0.jar`.

## Lưu ý

- Chỉ dùng API Bukkit/Spigot, không phụ thuộc Paper API hay Adventure.
- Không shade thư viện; các plugin phụ thuộc đều ở scope `provided`.
- Không hỗ trợ Folia; mọi xử lý chạy trên main thread.
- Plugin không có chế độ debug và không log mỗi lần đào.
