package de.hdg.keklist.commands;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.api.events.blacklist.*;
import de.hdg.keklist.commands.type.BrigadierCommand;
import de.hdg.keklist.commands.type.CommandData;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.util.LanguageUtil;
import de.hdg.keklist.extentions.WebhookManager;
import de.hdg.keklist.util.TypeUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.Cleanup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static de.hdg.keklist.util.TypeUtil.getEntryType;

public class BlacklistCommand implements BrigadierCommand {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().setStrictness(Strictness.LENIENT).create();
    private static final TypeToken<Map<String, String>> token = new TypeToken<>() {
    };

    @Override
    @CommandData(
            name = "blacklist",
            descriptionKey = "blacklist.description",
            aliases = {"bl"}
    )
    public @NotNull LiteralCommandNode<CommandSourceStack> getCommand() {
        LiteralArgumentBuilder<CommandSourceStack> addSubcommand = Commands.literal("add")
                .then(Commands.argument("entry", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(player -> {
                                try (DB.QueryResult rsUser = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklist WHERE name = ?", player.getName());
                                     DB.QueryResult rsIp = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklistIp WHERE ip = ?", player.getAddress().getAddress().getHostAddress())
                                ) {
                                    if (!rsUser.resultSet().next())
                                        builder.suggest(player.getName());

                                    if (!rsIp.resultSet().next())
                                        builder.suggest(player.getAddress().getAddress().getHostAddress() + "(" + player.getName() + ")");
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                            return builder.buildFuture();
                        })
                        .executes(this::execute)
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(this::execute)
                        )
                ).requires(sender -> sender.getSender().hasPermission("keklist.blacklist.add"));


        LiteralArgumentBuilder<CommandSourceStack> removeSubcommand = Commands.literal("remove")
                .then(Commands.argument("entry", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM (SELECT name AS entry FROM blacklist UNION ALL SELECT ip AS entry FROM blacklistIp) as blacklistEntries");
                                 DB.QueryResult rsMotd = Keklist.getDatabase().onQuery("SELECT ip FROM blacklistMotd")) {
                                List<String> blacklistEntries = new ArrayList<>();

                                while (rs.resultSet().next()) {
                                    blacklistEntries.add(rs.resultSet().getString(1));
                                }

                                while (rsMotd.resultSet().next()) {
                                    if (blacklistEntries.contains(rsMotd.resultSet().getString("ip"))) {
                                        continue;
                                    }

                                    blacklistEntries.add(rsMotd.resultSet().getString("ip") + "(motd)");
                                }

                                blacklistEntries.stream()
                                        .filter(entry -> entry.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                        .forEach(builder::suggest);
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }

                            return builder.buildFuture();
                        })
                        .executes(this::execute)
                ).requires(sender -> sender.getSender().hasPermission("keklist.blacklist.remove"));

        LiteralArgumentBuilder<CommandSourceStack> infoSubcommand = Commands.literal("info")
                .then(Commands.argument("entry", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM (SELECT name AS entry FROM blacklist UNION ALL SELECT ip AS entry FROM blacklistIp) as blacklistEntries");
                                 DB.QueryResult rsMotd = Keklist.getDatabase().onQuery("SELECT ip FROM blacklistMotd")) {
                                List<String> blacklistEntries = new ArrayList<>();

                                while (rs.resultSet().next()) {
                                    blacklistEntries.add(rs.resultSet().getString(1));
                                }

                                while (rsMotd.resultSet().next()) {
                                    if (blacklistEntries.contains(rsMotd.resultSet().getString("ip"))) {
                                        continue;
                                    }

                                    blacklistEntries.add(rsMotd.resultSet().getString("ip") + "(motd)");
                                }

                                blacklistEntries.stream()
                                        .filter(entry -> entry.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                        .forEach(builder::suggest);
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }

                            return builder.buildFuture();
                        })
                        .executes(this::execute)
                ).requires(sender -> sender.getSender().hasPermission("keklist.blacklist.info"));

