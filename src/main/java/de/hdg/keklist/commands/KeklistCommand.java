package de.hdg.keklist.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.commands.type.BrigadierCommand;
import de.hdg.keklist.commands.type.CommandData;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.gui.GuiManager;
import de.hdg.keklist.util.IpUtil;
import de.hdg.keklist.util.TypeUtil;
import de.hdg.keklist.util.mfa.MFAUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import lombok.Cleanup;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

@DefaultQualifier(NotNull.class)
public class KeklistCommand implements BrigadierCommand {

    @Override
    @CommandData(
            name = "keklist",
            descriptionKey = "keklist.description",
            aliases = {"kek"}
    )
    public @NotNull LiteralCommandNode<CommandSourceStack> getCommand() {
        LiteralArgumentBuilder<CommandSourceStack> blacklist = addExecutesLast(Commands.literal("blacklist")
                .then(Commands.literal("enable"))
                .then(Commands.literal("disable"))
                .then(Commands.literal("allow-blacklisted"))
                .then(Commands.literal("disallow-blacklisted"))
                .requires(sender -> sender.getSender().hasPermission("keklist.manage.blacklist") && Keklist.getInstance().getConfig().getBoolean("enable-manage-command")), this::execute);


        LiteralArgumentBuilder<CommandSourceStack> whitelist = Commands.literal("whitelist")
                .then(Commands.literal("import")
                        .then(Commands.literal("vanilla").executes(this::execute))
                )
                .then(Commands.literal("enable").executes(this::execute))
                .then(Commands.literal("disable").executes(this::execute))
                .then(Commands.literal("level")
                        .then(Commands.argument("whitelist level", IntegerArgumentType.integer(0)).executes(this::execute))
                )
                .requires(sender -> sender.getSender().hasPermission("keklist.manage.whitelist") && Keklist.getInstance().getConfig().getBoolean("enable-manage-command"));


        LiteralArgumentBuilder<CommandSourceStack> info = Commands.literal("info")
                .then(Commands.argument("entry", StringArgumentType.word()).suggests((ctx, builder) -> {
                    Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(entry -> entry.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                            .forEach(builder::suggest);

                    if ("1.1.1.1".startsWith(builder.getRemainingLowerCase()))
                        builder.suggest("1.1.1.1");

                    return builder.buildFuture();
                }).executes(this::execute).then(Commands.argument("player (automatically)", StringArgumentType.word()).executes(this::execute)))
                .requires(sender -> sender.getSender().hasPermission("keklist.info.use"));

        LiteralArgumentBuilder<CommandSourceStack> mfa = Commands.literal("2fa")
                .then(Commands.literal("enable").requires(sender -> sender.getSender() instanceof Player player && !MFAUtil.hasMFAEnabled(player)).executes(this::execute))
                .then(Commands.literal("disable")
                        .then(Commands.argument("2fa code", StringArgumentType.word()))
                        .requires(sender -> sender.getSender() instanceof Player player && MFAUtil.hasMFAEnabled(player)).executes(this::execute))
                .then(Commands.literal("codes").requires(sender -> sender.getSender() instanceof Player player && MFAUtil.hasMFAEnabled(player)).executes(this::execute))
                .then(Commands.literal("status").requires(sender -> sender.getSender() instanceof Player).executes(this::execute))
                .then(Commands.literal("verify")
                        .then(Commands.argument("2fa code", StringArgumentType.word()))
                        .requires(sender -> sender.getSender() instanceof Player player && MFAUtil.hasMFAEnabled(player)).executes(this::execute))
                .then(Commands.literal("delete")
                        .then(Commands.argument("player", ArgumentTypes.player()).suggests((ctx, builder) -> {
                            try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT uuid FROM mfa LIMIT 10")) {
                                while (rs.resultSet().next()) {
                                    builder.suggest(rs.resultSet().getString("uuid"));
                                }
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }

                            return builder.buildFuture();
                        }).requires(sender -> !(sender.getSender() instanceof Player)).executes(this::execute)))
                .requires(sender -> sender.getSender().hasPermission("keklist.2fa.use") && Keklist.getInstance().getConfig().getBoolean("2fa.enabled"));

        LiteralArgumentBuilder<CommandSourceStack> advancedCommandRoot = Commands.literal("keklist")
                .then(blacklist)
                .then(whitelist)
                .then(info)
                .then(mfa)
                .then(Commands.literal("reload").requires(sender -> sender.getSender().hasPermission("keklist.manage.reload")).executes(this::execute))
                .then(Commands.literal("gui").requires(sender -> sender.getSender() instanceof Player && sender.getSender().hasPermission("keklist.gui.use")).executes(this::execute))
                .then(Commands.literal("status").requires(sender -> sender.getSender().hasPermission("keklist.status.use")).executes(this::execute)).executes(this::execute);

        return advancedCommandRoot.build();
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (ctx.getNodes().size() <= 1) {
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("invalid-syntax")));
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.usage.command")));
            return Command.SINGLE_SUCCESS;
        }

        switch (ctx.getNodes().get(1).getNode().getName()) {
            case "blacklist" -> {
                if (!sender.hasPermission("keklist.manage.blacklist")) {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                    return Command.SINGLE_SUCCESS;
                }

                if (Keklist.getInstance().getConfig().getBoolean("enable-manage-command")) {
                    switch (ctx.getNodes().get(1).getNode().getName()) {
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
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-enabled")));

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
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-disabled")));

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
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-allow-blacklisted")));

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
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-disallow-blacklisted")));

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
                    return Command.SINGLE_SUCCESS;
                }

                switch (ctx.getNodes().get(1).getNode().getName()) {
                    case "import" -> {
                        if (ctx.getNodes().get(2) != null) {
                            if (ctx.getNodes().get(2).getNode().getName().equalsIgnoreCase("vanilla")) {
                                for (OfflinePlayer player : Bukkit.getWhitelistedPlayers()) {
                                    Bukkit.dispatchCommand(sender, "keklist whitelist add " + player.getUniqueId());
                                }

                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.imported")));
                                return Command.SINGLE_SUCCESS;
                            }
                        }
                    }
                }

                if (Keklist.getInstance().getConfig().getBoolean("enable-manage-command")) {
                    switch (ctx.getNodes().get(1).getNode().getName()) {
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
                            if (ctx.getNodes().get(2) != null) {
                                try {
                                    int level = ctx.getArgument("whitelist level", Integer.class);

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
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.level.info", Keklist.getInstance().getConfig().getInt("whitelist.level"))));
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
                    return Command.SINGLE_SUCCESS;
                }

                String entry = ctx.getArgument("entry", String.class);

                TypeUtil.EntryType type = TypeUtil.getEntryType(entry);


                switch (type) {
                    case IPv4, IPv6 -> {
                        new IpUtil(entry).getIpData().thenAccept(data ->
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.ip-info")
                                        .replace("%ip%", entry)
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
                                        .replace("%player%", ctx.getLastChild().getNodes().getLast().getNode().getName().equals("player (automatically)") ? ctx.getArgument("player (automatically)", String.class) : "<grey><hover:show_text:'May be due to searching just an IP'>unknown</hover>")
                                ))
                        );
                    }

                    case JAVA, BEDROCK -> {
                        if (Bukkit.getPlayer(entry) != null) {
                            Player target = Bukkit.getPlayer(entry);
                            assert target != null;

                            try (DB.QueryResult whitelistedRs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelist WHERE uuid = ?", target.getUniqueId().toString());
                                 DB.QueryResult blacklistedRs = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklist WHERE uuid = ?", target.getUniqueId().toString())
                            ) {
                                boolean whitelisted = whitelistedRs.resultSet().next();
                                boolean blacklisted = blacklistedRs.resultSet().next();

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
                            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry);

                            try (DB.QueryResult whitelistedRs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelist WHERE uuid = ?", offlinePlayer.getUniqueId().toString());
                                 DB.QueryResult blacklistedRs = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklist WHERE uuid = ?", offlinePlayer.getUniqueId().toString())
                            ) {
                                boolean whitelisted = whitelistedRs.resultSet().next();
                                boolean blacklisted = blacklistedRs.resultSet().next();

                                String latestIp = "unknown";
                                int protocolId = -1;
                                String brand = "unknown";

                                @Cleanup DB.QueryResult lastSeenRs = Keklist.getDatabase().onQuery("SELECT 1 FROM lastSeen WHERE uuid = ?", offlinePlayer.getUniqueId().toString());

                                if (lastSeenRs.resultSet().next()) {
                                    latestIp = lastSeenRs.resultSet().getString("ip");
                                    protocolId = lastSeenRs.resultSet().getInt("protocolId");
                                    brand = lastSeenRs.resultSet().getString("brand");
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
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.info.unknown-type", ctx.getArgument("entry", String.class))));

                }
            }


            case "2fa" -> {
                if (!sender.hasPermission("keklist.2fa.use")) {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                    return Command.SINGLE_SUCCESS;
                }

                if (!Keklist.getInstance().getConfig().getBoolean("2fa.enabled")) {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.feature-disabled")));
                    return Command.SINGLE_SUCCESS;
                }

                if (sender instanceof Player player) {
                    switch (ctx.getNodes().get(1).getNode().getName()) {
                        case "enable" -> {
                            if (!MFAUtil.hasMFAEnabled(player)) {
                                MFAUtil.setupPlayer(player);
                            } else {
                                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.already-enabled")));
                            }
                        }

                        case "disable" -> {
                            if (MFAUtil.hasMFAEnabled(player)) {
                                if (ctx.getNodes().get(2).getNode() != null) {
                                    String code = ctx.getArgument("2fa code", String.class);

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
                                try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT recoveryCodes FROM mfa WHERE uuid = ?", player.getUniqueId().toString())) {
                                    if (rs.resultSet().next()) {
                                        String[] recoveryCodes = rs.resultSet().getString("recoveryCodes").split(",");
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
                            if (ctx.getNodes().get(2) != null) {
                                if (MFAUtil.hasMFAEnabled(player)) {

                                    if (MFAUtil.validateCode(player, ctx.getArgument("2fa code", String.class))) {
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
                    if (ctx.getNodes().get(1).getNode().getName().equalsIgnoreCase("delete")) {
                        if (Keklist.getInstance().getConfig().getBoolean("2fa.console-can-delete-2fa")) {
                            if (ctx.getNodes().get(2) != null) {
                                try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM mfa WHERE uuid = ?", ctx.getArgument("player", String.class))) {

                                    if (rs.resultSet().next()) {
                                        Keklist.getDatabase().onUpdate("DELETE FROM mfa WHERE uuid = ?", ctx.getArgument("player", String.class));
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.deleted", ctx.getArgument("player", String.class))));

                                        Player player = Bukkit.getPlayer(UUID.fromString(ctx.getArgument("player", String.class)));

                                        if (player != null)
                                            player.kick(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.deleted.kick")));

                                    } else
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.not-enabled", ctx.getArgument("player", String.class))));

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


            case "reload" -> {
                if (!sender.hasPermission("keklist.manage.reload")) {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                    return Command.SINGLE_SUCCESS;
                }

                try {
                    if (Bukkit.getPluginManager().getPlugin("BKCommonLib") == null && Keklist.getInstance().getConfig().getBoolean("2fa.enabled")) {
                        Keklist.getInstance().getLogger().warning(Keklist.getTranslations().get("2fa.bkcommonlib"));
                        Keklist.getInstance().getConfig().set("2fa.enabled", false);
                    }

                    Keklist.getInstance().getConfig().load(new File(Keklist.getInstance().getDataFolder(), "config.yml"));

                    Bukkit.getOnlinePlayers().forEach(Player::updateCommands);

                    Keklist.getDatabase().reconnect();
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("config-reloaded")));
                } catch (IOException | InvalidConfigurationException e) {
                    throw new RuntimeException(e);
                }
            }

            case "gui" -> {
                if (!sender.hasPermission("keklist.gui.use")) {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                    return Command.SINGLE_SUCCESS;
                }

                GuiManager.openMainGUI((Player) sender);
            }

            case "status" -> {
                if (!sender.hasPermission("keklist.status.use")) {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                    return Command.SINGLE_SUCCESS;
                }

                try (DB.QueryResult whitelistedRs = Keklist.getDatabase().onQuery("SELECT SUM(c) FROM (SELECT COUNT(*) AS c FROM whitelist UNION ALL SELECT COUNT(*) FROM whitelistIp UNION ALL SELECT COUNT(*) FROM whitelistDomain) as whitelistCound");
                     DB.QueryResult blacklistedRs = Keklist.getDatabase().onQuery("SELECT SUM(c) FROM (SELECT COUNT(*) AS c FROM blacklist UNION ALL SELECT COUNT(*) FROM blacklistIp) as blacklistCount")
                ) {
                    int whitelisted = whitelistedRs.resultSet().getInt(1);
                    int blacklisted = blacklistedRs.resultSet().getInt(1);

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

        return Command.SINGLE_SUCCESS;
    }
}
