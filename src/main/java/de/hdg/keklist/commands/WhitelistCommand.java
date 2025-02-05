package de.hdg.keklist.commands;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.api.events.whitelist.*;
import de.hdg.keklist.util.LanguageUtil;
import de.hdg.keklist.extentions.WebhookManager;
import de.hdg.keklist.util.TypeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static de.hdg.keklist.util.TypeUtil.getEntryType;

public class WhitelistCommand extends Command {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();
    private static final TypeToken<Map<String, String>> token = new TypeToken<>() {
    };

    public WhitelistCommand() {
        super("whitelist");
        setDescription(Keklist.getTranslations().get("whitelist.description"));
        setAliases(List.of("wl"));
        setUsage(Keklist.getTranslations().get("whitelist.usage"));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("invalid-syntax")));
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.usage.command")));
            return true;
        }

        try {
            String senderName = sender.getName();

            TypeUtil.EntryType type = getEntryType(args[1]);

            if (type.equals(TypeUtil.EntryType.UNKNOWN)) {
                if (args[0].equalsIgnoreCase("list")) { // Not the best way to handle this, but it works
                    try {
                        handleList(sender, Integer.parseInt(args[1]));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("invalid-syntax")));
                    }

                    return true;
                } else {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.invalid-argument")));
                    return false;
                }
            }


            switch (args[0]) {
                case "add" -> {
                    if (!sender.hasPermission("keklist.whitelist.add")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return false;
                    }

                    int level = 0;

                    if (args.length >= 3) {
                        try {
                            level = Integer.parseInt(args[2]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.level.invalid", args[2])));
                            return false;
                        }
                    }

                    switch (type) {
                        case JAVA -> {
                            Request request = new Request.Builder().url("https://api.mojang.com/users/profiles/minecraft/" + args[1]).build();
                            client.newCall(request).enqueue(new WhitelistCommand.UserWhitelistAddCallback(sender, type, level));
                        }

                        case IPv4, IPv6 -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp WHERE ip = ?", args[1]);

                            if (!rs.next()) {
                                new IpAddToWhitelistEvent(args[1]).callEvent();
                                Keklist.getDatabase().onUpdate("INSERT INTO whitelistIp (ip, byPlayer, unix) VALUES (?, ?, ?)", args[1], senderName, System.currentTimeMillis());
                                Keklist.getDatabase().onUpdate("INSERT INTO whitelistLevel (entry, whitelistLevel, byPlayer) VALUES (?, ?, ?)", args[1], level, System.currentTimeMillis());
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.added", args[1])));

                                if (Keklist.getWebhookManager() != null)
                                    Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_ADD, args[1], senderName, System.currentTimeMillis());

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.add", args[1], senderName)), "keklist.notify.whitelist");
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.already-whitelisted", args[1])));

                        }

                        case BEDROCK -> {
                            FloodgateApi api = Keklist.getInstance().getFloodgateApi();

                            try {
                                UUID bedrockUUID = api.getUuidFor(args[1].replace(Keklist.getInstance().getConfig().getString("floodgate.prefix"), "")).get();
                                whitelistUser(sender, bedrockUUID, args[1], level);
                            } catch (Exception ex) {

                                if (Keklist.getInstance().getConfig().getString("floodgate.api-key") != null) {
                                    Request request = new Request.Builder()
                                            .url("https://mcprofile.io/api/v1/bedrock/gamertag/" + args[1].replace(".", ""))
                                            .header("x-api-key", Keklist.getInstance().getConfig().getString("floodgate.api-key"))
                                            .build();

                                    client.newCall(request).enqueue(new WhitelistCommand.UserWhitelistAddCallback(sender, type, level));
                                    return false;
                                } else
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("floodgate.api-key-not-set")));

                            }
                        }

                        case DOMAIN -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistDomain WHERE domain = ?", args[1]);

                            if (!rs.next()) {
                                try {
                                    InetAddress address = InetAddress.getByName(args[1]);

                                    new DomainAddToWhitelistEvent(args[1]).callEvent();
                                    Keklist.getDatabase().onUpdate("INSERT INTO whitelistDomain (domain, byPlayer, unix) VALUES (?, ?, ?)", args[1], senderName, System.currentTimeMillis());
                                    Keklist.getDatabase().onUpdate("INSERT INTO whitelistLevel (entry, whitelistLevel, byPlayer) VALUES (?, ?, ?)", args[1], level, System.currentTimeMillis());
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.domain-added", args[1], address.getHostAddress())));

                                    if (Keklist.getWebhookManager() != null)
                                        Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_ADD, args[1], senderName, System.currentTimeMillis());

                                    if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                        Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.add", args[1], senderName)), "keklist.notify.whitelist");


                                } catch (UnknownHostException e) {
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.invalid-domain", args[1])));
                                    return true;
                                }
                            }
                        }
                    }

                    return true;
                }

                case "remove" -> {
                    if (!sender.hasPermission("keklist.whitelist.remove")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return false;
                    }

                    Keklist.getDatabase().onUpdate("DELETE FROM whitelistLevel WHERE entry = ? ", args[1]);

                    switch (type) {
                        case JAVA, BEDROCK -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE name = ?", args[1]);
                            if (rs.next()) {
                                new PlayerRemovedFromWhitelistEvent(args[1]).callEvent();
                                Keklist.getDatabase().onUpdate("DELETE FROM whitelist WHERE name = ?", args[1]);
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.removed", args[1])));

                                if (Keklist.getWebhookManager() != null)
                                    Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_REMOVE, args[1], senderName, System.currentTimeMillis());

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.remove", args[1], senderName)), "keklist.notify.whitelist");

                            } else {
                                ResultSet rsUserFix = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE name = ?", args[1] + " (Old Name)");
                                if (rsUserFix.next()) {
                                    Keklist.getDatabase().onUpdate("DELETE FROM whitelist WHERE name = ?", args[1] + " (Old Name)");
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.removed", args[1] + " (Old Name)")));

                                    if (Keklist.getWebhookManager() != null)
                                        Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_REMOVE, args[1], senderName, System.currentTimeMillis());

                                    if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                        Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.remove", args[1] + "(Old Name)", senderName)), "keklist.notify.whitelist");

                                } else {
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", args[1])));
                                }
                            }
                        }

                        case IPv4, IPv6 -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp WHERE ip = ?", args[1]);
                            if (rs.next()) {
                                new IpRemovedFromWhitelistEvent(args[1]).callEvent();
                                Keklist.getDatabase().onUpdate("DELETE FROM whitelistIp WHERE ip = ?", args[1]);
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.removed", args[1])));

                                if (Keklist.getWebhookManager() != null)
                                    Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_REMOVE, args[1], senderName, System.currentTimeMillis());

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.remove", args[1], senderName)), "keklist.notify.whitelist");

                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", args[1])));
                            }
                        }

                        case DOMAIN -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistDomain WHERE domain = ?", args[1]);
                            if (rs.next()) {
                                new DomainRemovedFromWhitelistEvent(args[1]).callEvent();
                                Keklist.getDatabase().onUpdate("DELETE FROM whitelistDomain WHERE domain = ?", args[1]);
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.removed", args[1])));

                                if (Keklist.getWebhookManager() != null)
                                    Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_REMOVE, args[1], senderName, System.currentTimeMillis());

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.remove", args[1], senderName)), "keklist.notify.whitelist");

                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", args[1])));
                            }
                        }
                    }

                    return true;
                }

                case "info" -> {
                    if (!sender.hasPermission("keklist.whitelist.info")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return false;
                    }

                    switch (type) {
                        case IPv4, IPv6 -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp WHERE ip = ?", args[1]);

                            if (rs.next()) {
                                sendInfo(rs, sender, args[1]);
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", args[1])));
                        }

                        case JAVA, BEDROCK -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE name = ?", args[1]);

                            if (rs.next()) {
                                sendInfo(rs, sender, args[1]);
                            } else {
                                ResultSet rsUserFix = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE name = ?", args[1] + " (Old Name)");
                                if (rsUserFix.next()) {
                                    sendInfo(rsUserFix, sender, args[1] + " (Old Name)");
                                } else
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", args[1])));
                            }
                        }

                        case DOMAIN -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistDomain WHERE domain = ?", args[1]);

                            if (rs.next()) {
                                sendInfo(rs, sender, args[1]);
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", args[1])));
                        }
                    }
                }

                case "level" -> {
                    if (!sender.hasPermission("keklist.whitelist.level")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return false;
                    }

                    if (args.length >= 3) {
                        try {
                            int level = Integer.parseInt(args[2]);

                            String entry = args[1];

                            switch (type) {
                                case JAVA -> {
                                    entry = Objects.requireNonNull(Bukkit.getPlayerUniqueId(args[1])).toString();

                                    ResultSet isWhitelistedRs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", entry);

                                    if (!isWhitelistedRs.next()) {
                                        throw new NullPointerException("User not found on whitelist");
                                    }
                                }


                                case BEDROCK -> {
                                    assert Keklist.getInstance().getFloodgateApi() != null;
                                    ResultSet isWhitelistedRs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", Keklist.getInstance().getFloodgateApi().getUuidFor(entry).join()); // Please don't judge

                                    if (!isWhitelistedRs.next()) {
                                        throw new NullPointerException("User not found on whitelist");
                                    }
                                }

                                case DOMAIN -> {
                                    ResultSet isWhitelistedRs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistDomain WHERE domain = ?", entry);

                                    if (!isWhitelistedRs.next()) {
                                        throw new NullPointerException("Domain not found on whitelist");
                                    }
                                }

                                case IPv4, IPv6 -> {
                                    ResultSet isWhitelistedRs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp WHERE ip = ?", entry);

                                    if (!isWhitelistedRs.next()) {
                                        throw new NullPointerException("IP not found on whitelist");
                                    }
                                }

                                case UUID -> {
                                    ResultSet isWhitelistedRs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", entry);

                                    if (!isWhitelistedRs.next()) {
                                        throw new NullPointerException("IP not found on whitelist");
                                    }
                                }
                            }

                            try (ResultSet rs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelistLevel WHERE entry = ?", entry)) {
                                if (rs.next()) {
                                    Keklist.getDatabase().onUpdate("UPDATE whitelistLevel SET whitelistLevel = ? WHERE entry = ?", level, entry);
                                } else {
                                    Keklist.getDatabase().onUpdate("INSERT INTO whitelistLevel VALUES (?, ?, ?)", entry, level, senderName);
                                }

                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.level.update", level, args[1])));
                            }

                        } catch (NumberFormatException e) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.level.invalid", args[2])));
                            return false;
                        } catch (NullPointerException notFound) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.not-whitelisted", args[1])));
                            return false;
                        }

                    } else {
                        try (ResultSet rs = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelistLevel WHERE entry = ?", args[1])) {
                            if (rs.next()) {
                                execute(sender, commandLabel, new String[]{"info", args[1]});
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.level.not-set", args[1])));

                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }

                default -> {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("invalid-syntax")));
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.usage.command")));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void sendInfo(@NotNull ResultSet resultSet, @NotNull CommandSender sender, @NotNull String entry) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

            String byPlayer = resultSet.getString("byPlayer");
            String unix = sdf.format(resultSet.getLong("unix"));

            LanguageUtil translations = Keklist.getTranslations();
            MiniMessage miniMessage = Keklist.getInstance().getMiniMessage();

            int level = 0;
            try (ResultSet rs = Keklist.getDatabase().onQuery("SELECT whitelistLevel FROM whitelistLevel WHERE entry = ?", entry)) {
                if (rs.next()) {
                    level = rs.getInt("whitelistLevel");
                }
            }

            sender.sendMessage(miniMessage.deserialize(translations.get("whitelist.info", entry, level, byPlayer, unix)));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void whitelistUser(@NotNull CommandSender from, @NotNull UUID uuid, @NotNull String playerName,
                               int level) {
        try {
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", uuid.toString());
            ResultSet rsUserFix = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE name = ?", playerName);

            if (!rs.next()) {
                if (rsUserFix.next()) {
                    Keklist.getDatabase().onUpdate("UPDATE whitelist SET name = ? WHERE name = ?", playerName + " (Old Name)", playerName);
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
        try (ResultSet rs =
                     Keklist.getDatabase().onQuery("SELECT * FROM (SELECT uuid, byPlayer, unix FROM whitelist UNION ALL SELECT * FROM whitelistIp UNION ALL SELECT * FROM whitelistDomain) as `entries` LIMIT ?,8", (page - 1) * 8)) {

            if (!rs.next() || page < 1) {
                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.list.empty")));
                return;
            }

            StringBuilder listMessage = new StringBuilder();

            listMessage.append("\n").append(Keklist.getTranslations().get("whitelist.list.header")).append("\n\n");

            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

            do {
                String entry = rs.getString(1);
                String byPlayer = rs.getString(2);
                String date = sdf.format(new Date(rs.getLong(3)));

                int level = 0;
                try (ResultSet levelRs = Keklist.getDatabase().onQuery("SELECT whitelistLevel FROM whitelistLevel WHERE entry = ?", entry)) {
                    if (!levelRs.next()) {
                        level = levelRs.getInt("whitelistLevel");
                    }
                }

                TypeUtil.EntryType type = TypeUtil.getEntryType(entry);

                switch (type) {
                    case IPv4, IPv6, DOMAIN ->
                            listMessage.append(Keklist.getTranslations().get("whitelist.list.entry", date, level, entry, byPlayer)).append("\n");

                    case UUID -> {
                        ResultSet rsName = Keklist.getDatabase().onQuery("SELECT name FROM whitelist WHERE uuid = ?", entry);
                        String name = rsName.next() ? rsName.getString("name") : "Unknown";

                        listMessage.append(Keklist.getTranslations().get("whitelist.list.entry.player", date, level, entry, name, byPlayer)).append("\n");
                    }
                }

            } while (rs.next());

            listMessage.append("\n").append(Keklist.getTranslations().get("whitelist.list.footer", Math.max(page - 1, 0), page, page + 1));

            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(listMessage.toString()));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[]
            args) throws IllegalArgumentException {
        if (args.length < 2) {
            return List.of("add", "remove", "info", "list", "level");
        } else if (args.length == 2) {
            try {
                switch (args[0]) {
                    case "remove", "info" -> {
                        if (!sender.hasPermission("keklist.whitelist.remove")
                                || !sender.hasPermission("keklist.whitelist.info")) return Collections.emptyList();

                        List<String> list = new ArrayList<>();

                        ResultSet rsUser = Keklist.getDatabase().onQuery("SELECT name FROM whitelist");
                        while (rsUser.next()) {
                            list.add(rsUser.getString("name"));
                        }

                        ResultSet rsIp = Keklist.getDatabase().onQuery("SELECT ip FROM whitelistIp");
                        while (rsIp.next()) {
                            list.add(rsIp.getString("ip"));
                        }

                        ResultSet rsDomain = Keklist.getDatabase().onQuery("SELECT domain FROM whitelistDomain");
                        while (rsDomain.next()) {
                            list.add(rsDomain.getString("domain"));
                        }

                        return list;
                    }

                    case "add" -> {
                        if (!sender.hasPermission("keklist.whitelist.add")) return Collections.emptyList();

                        List<String> completions = new ArrayList<>();

                        Bukkit.getOnlinePlayers().forEach(player -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", player.getUniqueId().toString());
                            try {
                                if (!rs.next())
                                    completions.add(player.getName());
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        });

                        Bukkit.getOnlinePlayers().forEach(player -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp WHERE ip = ?", player.getAddress().getAddress().getHostAddress());
                            try {
                                if (!rs.next())
                                    completions.add(player.getAddress().getAddress().getHostAddress() + "(" + player.getName() + ")");

                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        });

                        return completions;
                    }

                    case "level" -> {
                        if (!sender.hasPermission("keklist.whitelist.level")) return Collections.emptyList();

                        List<String> completions = new ArrayList<>();

                        ResultSet rsUser = Keklist.getDatabase().onQuery("SELECT entry FROM whitelistLevel");
                        while (rsUser.next()) {
                            completions.add(rsUser.getString("entry"));
                        }

                        completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());

                        return completions;
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return Collections.emptyList();
    }
}
