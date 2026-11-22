package com.namvuive.regionalbreakspeed.command;

import com.namvuive.regionalbreakspeed.RegionalBreakSpeed;
import com.namvuive.regionalbreakspeed.config.ConfigManager;
import com.namvuive.regionalbreakspeed.config.Messages;
import com.namvuive.regionalbreakspeed.hook.CuboidSelection;
import com.namvuive.regionalbreakspeed.hook.WorldEditHook;
import com.namvuive.regionalbreakspeed.hook.WorldGuardHook;
import com.namvuive.regionalbreakspeed.region.BreakRegion;
import com.namvuive.regionalbreakspeed.region.CuboidArea;
import com.namvuive.regionalbreakspeed.region.LoadResult;
import com.namvuive.regionalbreakspeed.region.RegionRegistry;
import com.namvuive.regionalbreakspeed.region.WorldGuardArea;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public class RgsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("help", "add", "create", "remove", "list", "reload");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final int PAGE_SIZE = 10;

    private final RegionalBreakSpeed plugin;

    public RgsCommand(RegionalBreakSpeed plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return help(sender, label);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help":
                return help(sender, label);
            case "add":
                return add(sender, label, args);
            case "create":
                return create(sender, label, args);
            case "remove":
                return remove(sender, label, args);
            case "list":
                return list(sender, label, args);
            case "reload":
                return reload(sender, label);
            default:
                messages().send(sender, "unknown-command", "label", label);
                return true;
        }
    }

    private boolean hasPermission(CommandSender sender, String sub, String label) {
        if (!sender.hasPermission("regionalbreakspeed." + sub)) {
            messages().send(sender, "no-permission", "label", label);
            return false;
        }
        return true;
    }

    private boolean help(CommandSender sender, String label) {
        if (!hasPermission(sender, "help", label)) {
            return true;
        }
        messages().sendRaw(sender, "help-header");
        for (String sub : SUBCOMMANDS) {
            if (!sender.hasPermission("regionalbreakspeed." + sub)) {
                continue;
            }
            messages().sendRaw(sender, "help-line",
                    "usage", "/" + label + " " + usageFor(sub),
                    "description", messages().helpDescription(sub));
        }
        return true;
    }

    private static String usageFor(String sub) {
        switch (sub) {
            case "add":
                return "add <world:region> [name]";
            case "create":
                return "create <name>";
            case "remove":
                return "remove <name>";
            case "list":
                return "list [page]";
            case "reload":
                return "reload";
            case "help":
            default:
                return "help";
        }
    }

    private boolean add(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "add", label)) {
            return true;
        }

        WorldGuardHook worldGuardHook = plugin.getWorldGuardHook();
        if (worldGuardHook == null) {
            messages().send(sender, "worldguard-missing", "label", label);
            return true;
        }

        ConfigManager configManager = plugin.getConfigManager();
        if (configManager.isBroken()) {
            messages().send(sender, "config-broken", "label", label);
            return true;
        }

        if (args.length < 2) {
            messages().send(sender, "usage-add", "label", label);
            return true;
        }

        String input = args[1];
        int colon = input.indexOf(':');
        if (colon < 0) {
            messages().send(sender, "invalid-format", "label", label);
            return true;
        }
        String worldName = input.substring(0, colon);
        String regionArgument = input.substring(colon + 1);
        if (worldName.isEmpty() || regionArgument.isEmpty()) {
            messages().send(sender, "invalid-format", "label", label);
            return true;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            messages().send(sender, "world-not-found", "world", worldName, "label", label);
            return true;
        }

        String regionId = worldGuardHook.getRegionId(world, regionArgument);
        if (regionId == null) {
            messages().send(sender, "wg-region-not-found",
                    "region", regionArgument, "world", worldName, "label", label);
            return true;
        }

        RegionRegistry registry = plugin.getRegistry();
        BreakRegion duplicate = registry.findWorldGuardDuplicate(worldName, regionId);
        if (duplicate != null) {
            messages().send(sender, "wg-region-already-added",
                    "world", worldName, "region", regionId, "name", duplicate.name(), "label", label);
            return true;
        }

        String name = args.length >= 3 ? args[2] : regionId;
        if (!NAME_PATTERN.matcher(name).matches()) {
            messages().send(sender, "invalid-name", "name", name, "label", label);
            return true;
        }
        if (configManager.findRegionKey(name) != null) {
            messages().send(sender, "name-exists", "name", name, "label", label);
            return true;
        }

        YamlConfiguration config = configManager.get();
        String base = "regions." + name;
        config.set(base + ".world", worldName);
        config.set(base + ".worldguard", regionId);
        config.set(base + ".priority", 0);

        if (!configManager.save()) {
            messages().send(sender, "save-failed", "label", label);
            return true;
        }

        reloadRegistry();
        messages().send(sender, "added",
                "world", worldName, "region", regionId, "name", name, "label", label);
        return true;
    }

    private boolean create(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "create", label)) {
            return true;
        }

        WorldEditHook worldEditHook = plugin.getWorldEditHook();
        if (worldEditHook == null) {
            messages().send(sender, "worldedit-missing", "label", label);
            return true;
        }

        if (!(sender instanceof Player player)) {
            messages().send(sender, "players-only", "label", label);
            return true;
        }

        ConfigManager configManager = plugin.getConfigManager();
        if (configManager.isBroken()) {
            messages().send(sender, "config-broken", "label", label);
            return true;
        }

        if (args.length < 2) {
            messages().send(sender, "usage-create", "label", label);
            return true;
        }

        String name = args[1];
        if (!NAME_PATTERN.matcher(name).matches()) {
            messages().send(sender, "invalid-name", "name", name, "label", label);
            return true;
        }
        if (configManager.findRegionKey(name) != null) {
            messages().send(sender, "name-exists", "name", name, "label", label);
            return true;
        }

        CuboidSelection selection;
        try {
            selection = worldEditHook.getSelection(player);
        } catch (WorldEditHook.SelectionException exception) {
            if (exception.getError() == WorldEditHook.SelectionError.NONE_OR_INCOMPLETE) {
                messages().send(sender, "no-selection", "label", label);
            } else {
                messages().send(sender, "selection-not-cuboid", "label", label);
            }
            return true;
        }

        YamlConfiguration config = configManager.get();
        String base = "regions." + name;
        config.set(base + ".world", selection.worldName());
        config.set(base + ".pos1", List.of(selection.x1(), selection.y1(), selection.z1()));
        config.set(base + ".pos2", List.of(selection.x2(), selection.y2(), selection.z2()));
        config.set(base + ".priority", 0);

        if (!configManager.save()) {
            messages().send(sender, "save-failed", "label", label);
            return true;
        }

        reloadRegistry();
        messages().send(sender, "created",
                "name", name,
                "world", selection.worldName(),
                "x1", String.valueOf(selection.x1()),
                "y1", String.valueOf(selection.y1()),
                "z1", String.valueOf(selection.z1()),
                "x2", String.valueOf(selection.x2()),
                "y2", String.valueOf(selection.y2()),
                "z2", String.valueOf(selection.z2()),
                "label", label);
        return true;
    }

    private boolean remove(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "remove", label)) {
            return true;
        }

        ConfigManager configManager = plugin.getConfigManager();
        if (configManager.isBroken()) {
            messages().send(sender, "config-broken", "label", label);
            return true;
        }

        if (args.length < 2) {
            messages().send(sender, "usage-remove", "label", label);
            return true;
        }

        String key = configManager.findRegionKey(args[1]);
        if (key == null) {
            messages().send(sender, "region-not-found", "name", args[1], "label", label);
            return true;
        }

        YamlConfiguration config = configManager.get();
        config.set("regions." + key, null);

        if (!configManager.save()) {
            messages().send(sender, "save-failed", "label", label);
            return true;
        }

        reloadRegistry();
        messages().send(sender, "removed", "name", key, "label", label);
        return true;
    }

    private boolean list(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "list", label)) {
            return true;
        }

        List<BreakRegion> recorded = plugin.getRegistry().getRecorded();
        if (recorded.isEmpty()) {
            messages().send(sender, "list-empty", "label", label);
            return true;
        }

        int pages = (recorded.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                page = -1;
            }
            if (page < 1 || page > pages) {
                messages().send(sender, "invalid-page", "pages", String.valueOf(pages), "label", label);
                return true;
            }
        }

        messages().sendRaw(sender, "list-header",
                "page", String.valueOf(page),
                "pages", String.valueOf(pages),
                "total", String.valueOf(recorded.size()));

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, recorded.size());
        for (int i = start; i < end; i++) {
            sendListEntry(sender, recorded.get(i));
        }
        return true;
    }

    private void sendListEntry(CommandSender sender, BreakRegion region) {
        String status;
        if (region.active()) {
            status = messages().get("status-active");
        } else {
            status = messages().get("status-inactive",
                    "reason", messages().get(region.inactiveReason().messageKey()));
        }

        String levels;
        if (region.speeds().isEmpty()) {
            levels = "-";
        } else {
            StringJoiner joiner = new StringJoiner(",");
            for (Integer level : region.speeds().keySet()) {
                joiner.add(String.valueOf(level));
            }
            levels = joiner.toString();
        }

        if (region.area() instanceof CuboidArea cuboid) {
            messages().sendRaw(sender, "list-entry-cuboid",
                    "name", region.name(),
                    "status", status,
                    "world", region.worldName(),
                    "x1", String.valueOf(cuboid.minX()),
                    "y1", String.valueOf(cuboid.minY()),
                    "z1", String.valueOf(cuboid.minZ()),
                    "x2", String.valueOf(cuboid.maxX()),
                    "y2", String.valueOf(cuboid.maxY()),
                    "z2", String.valueOf(cuboid.maxZ()),
                    "priority", String.valueOf(region.priority()),
                    "levels", levels);
        } else if (region.area() instanceof WorldGuardArea area) {
            messages().sendRaw(sender, "list-entry-worldguard",
                    "name", region.name(),
                    "status", status,
                    "world", region.worldName(),
                    "region", area.regionId(),
                    "priority", String.valueOf(region.priority()),
                    "levels", levels);
        }
    }

    private boolean reload(CommandSender sender, String label) {
        if (!hasPermission(sender, "reload", label)) {
            return true;
        }

        ConfigManager configManager = plugin.getConfigManager();
        String error = configManager.load();
        if (error != null) {
            messages().load();
            messages().send(sender, "reload-failed", "error", error, "label", label);
            return true;
        }

        messages().load();
        LoadResult result = reloadRegistry();
        messages().send(sender, "reloaded",
                "active", String.valueOf(result.active()),
                "inactive", String.valueOf(result.inactive()),
                "skipped", String.valueOf(result.skipped()),
                "label", label);
        if (result.inactive() + result.skipped() > 0) {
            messages().send(sender, "reload-warnings", "label", label);
        }
        return true;
    }

    private LoadResult reloadRegistry() {
        return plugin.getRegistry().load(
                plugin.getConfigManager().get(),
                plugin.getLogger(),
                plugin.getWorldGuardHook());
    }

    private Messages messages() {
        return plugin.getMessages();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String sub : SUBCOMMANDS) {
                if (sender.hasPermission("regionalbreakspeed." + sub)
                        && sub.startsWith(prefix)) {
                    result.add(sub);
                }
            }
            return result;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String prefix = args[1].toLowerCase(Locale.ROOT);

            if (sub.equals("add") && sender.hasPermission("regionalbreakspeed.add")) {
                WorldGuardHook hook = plugin.getWorldGuardHook();
                if (hook != null) {
                    for (String key : hook.getAllRegionKeys()) {
                        if (key.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                            result.add(key);
                        }
                    }
                }
            } else if (sub.equals("remove") && sender.hasPermission("regionalbreakspeed.remove")) {
                for (String key : plugin.getConfigManager().getRegionKeys()) {
                    if (key.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        result.add(key);
                    }
                }
            } else if (sub.equals("list") && sender.hasPermission("regionalbreakspeed.list")) {
                int total = plugin.getRegistry().getRecorded().size();
                int pages = (total + PAGE_SIZE - 1) / PAGE_SIZE;
                for (int i = 1; i <= pages; i++) {
                    String page = String.valueOf(i);
                    if (page.startsWith(args[1])) {
                        result.add(page);
                    }
                }
            }
        }

        return result;
    }
}
