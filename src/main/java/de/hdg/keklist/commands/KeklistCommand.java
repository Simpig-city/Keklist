package de.hdg.keklist.commands;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.gui.GuiManager;
import de.hdg.keklist.util.IpUtil;
import de.hdg.keklist.util.TypeUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class KeklistCommand extends Command {

    public KeklistCommand() {
        super("keklist");
        setAliases(List.of("kek"));
        setUsage(Keklist.getTranslations().get("keklist.usage"));
        setDescription(Keklist.getTranslations().get("keklist.description"));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        switch (args) {
            case String[] a when a.length >= 2 -> {
                switch (args[0]) {
                    case "blacklist" -> {
                        if (!sender.hasPermission("keklist.manage.blacklist")) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                            return false;
                        }

                        if (Keklist.getInstance().getConfig().getBoolean("enable-manage-command")) {
                            switch (args[1]) {
                                case "enable" -> {
                                    if (!Keklist.getInstance().getConfig().getBoolean("blacklist.enabled")) {
                                        Keklist.getInstance().getConfig().set("blacklist.enabled", true);
                                        try {
                                            Keklist.getInstance().getConfig().save(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                                            for (Player player : Bukkit.getOnlinePlayers()) {
                                                if (player.hasPermission("keklist.manage.blacklist")) {
                                                    player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.enabled")));
                                                }
                                            }

                                            Bukkit.getConsoleSender().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.enabled")));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    } else {
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-enabled")));
                                    }
                                }

                                case "disable" -> {
                                    if (Keklist.getInstance().getConfig().getBoolean("blacklist.enabled")) {
                                        Keklist.getInstance().getConfig().set("blacklist.enabled", false);
                                        try {
                                            Keklist.getInstance().getConfig().save(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                                            for (Player player : Bukkit.getOnlinePlayers()) {
                                                if (player.hasPermission("keklist.manage.blacklist")) {
                                                    player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.disabled")));
                                                }
                                            }

                                            Bukkit.getConsoleSender().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.disabled")));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    } else {
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-disabled")));
                                    }
                                }

                                case "allow-blacklisted" -> {
                                    if (!Keklist.getInstance().getConfig().getBoolean("blacklist.allow-join-with-admin")) {
                                        Keklist.getInstance().getConfig().set("blacklist.allow-join-with-admin", true);
                                        try {
                                            Keklist.getInstance().getConfig().save(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                                            for (Player player : Bukkit.getOnlinePlayers()) {
                                                if (player.hasPermission("keklist.manage.blacklist")) {
                                                    player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.allow-blacklisted")));
                                                }
                                            }

                                            Bukkit.getConsoleSender().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.allow-blacklisted")));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    } else {
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-allow-blacklisted")));
                                    }
                                }

                                case "disallow-blacklisted" -> {
                                    if (Keklist.getInstance().getConfig().getBoolean("blacklist.allow-join-with-admin")) {
                                        Keklist.getInstance().getConfig().set("blacklist.allow-join-with-admin", false);
                                        try {
                                            Keklist.getInstance().getConfig().save(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                                            for (Player player : Bukkit.getOnlinePlayers()) {
                                                if (player.hasPermission("keklist.manage.blacklist")) {
                                                    player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.disallow-blacklisted")));
                                                }
                                            }

                                            Bukkit.getConsoleSender().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.disallow-blacklisted")));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    } else {
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-disallow-blacklisted")));
                                    }
                                }

                                default ->
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.usage.command")));

                            }
                        } else
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.manage.disabled")));
                    }

                    case "whitelist" -> {
                        if (!sender.hasPermission("keklist.manage.whitelist")) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                            return false;
                        }

                        switch (args[1]) {
                            case "import" -> {
                                if (args.length >= 3) {
                                    if (args[2].equalsIgnoreCase("vanilla")) {
                                        for (OfflinePlayer player : Bukkit.getWhitelistedPlayers()) {
                                            Bukkit.dispatchCommand(sender, "keklist whitelist add " + player.getUniqueId());
                                        }

                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.imported")));
                                        return true;
                                    }
                                }
                            }
                        }

                        if (Keklist.getInstance().getConfig().getBoolean("enable-manage-command")) {
                            switch (args[1]) {
                                case "enable" -> {
                                    if (!Keklist.getInstance().getConfig().getBoolean("whitelist.enabled")) {
                                        Keklist.getInstance().getConfig().set("whitelist.enabled", true);
                                        try {
                                            Keklist.getInstance().getConfig().save(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                                            for (Player player : Bukkit.getOnlinePlayers()) {
                                                if (player.hasPermission("keklist.manage.whitelist")) {
                                                    player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.enabled")));
                                                }
                                            }

                                            Bukkit.getConsoleSender().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.enabled")));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    } else {
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.already-enabled")));
                                    }
                                }

                                case "disable" -> {
                                    if (Keklist.getInstance().getConfig().getBoolean("whitelist.enabled")) {
                                        Keklist.getInstance().getConfig().set("whitelist.enabled", false);
                                        try {
                                            Keklist.getInstance().getConfig().save(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                                            for (Player player : Bukkit.getOnlinePlayers()) {
                                                if (player.hasPermission("keklist.manage.whitelist")) {
                                                    player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.disabled")));
                                                }
                                            }

                                            Bukkit.getConsoleSender().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.disabled")));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    } else {
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.already-disabled")));
                                    }
                                }

                                case "level" -> {
                                    if (args.length >= 3) {
                                        try {
                                            int level = Integer.parseInt(args[2]);

                                            Keklist.getInstance().getConfig().set("whitelist.level", level);

                                            try {
                                                Keklist.getInstance().getConfig().save(new File(Keklist.getInstance().getDataFolder(), "config.yml"));

                                                for (Player player : Bukkit.getOnlinePlayers()) {
                                                    if (player.hasPermission("keklist.manage.whitelist")) {
                                                        player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.level.set", level)));
                                                    }
                                                }

                                                Bukkit.getConsoleSender().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.level.set", level)));
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }

                                        } catch (NumberFormatException e) {
                                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.level.invalid")));
                                        }
                                    }
                                }

                                default ->
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.usage.command")));
                            }
                        } else
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.manage.disabled")));
                    }

                    case "info" -> {
                        if (!sender.hasPermission("keklist.info.use")) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                            return false;
                        }

                        TypeUtil.EntryType type = TypeUtil.getEntryType(args[1]);

                        switch (type) {
                            case IPv4, IPv6 -> {
                                new IpUtil(args[1]).getIpData().thenAccept(data ->
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.ip-info")
                                                .replace("%ip%", args[1])
                                                .replace("%country%", data.country())
                                                .replace("%country_code%", data.countryCode())
                                                .replace("%continent%", data.continent())
                                                .replace("%continent_code%", data.continentCode())
                                                .replace("%region%", data.regionName())
                                                .replace("%city%", data.city())
                                                .replace("%org%", data.org())
                                                .replace("%as%", data.as())
                                                .replace("%timezone%", data.timezone())
                                                .replace("%mobile%", data.mobile() ? "<green>" + Keklist.getTranslations().get("yes") : "<red>" + Keklist.getTranslations().get("no"))
                                                .replace("%proxy%", data.proxy() ? "<green>" + Keklist.getTranslations().get("yes") : "<red>" + Keklist.getTranslations().get("no"))
                                                .replace("%hosting%", data.hosting() ? "<green>" + Keklist.getTranslations().get("yes") : "<red>" + Keklist.getTranslations().get("no"))
                                                .replace("%query%", data.query())
                                                .replace("%player%", args.length >= 3 ? args[2] : "<grey><hover:show_text:'May be due to searching just an IP'>unknown</hover>")
                                        ))
                                );
                            }

                            case JAVA, BEDROCK -> {
                                if (Bukkit.getPlayer(args[1]) != null) {
                                    Player target = Bukkit.getPlayer(args[1]);
                                    assert target != null;

                                    try {
                                        boolean whitelisted = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelist WHERE uuid = ?", target.getUniqueId().toString()).next();
                                        boolean blacklisted = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklist WHERE uuid = ?", target.getUniqueId().toString()).next();

                                        String brand = target.getClientBrandName();
                                        int ping = target.getPing();
                                        int version = target.getProtocolVersion();
                                        long idle = target.getIdleDuration().getSeconds();
                                        GameMode mode = target.getGameMode();
                                        Location location = target.getLocation();

                                        String ip = Keklist.getTranslations().get("unknown");
                                        if (target.getAddress().getAddress() != null) {
                                            ip = target.getAddress().getAddress().getHostAddress();
                                        }

                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.player-info")
                                                .replace("%name%", target.getName())
                                                .replace("%uuid%", target.getUniqueId().toString())
                                                .replace("%whitelisted%", whitelisted ? "<green>" + Keklist.getTranslations().get("yes") : "<red>" + Keklist.getTranslations().get("no"))
                                                .replace("%blacklisted%", blacklisted ? "<green>" + Keklist.getTranslations().get("yes") : "<red>" + Keklist.getTranslations().get("no"))
                                                .replace("%brand%", brand == null ? "<red>" + Keklist.getTranslations().get("unknown") : brand)
                                                .replace("%ping%", String.valueOf(ping))
                                                .replace("%version%", String.valueOf(version))
                                                .replace("%idle%", String.valueOf(idle))
                                                .replace("%gamemode%", mode.toString())
                                                .replace("%x%", String.valueOf(location.getBlockX()))
                                                .replace("%y%", String.valueOf(location.getBlockY()))
                                                .replace("%z%", String.valueOf(location.getBlockZ()))
                                                .replace("%ip%", ip)
                                                .replace("%requested_by%", sender.getName())
                                        ));
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                } else {
                                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);

                                    try {
                                        boolean whitelisted = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelist WHERE uuid = ?", offlinePlayer.getUniqueId().toString()).next();
                                        boolean blacklisted = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklist WHERE uuid = ?", offlinePlayer.getUniqueId().toString()).next();

                                        String latestIp = "unknown";
                                        int protocolId = -1;
                                        String brand = "unknown";

                                        if (Keklist.getDatabase().onQuery("SELECT 1 FROM lastSeen WHERE uuid = ?", offlinePlayer.getUniqueId().toString()).next()) {
                                            latestIp = Keklist.getDatabase().onQuery("SELECT ip FROM lastSeen WHERE uuid = ?", offlinePlayer.getUniqueId().toString()).getString("ip");
                                            protocolId = Keklist.getDatabase().onQuery("SELECT protocolId FROM lastSeen WHERE uuid = ?", offlinePlayer.getUniqueId().toString()).getInt("protocolId");
                                            brand = Keklist.getDatabase().onQuery("SELECT brand FROM lastSeen WHERE uuid = ?", offlinePlayer.getUniqueId().toString()).getString("brand");
                                        }

                                        Location location = offlinePlayer.getLocation();

                                        if (location == null)
                                            location = new Location(Bukkit.getWorlds().getFirst(), 0, 100, 0);

                                        long lastSeen = offlinePlayer.getLastSeen();
                                        SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.offline-player-info")
                                                .replace("%name%", offlinePlayer.getName())
                                                .replace("%uuid%", offlinePlayer.getUniqueId().toString())
                                                .replace("%whitelisted%", whitelisted ? "<green>" + Keklist.getTranslations().get("yes") : "<red>" + Keklist.getTranslations().get("no"))
                                                .replace("%blacklisted%", blacklisted ? "<green>" + Keklist.getTranslations().get("yes") : "<red>" + Keklist.getTranslations().get("no"))
                                                .replace("%brand%", brand)
                                                .replace("%version%", protocolId == -1 ? Keklist.getTranslations().get("unknown") : String.valueOf(protocolId))
                                                .replace("%x%", String.valueOf(location.getBlockX()))
                                                .replace("%y%", String.valueOf(location.getBlockY()))
                                                .replace("%z%", String.valueOf(location.getBlockZ()))
                                                .replace("%ip%", latestIp)
                                                .replace("%requested_by%", sender.getName())
                                                .replace("%last_seen%", sdf.format(new Date(lastSeen)))
                                        ));

                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }

                            default ->
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.info.unknown-type", args[1])));

                        }
                    }

                    default ->
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.usage.command")));
                }
            }


            case String[] a when a.length == 1 -> {
                switch (args[0]) {
                    case "reload" -> {
                        if (!sender.hasPermission("keklist.manage.reload")) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                            return false;
                        }

                        try {
                            Keklist.getInstance().getConfig().load(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                            Keklist.getDatabase().reconnect();
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("config-reloaded")));
                        } catch (IOException | InvalidConfigurationException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    case "gui" -> {
                        if (!sender.hasPermission("keklist.gui.use")) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                            return false;
                        }

                        GuiManager.openMainGUI((Player) sender);
                    }

                    case "status" -> {
                        if (!sender.hasPermission("keklist.status.use")) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                            return false;
                        }

                        try {
                            int whitelisted = Keklist.getDatabase().onQuery("SELECT SUM(c) FROM (SELECT COUNT(*) AS c FROM whitelist UNION ALL SELECT COUNT(*) FROM whitelistIp UNION ALL SELECT COUNT(*) FROM whitelistDomain) as whitelistCound").getInt(1);
                            int blacklisted = Keklist.getDatabase().onQuery("SELECT SUM(c) FROM (SELECT COUNT(*) AS c FROM blacklist UNION ALL SELECT COUNT(*) FROM blacklistIp) as blacklistCount").getInt(1);

                            boolean whitelist = Keklist.getInstance().getConfig().getBoolean("whitelist.enabled");
                            boolean blacklist = Keklist.getInstance().getConfig().getBoolean("blacklist.enabled");

                            DB.DBType type = Keklist.getDatabase().getType();

                            String version = Keklist.getInstance().getPluginMeta().getVersion();

                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.info")
                                    .replace("%whitelisted%", String.valueOf(whitelisted))
                                    .replace("%blacklisted%", String.valueOf(blacklisted))
                                    .replace("%whitelist%", whitelist ? "<green>" + Keklist.getTranslations().get("enabled") : "<red>" + Keklist.getTranslations().get("disabled"))
                                    .replace("%whitelistLevel%", String.valueOf(Keklist.getInstance().getConfig().getInt("whitelist.level")))
                                    .replace("%blacklist%", blacklist ? "<green>" + Keklist.getTranslations().get("enabled") : "<red>" + Keklist.getTranslations().get("disabled"))
                                    .replace("%database%", type.toString())
                                    .replace("%version%", version)
                            ));

                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    default ->
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.usage.command")));
                }
            }

            default ->
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.usage.command")));

        }

        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        List<String> suggestions = new ArrayList<>();

        if (args.length < 2) {
            if (sender.hasPermission("keklist.gui.use"))
                suggestions.add("gui");

            if (sender.hasPermission("keklist.manage.reload"))
                suggestions.add("reload");

            if (sender.hasPermission("keklist.status.use"))
                suggestions.add("status");

            if (sender.hasPermission("keklist.info.use"))
                suggestions.add("info");

            if (Keklist.getInstance().getConfig().getBoolean("enable-manage-command")) {
                if (sender.hasPermission("keklist.manage.blacklist"))
                    suggestions.add("blacklist");

                if (sender.hasPermission("keklist.manage.whitelist"))
                    suggestions.add("whitelist");
            }

        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("whitelist")
                    && sender.hasPermission("keklist.manage.whitelist")
                    && Keklist.getInstance().getConfig().getBoolean("enable-manage-command")) {

                suggestions.addAll(List.of("enable", "disable", "level", "import"));

            } else if (args[0].equalsIgnoreCase("blacklist")
                    && Keklist.getInstance().getConfig().getBoolean("enable-manage-command")
                    && sender.hasPermission("keklist.manage.blacklist")) {

                suggestions.addAll(List.of("enable", "disable", "allow-blacklisted", "disallow-blacklisted"));
            } else if (args[0].equalsIgnoreCase("info")
                    && sender.hasPermission("keklist.info.use")) {

                suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                suggestions.add("1.1.1.1");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("whitelist")
                    && args[1].equalsIgnoreCase("import")
                    && sender.hasPermission("keklist.manage.whitelist")
                    && Keklist.getInstance().getConfig().getBoolean("enable-manage-command")) {

                suggestions.add("vanilla");
            }
        }

        return suggestions;
    }
}
