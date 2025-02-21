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
import de.hdg.keklist.api.events.whitelist.*;
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
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static de.hdg.keklist.util.TypeUtil.getEntryType;

public class WhitelistCommand implements BrigadierCommand {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().setStrictness(Strictness.LENIENT).create();
    private static final TypeToken<Map<String, String>> token = new TypeToken<>() {
    };

    @Override
    @CommandData(
            name = "whitelist",
            descriptionKey = "whitelist.description",
            aliases = {"wl"}
    )
    public @NotNull LiteralCommandNode<CommandSourceStack> getCommand() {
        LiteralArgumentBuilder<CommandSourceStack> addSubcommand = Commands.literal("add")
                .then(Commands.argument("entry", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(player -> {
                                try (DB.QueryResult rsUser = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelist WHERE name = ?", player.getName());
                                     DB.QueryResult rsIp = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelistIp WHERE ip = ?", player.getAddress().getAddress().getHostAddress())
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
                        .then(Commands.argument("level", IntegerArgumentType.integer())
                                .executes(this::execute)
                        )
                ).requires(sender -> sender.getSender().hasPermission("keklist.whitelist.add"));


        LiteralArgumentBuilder<CommandSourceStack> removeSubcommand = Commands.literal("remove")
                .then(Commands.argument("entry", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM (SELECT name AS entry FROM whitelist UNION ALL SELECT ip AS entry FROM whitelistIp UNION ALL SELECT domain AS entry FROM whitelistDomain) as whitelistEntries")
                            ) {
                                List<String> whitelistEntries = new ArrayList<>();

                                while (rs.resultSet().next()) {
                                    whitelistEntries.add(rs.resultSet().getString(1));
                                }

                                whitelistEntries.stream()
                                        .filter(entry -> entry.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                        .forEach(builder::suggest);
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }

                            return builder.buildFuture();
                        })
                        .executes(this::execute)
                ).requires(sender -> sender.getSender().hasPermission("keklist.whitelist.remove"));

        LiteralArgumentBuilder<CommandSourceStack> infoSubcommand = Commands.literal("info")
                .then(Commands.argument("entry", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM (SELECT name AS entry FROM whitelist UNION ALL SELECT ip AS entry FROM whitelistIp UNION ALL SELECT domain AS entry FROM whitelistDomain) as whitelistEntries")
                            ) {
                                List<String> whitelistEntries = new ArrayList<>();

                                while (rs.resultSet().next()) {
                                    whitelistEntries.add(rs.resultSet().getString(1));
                                }

                                whitelistEntries.stream()
                                        .filter(entry -> entry.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                        .forEach(builder::suggest);
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }

                            return builder.buildFuture();
                        })
                        .executes(this::execute)
                ).requires(sender -> sender.getSender().hasPermission("keklist.whitelist.info"));

        LiteralArgumentBuilder<CommandSourceStack> listSubcommand = Commands.literal("list")
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(this::execute)
                ).executes(this::execute).requires(sender -> sender.getSender().hasPermission("keklist.whitelist.list"));

        LiteralArgumentBuilder<CommandSourceStack> levelSubcommand = Commands.literal("level")
                .then(Commands.argument("entry", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            List<String> levelEntries = new ArrayList<>();

                            try (DB.QueryResult entriesRs = Keklist.getDatabase().onQuery("SELECT entry FROM whitelistLevel")) {
                                while (entriesRs.resultSet().next()) {
                                    levelEntries.add(entriesRs.resultSet().getString(1));
                                }
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }

                            levelEntries.stream()
                                    .filter(entry -> entry.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                    .forEach(builder::suggest);

                            return builder.buildFuture();
                        }).executes(this::execute)
                        .then(Commands.argument("level", IntegerArgumentType.integer()).executes(this::execute))
                ).requires(sender -> sender.getSender().hasPermission("keklist.whitelist.level"));


        return Commands.literal("whitelist")
                .then(addSubcommand)
                .then(removeSubcommand)
                .then(infoSubcommand)
                .then(listSubcommand)
                .then(levelSubcommand)
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
                    if (!sender.hasPermission("keklist.whitelist.add")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return Command.SINGLE_SUCCESS;
                    }

                    String entry = ctx.getArgument("entry", String.class);
                    TypeUtil.EntryType type = getEntryType(entry);

                    int level = 0;

                    if (ctx.getLastChild().getNodes().getLast().getNode().getName().equals("level")) {
                        level = ctx.getArgument("level", Integer.class);
                    }

                    switch (type) {
                        case JAVA -> {
                            Request request = new Request.Builder().url("https://api.mojang.com/users/profiles/minecraft/" + entry).build();
                            client.newCall(request).enqueue(new WhitelistCommand.UserWhitelistAddCallback(sender, type, level));
                        }

                        case IPv4, IPv6 -> {
                            @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelistIp WHERE ip = ?", entry);

                            if (!rs.resultSet().next()) {
                                new IpAddToWhitelistEvent(entry).callEvent();
                                Keklist.getDatabase().onUpdate("INSERT INTO whitelistIp (ip, byPlayer, unix) VALUES (?, ?, ?)", entry, senderName, System.currentTimeMillis());
                                Keklist.getDatabase().onUpdate("INSERT INTO whitelistLevel (entry, whitelistLevel, byPlayer) VALUES (?, ?, ?)", entry, level, System.currentTimeMillis());
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.added", entry)));

                                if (Keklist.getWebhookManager() != null)
                                    Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_ADD, entry, senderName, System.currentTimeMillis());

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.add", entry, senderName)), "keklist.notify.whitelist");
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.already-whitelisted", entry)));

                        }

                        case BEDROCK -> {
                            FloodgateApi api = Keklist.getInstance().getFloodgateApi();

                            try {
                                UUID bedrockUUID = api.getUuidFor(entry.replace(Keklist.getInstance().getConfig().getString("floodgate.prefix"), "")).get();
                                whitelistUser(sender, bedrockUUID, entry, level);
                            } catch (Exception ex) {

                                if (Keklist.getInstance().getConfig().getString("floodgate.api-key") != null) {
                                    Request request = new Request.Builder()
                                            .url("https://mcprofile.io/api/v1/bedrock/gamertag/" + entry.replace(".", ""))
                                            .header("x-api-key", Keklist.getInstance().getConfig().getString("floodgate.api-key"))
                                            .build();

                                    client.newCall(request).enqueue(new WhitelistCommand.UserWhitelistAddCallback(sender, type, level));
                                    return Command.SINGLE_SUCCESS;
                                } else
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("floodgate.api-key-not-set")));

                            }
                        }

                        case DOMAIN -> {
                            @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelistDomain WHERE domain = ?", entry);

                            if (!rs.resultSet().next()) {
                                try {
                                    InetAddress address = InetAddress.getByName(entry);

                                    new DomainAddToWhitelistEvent(entry).callEvent();
                                    Keklist.getDatabase().onUpdate("INSERT INTO whitelistDomain (domain, byPlayer, unix) VALUES (?, ?, ?)", entry, senderName, System.currentTimeMillis());
                                    Keklist.getDatabase().onUpdate("INSERT INTO whitelistLevel (entry, whitelistLevel, byPlayer) VALUES (?, ?, ?)", entry, level, System.currentTimeMillis());
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.domain-added", entry, address.getHostAddress())));

                                    if (Keklist.getWebhookManager() != null)
                                        Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_ADD, entry, senderName, System.currentTimeMillis());

                                    if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                        Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.add", entry, senderName)), "keklist.notify.whitelist");


                                } catch (UnknownHostException e) {
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.invalid-domain", entry)));
                                    return Command.SINGLE_SUCCESS;
                                }
                            }
                        }
                    }

                    return Command.SINGLE_SUCCESS;
                }

                case "remove" -> {
                    if (!sender.hasPermission("keklist.whitelist.remove")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return Command.SINGLE_SUCCESS;
                    }

                    String entry = ctx.getArgument("entry", String.class);
                    TypeUtil.EntryType type = getEntryType(entry);

                    Keklist.getDatabase().onUpdate("DELETE FROM whitelistLevel WHERE UPPER(entry) = UPPER(?) ", entry);

                    switch (type) {
                        case JAVA, BEDROCK -> {
                            @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelist WHERE UPPER(name) = UPPER(?)", entry);
                            if (rs.resultSet().next()) {
                                new PlayerRemovedFromWhitelistEvent(entry).callEvent();
                                Keklist.getDatabase().onUpdate("DELETE FROM whitelist WHERE UPPER(name) = UPPER(?)", entry);
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.removed", entry)));

                                if (Keklist.getWebhookManager() != null)
                                    Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_REMOVE, entry, senderName, System.currentTimeMillis());

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.remove", entry, senderName)), "keklist.notify.whitelist");

                            } else {
                                @Cleanup DB.QueryResult rsUserFix = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelist WHERE UPPER(name) = UPPER(?)", entry + " (Old Name)");
                                if (rsUserFix.resultSet().next()) {
                                    Keklist.getDatabase().onUpdate("DELETE FROM whitelist WHERE UPPER(name) = UPPER(?)", entry + " (Old Name)");
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.removed", entry + " (Old Name)")));

                                    if (Keklist.getWebhookManager() != null)
                                        Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_REMOVE, entry, senderName, System.currentTimeMillis());

                                    if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                        Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.remove", entry + "(Old Name)", senderName)), "keklist.notify.whitelist");

                                } else {
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", entry)));
                                }
                            }
                        }

                        case IPv4, IPv6 -> {
                            @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelistIp WHERE ip = ?", entry);
                            if (rs.resultSet().next()) {
                                new IpRemovedFromWhitelistEvent(entry).callEvent();
                                Keklist.getDatabase().onUpdate("DELETE FROM whitelistIp WHERE ip = ?", entry);
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.removed", entry)));

                                if (Keklist.getWebhookManager() != null)
                                    Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_REMOVE, entry, senderName, System.currentTimeMillis());

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.remove", entry, senderName)), "keklist.notify.whitelist");

                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", entry)));
                            }
                        }

                        case DOMAIN -> {
                            @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelistDomain WHERE UPPER(domain) = UPPER(?)", entry);
                            if (rs.resultSet().next()) {
                                new DomainRemovedFromWhitelistEvent(entry).callEvent();
                                Keklist.getDatabase().onUpdate("DELETE FROM whitelistDomain WHERE UPPER(domain) = UPPER(?)", entry);
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.removed", entry)));

                                if (Keklist.getWebhookManager() != null)
                                    Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_REMOVE, entry, senderName, System.currentTimeMillis());

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.remove", entry, senderName)), "keklist.notify.whitelist");

                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", entry)));
                            }
                        }
                    }

                    return Command.SINGLE_SUCCESS;
                }

                case "info" -> {
                    if (!sender.hasPermission("keklist.whitelist.info")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return Command.SINGLE_SUCCESS;
                    }

                    String entry = ctx.getArgument("entry", String.class);
                    TypeUtil.EntryType type = getEntryType(entry);

                    switch (type) {
                        case IPv4, IPv6 -> {
                            @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp WHERE ip = ?", entry);

                            if (rs.resultSet().next()) {
                                sendInfo(rs, sender, entry);
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", entry)));
                        }

                        case JAVA, BEDROCK -> {
                            @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE UPPER(name) = UPPER(?)", entry);

                            if (rs.resultSet().next()) {
                                sendInfo(rs, sender, entry);
                            } else {
                                @Cleanup DB.QueryResult rsUserFix = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE UPPER(name) = UPPER(?)", entry + " (Old Name)");
                                if (rsUserFix.resultSet().next()) {
                                    sendInfo(rsUserFix, sender, entry + " (Old Name)");
                                } else
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", entry)));
                            }
                        }

                        case DOMAIN -> {
                            @Cleanup DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistDomain WHERE UPPER(domain) = UPPER(?)", entry);

                            if (rs.resultSet().next()) {
                                sendInfo(rs, sender, entry);
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", entry)));
                        }
                    }
                }

                case "level" -> {
                    if (!sender.hasPermission("keklist.whitelist.level")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return Command.SINGLE_SUCCESS;
                    }

                    String entry = ctx.getArgument("entry", String.class);
                    TypeUtil.EntryType type = getEntryType(entry);

                    if (ctx.getLastChild().getNodes().getLast().getNode().getName().equals("level")) {
                        try {
                            int level = ctx.getArgument("level", Integer.class);

                            switch (type) {
                                case JAVA -> {
                                    entry = Objects.requireNonNull(Bukkit.getPlayerUniqueId(entry)).toString();

                                    @Cleanup DB.QueryResult isWhitelistedRs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelist WHERE uuid = ?", entry);

                                    if (!isWhitelistedRs.resultSet().next()) {
                                        throw new NullPointerException("User not found on whitelist");
                                    }
                                }


                                case BEDROCK -> {
                                    assert Keklist.getInstance().getFloodgateApi() != null;
                                    @Cleanup DB.QueryResult isWhitelistedRs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelist WHERE uuid = ?", Keklist.getInstance().getFloodgateApi().getUuidFor(entry).join()); // Please don't judge

                                    if (!isWhitelistedRs.resultSet().next()) {
                                        throw new NullPointerException("User not found on whitelist");
                                    }
                                }

                                case DOMAIN -> {
                                    @Cleanup DB.QueryResult isWhitelistedRs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelistDomain WHERE UPPER(domain) = UPPER(?)", entry);

                                    if (!isWhitelistedRs.resultSet().next()) {
                                        throw new NullPointerException("Domain not found on whitelist");
                                    }
                                }

                                case IPv4, IPv6 -> {
                                    @Cleanup DB.QueryResult isWhitelistedRs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelistIp WHERE ip = ?", entry);

                                    if (!isWhitelistedRs.resultSet().next()) {
                                        throw new NullPointerException("IP not found on whitelist");
                                    }
                                }

                                case UUID -> {
                                    @Cleanup DB.QueryResult isWhitelistedRs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelist WHERE uuid = ?", entry);

                                    if (!isWhitelistedRs.resultSet().next()) {
                                        throw new NullPointerException("IP not found on whitelist");
                                    }
                                }
                            }

                            try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelistLevel WHERE UPPER(entry) = UPPER(?)", entry)) {
                                if (rs.resultSet().next()) {
                                    Keklist.getDatabase().onUpdate("UPDATE whitelistLevel SET whitelistLevel = ? WHERE UPPER(entry) = UPPER(?)", level, entry);
                                } else {
                                    Keklist.getDatabase().onUpdate("INSERT INTO whitelistLevel VALUES (?, ?, ?)", entry, level, senderName);
                                }

                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.level.update", level, ctx.getArgument("entry", String.class))));
                            }

                        } catch (NullPointerException notFound) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", ctx.getArgument("entry", String.class))));
                            return Command.SINGLE_SUCCESS;
                        }

                    } else {
                        try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelistLevel WHERE UPPER(entry) = UPPER(?)", entry)) {
                            if (rs.resultSet().next()) {
                                Keklist.getInstance().getServer().dispatchCommand(sender, "whitelist info " + entry);
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.level.not-set", entry)));

                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }

                case "list" -> handleList(sender, ctx.getArgument("page", Integer.class));

                default -> {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("invalid-syntax")));
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.usage.command")));
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
            String unix = sdf.format(resultSet.resultSet().getLong("unix"));

            LanguageUtil translations = Keklist.getTranslations();
            MiniMessage miniMessage = Keklist.getInstance().getMiniMessage();

            int level = 0;
            try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT whitelistLevel FROM whitelistLevel WHERE UPPER(entry) = UPPER(?)", entry)) {
                if (rs.resultSet().next()) {
                    level = rs.resultSet().getInt("whitelistLevel");
                }
            }

            sender.sendMessage(miniMessage.deserialize(translations.get("whitelist.info", entry, level, byPlayer, unix)));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void whitelistUser(@NotNull CommandSender from, @NotNull UUID uuid, @NotNull String playerName, int level) {
        try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelist WHERE uuid = ?", uuid.toString());
             DB.QueryResult rsUserFix = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelist WHERE UPPER(name) = UPPER(?)", playerName)
        ) {

            if (!rs.resultSet().next()) {
                if (rsUserFix.resultSet().next()) {
                    Keklist.getDatabase().onUpdate("UPDATE whitelist SET name = ? WHERE UPPER(name) = UPPER(?)", playerName + " (Old Name)", playerName);
                }

                Bukkit.getScheduler().runTask(Keklist.getInstance(), () -> new UUIDAddToWhitelistEvent(uuid).callEvent());
                Keklist.getDatabase().onUpdate("INSERT INTO whitelist (uuid, name, byPlayer, unix) VALUES (?, ?, ?, ?)", uuid.toString(), playerName, from.getName(), System.currentTimeMillis());
                Keklist.getDatabase().onUpdate("INSERT INTO whitelistLevel (entry, whitelistLevel, byPlayer) VALUES (?, ?, ?)", uuid.toString(), level, System.currentTimeMillis());
                from.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.added", playerName)));

                if (Keklist.getWebhookManager() != null)
                    Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_ADD, playerName, from.getName(), System.currentTimeMillis());

                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.add", playerName, from.getName())), "keklist.notify.whitelist");

            } else
                from.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.already-whitelisted", playerName)));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private class UserWhitelistAddCallback implements Callback {
        private final CommandSender sender;
        private final TypeUtil.EntryType type;
        private final int level;

        public UserWhitelistAddCallback(@NotNull CommandSender sender, TypeUtil.EntryType type, int level) {
            this.sender = sender;
            this.type = type;
            this.level = level;
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
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

                whitelistUser(sender, UUID.fromString(uuid.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5")), name, level);
            }
        }

        @Override
        public void onFailure(@NotNull Call call, IOException e) {
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("http.error")));
            sender.sendMessage(Component.text(Keklist.getTranslations().get("http.detail", e.getMessage())));
        }
    }

    @Nullable
    private static Component checkForGoodResponse(@NotNull String response, @NotNull TypeUtil.EntryType type) {
        JsonElement responseElement = JsonParser.parseString(response);

        if (!responseElement.isJsonNull()) {
            if (type.equals(TypeUtil.EntryType.JAVA)) {
                if (responseElement.getAsJsonObject().get("error") != null ||
                        !responseElement.getAsJsonObject().has("id") ||
                        !responseElement.getAsJsonObject().has("name")) {
                    return Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("http.not-found", responseElement.getAsJsonObject().get("error").getAsString()));
                }
            } else {
                if (responseElement.getAsJsonObject().get("message") != null)
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("http.not-found", responseElement.getAsJsonObject().get("message").getAsString()));

            }

        } else {
            return Component.text(Keklist.getTranslations().get("http.null-response"));
        }

        return null;
    }

    /**
     * Handles the list command
     *
     * @param sender the sender who executed the command
     * @param page   the page to display
     */
    private void handleList(@NotNull CommandSender sender, int page) {
        try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM (SELECT uuid, byPlayer, unix FROM whitelist UNION ALL SELECT * FROM whitelistIp UNION ALL SELECT * FROM whitelistDomain) as `entries` LIMIT ?,8", (page - 1) * 8)) {

            if (!rs.resultSet().next() || page < 1) {
                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.list.empty")));
                return;
            }

            StringBuilder listMessage = new StringBuilder();

            listMessage.append("\n").append(Keklist.getTranslations().get("whitelist.list.header")).append("\n\n");

            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

            do {
                String entry = rs.resultSet().getString(1);
                String byPlayer = rs.resultSet().getString(2);
                String date = sdf.format(new Date(rs.resultSet().getLong(3)));

                int level = 0;
                try (DB.QueryResult levelRs = Keklist.getDatabase().onQuery("SELECT whitelistLevel FROM whitelistLevel WHERE entry = ?", entry)) {
                    if (!levelRs.resultSet().next()) {
                        level = levelRs.resultSet().getInt("whitelistLevel");
                    }
                }

                TypeUtil.EntryType type = TypeUtil.getEntryType(entry);

                switch (type) {
                    case IPv4, IPv6, DOMAIN ->
                            listMessage.append(Keklist.getTranslations().get("whitelist.list.entry", date, level, entry, byPlayer)).append("\n");

                    case UUID -> {
                        @Cleanup DB.QueryResult rsName = Keklist.getDatabase().onQuery("SELECT name FROM whitelist WHERE uuid = ?", entry);
                        String name = rsName.resultSet().next() ? rsName.resultSet().getString("name") : "Unknown";

                        listMessage.append(Keklist.getTranslations().get("whitelist.list.entry.player", date, level, entry, name, byPlayer)).append("\n");
                    }
                }

            } while (rs.resultSet().next());

            listMessage.append("\n").append(Keklist.getTranslations().get("whitelist.list.footer", Math.max(page - 1, 0), page, page + 1));

            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(listMessage.toString()));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