        LiteralArgumentBuilder<CommandSourceStack> listSubcommand = Commands.literal("list")
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(this::execute)
                ).requires(sender -> sender.getSender().hasPermission("keklist.blacklist.list"));

        LiteralArgumentBuilder<CommandSourceStack> motdSubcommand = Commands.literal("motd")
                .then(Commands.argument("entry", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            List<String> blacklistEntries = new ArrayList<>();

                            Bukkit.getOnlinePlayers().forEach(player -> {
                                try (DB.QueryResult rsMotd = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklistMotd WHERE ip = ?", player.getAddress().getAddress().getHostAddress())) {
                                    if (!rsMotd.resultSet().next()) {
                                        blacklistEntries.add(player.getAddress().getAddress().getHostAddress() + "(" + player.getName() + ")");
                                    }
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                            blacklistEntries.stream()
                                    .filter(entry -> entry.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                    .forEach(builder::suggest);

                            return builder.buildFuture();
                        })
                        .executes(this::execute)
                ).requires(sender -> sender.getSender().hasPermission("keklist.blacklist.motd"));

        return Commands.literal("blacklist")
                .then(addSubcommand)
                .then(removeSubcommand)
                .then(infoSubcommand)
                .then(listSubcommand)
                .then(motdSubcommand)
                .build();
    }


    public int execute(@NotNull CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSender sender = ctx.getSource().getSender();
            String senderName = sender.getName();

            if (ctx.getNodes().size() <= 1) {
                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("invalid-syntax")));
                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.usage.command")));
                return Command.SINGLE_SUCCESS;
            }

            switch (ctx.getNodes().get(1).getNode().getName()) {
                case "add" -> {
                    if (!sender.hasPermission("keklist.blacklist.add")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return Command.SINGLE_SUCCESS;
                    }

                    String entry = ctx.getArgument("entry", String.class);
                    TypeUtil.EntryType type = getEntryType(entry);
                    String reason = null;
                    if (ctx.getLastChild().getNodes().getLast().getNode().getName().equals("reason"))
                        reason = ctx.getArgument("reason", String.class);


                    switch (type) {
                        case JAVA -> {
                            Request request = new Request.Builder().url("https://api.mojang.com/users/profiles/minecraft/" + entry).build();
                            client.newCall(request).enqueue(new UserBlacklistAddCallback(sender, reason, type));
                        }

                        case IPv4, IPv6 -> {
                            @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklistIp WHERE ip = ?", entry);

                            if (!rs.resultSet().next()) {
                                if (reason == null) {
                                    new IpAddToBlacklistEvent(entry, null).callEvent();
                                    Keklist.getDatabase().onUpdate("INSERT INTO blacklistIp (ip, byPlayer, unix) VALUES (?, ?, ?)", entry, senderName, System.currentTimeMillis());

                                    if (Keklist.getWebhookManager() != null)
                                        Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_ADD, entry, senderName, null, System.currentTimeMillis());

                                } else {
                                    if (reason.length() <= 1500) {
                                        new IpAddToBlacklistEvent(entry, reason).callEvent();
                                        Keklist.getDatabase().onUpdate("INSERT INTO blacklistIp (ip, byPlayer, unix, reason) VALUES (?, ?, ?, ?)", entry, senderName, System.currentTimeMillis(), reason);

                                        if (Keklist.getWebhookManager() != null)
                                            Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_ADD, entry, senderName, reason, System.currentTimeMillis());

                                    } else {
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.reason-too-long")));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                }

                                @Cleanup DB.QueryResult rsMotd = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklistMotd WHERE ip = ?", entry);
                                if (!rsMotd.resultSet().next()) {
                                    new IpAddToMOTDBlacklistEvent(entry).callEvent();
                                    Keklist.getDatabase().onUpdate("INSERT INTO blacklistMotd (ip, byPlayer, unix) VALUES (?, ?, ?)", entry, senderName, System.currentTimeMillis());
                                }

                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.added", entry)));

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.notify.add", entry, senderName)), "keklist.notify.blacklist");

                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-blacklisted", entry)));
                                return Command.SINGLE_SUCCESS;
                            }
                        }

                        case BEDROCK -> {
                            FloodgateApi api = Keklist.getInstance().getFloodgateApi();

                            try {
                                UUID bedrockUUID = api.getUuidFor(entry.replace(Keklist.getInstance().getConfig().getString("floodgate.prefix"), "")).get();
                                blacklistUser(sender, bedrockUUID, entry, reason);
                            } catch (Exception ex) {

                                if (Keklist.getInstance().getConfig().getString("floodgate.api-key") != null) {
                                    Request request = new Request.Builder()
                                            .url("https://mcprofile.io/api/v1/bedrock/gamertag/" + entry.replace(".", ""))
                                            .header("x-api-key", Keklist.getInstance().getConfig().getString("floodgate.api-key"))
                                            .build();

                                    client.newCall(request).enqueue(new BlacklistCommand.UserBlacklistAddCallback(sender, reason, type));
                                    return Command.SINGLE_SUCCESS;
                                } else
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("floodgate.api-key-not-set")));
                            }
                        }

                        default ->
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.invalid-argument")));
                    }
                }

                case "remove" -> {
                    if (!sender.hasPermission("keklist.blacklist.remove")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return Command.SINGLE_SUCCESS;
                    }

                    String entry = ctx.getArgument("entry", String.class);
                    TypeUtil.EntryType type = getEntryType(entry);

                    switch (type) {
                        case TypeUtil.EntryType.JAVA, TypeUtil.EntryType.BEDROCK -> {
                            @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklist WHERE name = ?", entry);
                            if (rs.resultSet().next()) {
                                new PlayerRemovedFromBlacklist(entry).callEvent();
                                Keklist.getDatabase().onUpdate("DELETE FROM blacklist WHERE name = ?", entry);

                                if (Keklist.getWebhookManager() != null)
                                    Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_REMOVE, entry, senderName, null, System.currentTimeMillis());

                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.removed", entry)));

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.notify.remove", entry, senderName)), "keklist.notify.blacklist");

                            } else {
                                @Cleanup DB.QueryResult rsUserFix = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklist WHERE name = ?", entry + " (Old Name)");
                                if (rsUserFix.resultSet().next()) {
                                    new PlayerRemovedFromBlacklist(entry).callEvent();
                                    Keklist.getDatabase().onUpdate("DELETE FROM blacklist WHERE name = ?", entry + " (Old Name)");

                                    if (Keklist.getWebhookManager() != null)
                                        Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_REMOVE, entry, senderName, null, System.currentTimeMillis());

                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.removed", entry) + " (Old Name)"));

                                    if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                        Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.notify.remove", entry, senderName + "(Old Name)")), "keklist.notify.blacklist");

                                } else {
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.not-blacklisted", entry)));
                                }
                            }
                        }

                        case TypeUtil.EntryType.IPv4, TypeUtil.EntryType.IPv6 -> {
                            @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklistIp WHERE ip = ?", entry);
                            @Cleanup DB.QueryResult rsMotd = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklistMotd WHERE ip = ?", entry);
                            if (rs.resultSet().next() || rsMotd.resultSet().next()) {
                                new IpRemovedFromBlacklistEvent(entry).callEvent();
                                new IpRemovedFromMOTDBlacklistEvent(entry).callEvent();

                                Keklist.getDatabase().onUpdate("DELETE FROM blacklistIp WHERE ip = ?", entry);
                                Keklist.getDatabase().onUpdate("DELETE FROM blacklistMotd WHERE ip = ?", entry);

                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.ip.removed", entry)));

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.notify.remove", entry, senderName)), "keklist.notify.blacklist");

                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.not-blacklisted", entry)));
                                return Command.SINGLE_SUCCESS;
                            }
                        }

                        default ->
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.invalid-argument")));

                    }
                }

                case "motd" -> {
                    if (!sender.hasPermission("keklist.blacklist.motd")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return Command.SINGLE_SUCCESS;
                    }

                    String entry = ctx.getArgument("entry", String.class);
                    TypeUtil.EntryType type = getEntryType(entry);

                    if (type.equals(TypeUtil.EntryType.IPv4)) {
                        @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklistMotd WHERE ip = ?", entry);
                        if (rs.resultSet().next()) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.motd.already-blacklisted", entry)));
                        } else {
                            new IpAddToMOTDBlacklistEvent(entry).callEvent();
                            Keklist.getDatabase().onUpdate("INSERT INTO blacklistMotd (ip, byPlayer, unix) VALUES (?, ?, ?)", entry, senderName, System.currentTimeMillis());
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.motd.added", entry)));

                            if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.notify.motd", entry, senderName)), "keklist.notify.blacklist");
                        }
                    } else
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.motd.syntax")));
                }

                case "info" -> {
                    if (!sender.hasPermission("keklist.blacklist.info")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return Command.SINGLE_SUCCESS;
                    }

                    String entry = ctx.getArgument("entry", String.class);
                    TypeUtil.EntryType type = getEntryType(entry);

                    switch (type) {
                        case TypeUtil.EntryType.IPv4, TypeUtil.EntryType.IPv6 -> {
                            @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", entry);
                            @Cleanup DB.QueryResult rsMotd = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", entry);

                            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));
                            LanguageUtil translations = Keklist.getTranslations();
                            MiniMessage miniMessage = Keklist.getInstance().getMiniMessage();

                            if (rs.resultSet().next()) {
                                sendInfo(rs, sender, entry);
                            } else if (rsMotd.resultSet().next()) {
                                String byPlayer = rsMotd.resultSet().getString("byPlayer");
                                String unix = sdf.format(rsMotd.resultSet().getLong("unix"));

                                sender.sendMessage(miniMessage.deserialize(translations.get("blacklist.info")));
                                sender.sendMessage(miniMessage.deserialize(translations.get("blacklist.info.entry", entry)));
                                sender.sendMessage(miniMessage.deserialize(translations.get("blacklist.info.by", byPlayer)));
                                sender.sendMessage(miniMessage.deserialize(translations.get("blacklist.info.at", unix)));
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.not-blacklisted", entry)));

                        }

                        case TypeUtil.EntryType.JAVA, TypeUtil.EntryType.BEDROCK -> {
                            @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE name = ?", entry);

                            if (rs.resultSet().next()) {
                                sendInfo(rs, sender, entry);
                            } else {
                                @Cleanup DB.QueryResult rsUserFix = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE name = ?", entry + " (Old Name)");
                                if (rsUserFix.resultSet().next()) {
                                    sendInfo(rsUserFix, sender, entry);
                                } else
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.not-blacklisted", entry)));
                            }
                        }

                        default ->
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.invalid-argument")));

                    }
                }

                case "list" -> handleList(sender, ctx.getArgument("page", Integer.class));

                default -> {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("invalid-syntax")));
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.usage.command")));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Command.SINGLE_SUCCESS;
    }

    private void sendInfo(@NotNull DB.QueryResult resultSet, @NotNull CommandSender sender, @NotNull String entry) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

            String byPlayer = resultSet.resultSet().getString("byPlayer");
            String blacklistedReason = resultSet.resultSet().getString("reason");
            blacklistedReason = blacklistedReason == null ? "No reason given" : blacklistedReason;
            String unix = sdf.format(resultSet.resultSet().getLong("unix"));

            LanguageUtil translations = Keklist.getTranslations();
            MiniMessage miniMessage = Keklist.getInstance().getMiniMessage();

            sender.sendMessage(miniMessage.deserialize(translations.get("blacklist.info")));
            sender.sendMessage(miniMessage.deserialize(translations.get("blacklist.info.entry", entry)));
            sender.sendMessage(miniMessage.deserialize(translations.get("blacklist.info.by", byPlayer)));
            sender.sendMessage(miniMessage.deserialize(translations.get("blacklist.info.at", unix)));
            sender.sendMessage(miniMessage.deserialize(translations.get("blacklist.info.reason", blacklistedReason)));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void blacklistUser(CommandSender from, UUID uuid, String playerName, String reason) {
        try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklist WHERE uuid = ?", uuid.toString());
             DB.QueryResult rsUserFix = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklist WHERE name = ?", playerName)
        ) {
            //User is not blacklisted
            if (!rs.resultSet().next()) {
                if (rsUserFix.resultSet().next()) {
                    Keklist.getDatabase().onUpdate("UPDATE blacklist SET name = ? WHERE name = ?", playerName + " (Old Name)", playerName);
                }

                if (reason == null) {
                    Bukkit.getScheduler().runTask(Keklist.getInstance(), () -> new UUIDAddToBlacklistEvent(uuid, null).callEvent());
                    Keklist.getDatabase().onUpdate("INSERT INTO blacklist (uuid, name, byPlayer, unix) VALUES (?, ?, ?, ?)", uuid.toString(), playerName, from.getName(), System.currentTimeMillis());

                    if (Keklist.getWebhookManager() != null)
                        Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_ADD, playerName, from.getName(), "No reason given!", System.currentTimeMillis());
                } else {
                    if (reason.length() <= 1500) {
                        Bukkit.getScheduler().runTask(Keklist.getInstance(), () -> new UUIDAddToBlacklistEvent(uuid, reason).callEvent());
                        Keklist.getDatabase().onUpdate("INSERT INTO blacklist (uuid, name, byPlayer, unix, reason) VALUES (?, ?, ?, ?, ?)", uuid.toString(), playerName, from.getName(), System.currentTimeMillis(), reason);

                        if (Keklist.getWebhookManager() != null)
                            Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_ADD, playerName, from.getName(), reason, System.currentTimeMillis());
                    } else {
                        from.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.reason-too-long")));
                        return;
                    }
                }

                Player blacklisted = Bukkit.getPlayer(playerName);
                if (blacklisted != null) {
                    DB.QueryResult rsMotd = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklistMotd WHERE ip = ?", blacklisted.getAddress().getAddress().getHostAddress());
                    if (!rsMotd.resultSet().next()) {
                        Bukkit.getScheduler().runTask(Keklist.getInstance(), () -> new IpAddToMOTDBlacklistEvent(blacklisted.getAddress().getAddress().getHostAddress()).callEvent());
                        Keklist.getDatabase().onUpdate("INSERT INTO blacklistMotd (ip, byPlayer, unix) VALUES (?, ?, ?)", blacklisted.getAddress().getAddress().getHostAddress(), from.getName(), System.currentTimeMillis());
                    }
                }

                from.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.added", playerName)));

                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.notify.add", playerName, from.getName())), "keklist.notify.blacklist");

            } else
                from.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-blacklisted", playerName)));

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private class UserBlacklistAddCallback implements Callback {
        private final CommandSender sender;
        private final String reason;
        private final TypeUtil.EntryType type;

        public UserBlacklistAddCallback(@NotNull CommandSender sender, String reason, TypeUtil.EntryType type) {
            this.sender = sender;
            this.reason = reason;
            this.type = type;
        }

        @Override
        public void onResponse(@NotNull Call call, Response response) throws IOException {
            String body = response.body().string();
            if (checkForGoodResponse(body, type) != null) {
                sender.sendMessage(checkForGoodResponse(body, type));

            } else if (response.code() == 429) {
                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("http.rate-limit")));
            } else {
                Map<String, String> map = gson.fromJson(body, token);

                String uuid;
                String name;

                if (type.equals(TypeUtil.EntryType.JAVA)) {
                    uuid = map.get("id");
                    name = map.get("name");
                } else {
                    uuid = map.get("floodgateuid");
                    name = Keklist.getInstance().getConfig().getString("floodgate.prefix") + map.get("gamertag");
                }

                blacklistUser(sender, UUID.fromString(uuid.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5")), name, reason);
            }

        }

        @Override
        public void onFailure(@NotNull Call call, IOException e) {
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("http.error")));
            sender.sendMessage(Component.text(Keklist.getTranslations().get("http.detail", e.getMessage())));
        }
    }

    private Component checkForGoodResponse(String response, TypeUtil.EntryType type) {
        JsonElement element = JsonParser.parseString(response);

        if (!element.isJsonNull()) {
            if (type.equals(TypeUtil.EntryType.JAVA)) {
                if (element.getAsJsonObject().get("error") != null ||
                        !element.getAsJsonObject().has("id") ||
                        !element.getAsJsonObject().has("name")) {
                    return Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("http.not-found", element.getAsJsonObject().get("error").getAsString()));
                }
            } else {
                if (element.getAsJsonObject().get("message") != null)
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("http.not-found", element.getAsJsonObject().get("message").getAsString()));

            }

        } else {
            return Component.text(Keklist.getTranslations().get("http.null-response"));
        }

        return null;
    }

    /**
     * Handles the blacklist list subcommand
     *
     * @param sender the sender who executed the command
     * @param page   the page to display
     */
    private void handleList(@NotNull CommandSender sender, @Range(from = 1, to = Integer.MAX_VALUE) int page) {
        try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM (SELECT uuid, byPlayer, unix FROM blacklist UNION ALL SELECT ip, byPlayer, unix FROM blacklistIp UNION SELECT ip, byPlayer, unix FROM blacklistMotd) as entries LIMIT ?,8", (page - 1) * 8)) {

            if (!rs.resultSet().next()) {
                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.list.empty")));
                return;
            }

            StringBuilder listMessage = new StringBuilder();

            listMessage.append("\n").append(Keklist.getTranslations().get("blacklist.list.header")).append("\n\n");

            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

            do {
                String entry = rs.resultSet().getString(1);
                String byPlayer = rs.resultSet().getString(2);
                String date = sdf.format(new Date(rs.resultSet().getLong(3)));

                TypeUtil.EntryType type = TypeUtil.getEntryType(entry);

                switch (type) {
                    case IPv4, IPv6, DOMAIN -> {
                        @Cleanup DB.QueryResult rsMotd = Keklist.getDatabase().onQuery("SELECT CASE WHEN EXISTS (SELECT 1 FROM blacklistIp WHERE ip = ?) THEN NULL ELSE (SELECT 1 FROM blacklistMotd WHERE ip = ?) END", entry, entry);
                        boolean isMotd = false;

                        if (rsMotd.resultSet().next())
                            isMotd = rsMotd.resultSet().getInt(1) == 1;

                        listMessage.append(Keklist.getTranslations().get("blacklist.list.entry", date, isMotd ? entry + " (MOTD)" : entry, byPlayer)).append("\n");
                    }

                    case UUID -> {
                        @Cleanup DB.QueryResult rsName = Keklist.getDatabase().onQuery("SELECT name FROM blacklist WHERE uuid = ?", entry);
                        String name = rsName.resultSet().next() ? rsName.resultSet().getString("name") : "Unknown";

                        listMessage.append(Keklist.getTranslations().get("blacklist.list.entry.player", date, entry, name, byPlayer)).append("\n");
                    }
                }

            } while (rs.resultSet().next());

            listMessage.append("\n").append(Keklist.getTranslations().get("blacklist.list.footer", Math.max(page - 1, 0), page, page + 1));

            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(listMessage.toString()));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}