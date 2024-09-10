package de.hdg.keklist.commands;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.gui.GuiManager;
import de.hdg.keklist.util.IpUtil;
import de.hdg.keklist.util.TypeUtil;
import de.hdg.keklist.util.mfa.MFAUtil;
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
import java.sql.ResultSet;
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
                                if (args.length == 3) {
                                    if (args[2].equalsIgnoreCase("vanilla")) {
                                        for (OfflinePlayer player : Bukkit.getWhitelistedPlayers()) {
                                            Bukkit.dispatchCommand(sender, "keklist whitelist add " + player.getName());
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


                    case "2fa" -> {
                        if (!sender.hasPermission("keklist.2fa.use")) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                            return false;
                        }

                        if (!Keklist.getInstance().getConfig().getBoolean("2fa.enabled")) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.feature-disabled")));
                            return false;
                        }

                        if (sender instanceof Player player) {
                            switch (args[1]) {
                                case "enable" -> {
                                    if (!MFAUtil.hasMFAEnabled(player)) {
                                        MFAUtil.setupPlayer(player);
                                    } else {
                                        player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.already-enabled")));
                                    }

                                }

                                case "disable" -> {
                                    if (MFAUtil.hasMFAEnabled(player)) {
                                        if (args.length >= 3) {
                                            String code = args[2];

                                            if (MFAUtil.validateCode(player, code) || MFAUtil.validateRecoveryCode(player, code)) {
                                                MFAUtil.disableMFA(player);
                                                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.disabled")));
                                            } else {
                                                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.invalid-code")));
                                            }

                                        } else
                                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.no-code")));

                                    } else {
                                        player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.already-disabled")));
                                    }
                                }

                                case "codes" -> {
                                    if (MFAUtil.hasMFAEnabled(player)) {
                                        try (ResultSet rs = Keklist.getDatabase().onQuery("SELECT recoveryCodes FROM mfa WHERE uuid = ?", player.getUniqueId().toString())) {
                                            if (rs.next()) {
                                                String[] recoveryCodes = rs.getString("recoveryCodes").split(",");
                                                StringBuilder builder = new StringBuilder();

                                                for (String code : recoveryCodes) {
                                                    code = code.replace("[", "").replace("]", "").replace(",", "");

                                                    if ((builder.length() / 2) != 1
                                                            && !code.equals(recoveryCodes[recoveryCodes.length - 1])) {
                                                        builder.append(code).append(" | ");
                                                    } else
                                                        builder.append(code).append("\n");
                                                }

                                                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.codes", builder.toString())));
                                            }
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }

                                    } else {
                                        player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.required")));
                                    }
                                }

                                case "status" -> {
                                    if (MFAUtil.hasMFAEnabled(player)) {
                                        player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.status.enabled")));
                                    } else {
                                        player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.status.disabled")));
                                    }
                                }

                                case "verify" -> {
                                    if (args.length <= 3) {
                                        if (MFAUtil.hasMFAEnabled(player)) {

                                            if (MFAUtil.validateCode(player, args[2])) {
                                                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.verified")));
                                                MFAUtil.setVerified(player, true);
                                            } else {
                                                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.invalid-code")));
                                                MFAUtil.setVerified(player, false);
                                            }

                                        } else
                                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.required")));
                                    } else
                                        player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.no-code")));
                                }
                            }

                        } else {
                            if (args[1].equalsIgnoreCase("delete")) {
                                if (Keklist.getInstance().getConfig().getBoolean("2fa.console-can-delete-2fa")) {
                                    if (args.length >= 3) {
                                        try (ResultSet rs = Keklist.getDatabase().onQuery("SELECT 1 FROM mfa WHERE secret = ?", args[2])) {

                                            if (rs.next()) {
                                                Keklist.getDatabase().onUpdate("DELETE FROM mfa WHERE secret = ?", args[2]);
                                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.deleted", args[2])));
                                            } else
                                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.not-enabled", args[2])));

                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }

                                    } else
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.no-uuid")));
                                } else
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.feature-disabled")));
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("player-only")));
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
                            if (Bukkit.getPluginManager().getPlugin("BKCommonLib") == null && Keklist.getInstance().getConfig().getBoolean("2fa.enabled")) {
                                Keklist.getInstance().getLogger().warning(Keklist.getTranslations().get("2fa.bkcommonlib"));
                                Keklist.getInstance().getConfig().set("2fa.enabled", false);
                            }

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

            if (sender.hasPermission("keklist.2fa.use") &&
                    Keklist.getInstance().getConfig().getBoolean("2fa.enabled"))
                suggestions.add("2fa");

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

                suggestions.addAll(List.of("enable", "disable"));
                suggestions.add("import");

            } else if (args[0].equalsIgnoreCase("blacklist")
                    && Keklist.getInstance().getConfig().getBoolean("enable-manage-command")
                    && sender.hasPermission("keklist.manage.blacklist")) {

                suggestions.addAll(List.of("enable", "disable", "allow-blacklisted", "disallow-blacklisted"));
            } else if (args[0].equalsIgnoreCase("info")
                    && sender.hasPermission("keklist.info.use")) {

                suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                suggestions.add("1.1.1.1");
            } else if (args[0].equalsIgnoreCase("2fa")
                    && sender.hasPermission("keklist.2fa.use")
                    && Keklist.getInstance().getConfig().getBoolean("2fa.enabled")) {

                if (sender instanceof Player player)
                    if(MFAUtil.hasMFAEnabled(player))
                        suggestions.addAll(List.of("disable", "codes", "status", "verify"));
                    else
                        suggestions.addAll(List.of("enable", "status"));

                else
                    suggestions.add("delete");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("whitelist")
                    && args[1].equalsIgnoreCase("import")
                    && sender.hasPermission("keklist.manage.whitelist")
                    && Keklist.getInstance().getConfig().getBoolean("enable-manage-command")) {

                suggestions.add("vanilla");
            }

            if (args[0].equalsIgnoreCase("2fa")
                    && sender.hasPermission("keklist.2fa.use")
                    && !(sender instanceof Player)
                    && Keklist.getInstance().getConfig().getBoolean("2fa.console-can-delete-2fa")
                    && Keklist.getInstance().getConfig().getBoolean("2fa.enabled")) {

                try (ResultSet rs = Keklist.getDatabase().onQuery("SELECT uuid FROM mfa LIMIT 10")) {
                    while (rs.next()) {
                        suggestions.add(rs.getString("uuid"));
                    }

                    suggestions.add("uuid");
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return suggestions;
    }
}
