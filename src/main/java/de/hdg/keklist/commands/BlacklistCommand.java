package de.hdg.keklist.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.api.events.blacklist.*;
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

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static de.hdg.keklist.util.TypeUtil.getEntryType;

public class BlacklistCommand extends Command {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();
    private static final TypeToken<Map<String, String>> token = new TypeToken<>() {
    };

    public BlacklistCommand() {
        super("blacklist");
        setAliases(List.of("bl"));
        setUsage(Keklist.getTranslations().get("blacklist.usage"));
        setDescription(Keklist.getTranslations().get("blacklist.description"));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("invalid-syntax")));
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.usage.command")));
            return false;
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

            String reason = null;
            if (args.length > 2) {
                reason = String.join(" ", args).substring(args[0].length() + args[1].length() + 2);
            }

            switch (args[0]) {
                case "add" -> {
                    if (!sender.hasPermission("keklist.blacklist.add")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return false;
                    }

                    switch (type) {
                        case JAVA -> {
                            Request request = new Request.Builder().url("https://api.mojang.com/users/profiles/minecraft/" + args[1]).build();
                            client.newCall(request).enqueue(new UserBlacklistAddCallback((sender instanceof Player) ? sender : null, reason, type));
                        }

                        case IPv4, IPv6 -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", args[1]);

                            if (!rs.next()) {
                                if (reason == null) {
                                    new IpAddToBlacklistEvent(args[1], null).callEvent();
                                    Keklist.getDatabase().onUpdate("INSERT INTO blacklistIp (ip, byPlayer, unix) VALUES (?, ?, ?)", args[1], senderName, System.currentTimeMillis());

                                    if (Keklist.getWebhookManager() != null)
                                        Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_ADD, args[1], senderName, null, System.currentTimeMillis());

                                } else {
                                    if (reason.length() <= 1500) {
                                        new IpAddToBlacklistEvent(args[1], reason).callEvent();
                                        Keklist.getDatabase().onUpdate("INSERT INTO blacklistIp (ip, byPlayer, unix, reason) VALUES (?, ?, ?, ?)", args[1], senderName, System.currentTimeMillis(), reason);

                                        if (Keklist.getWebhookManager() != null)
                                            Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_ADD, args[1], senderName, reason, System.currentTimeMillis());

                                    } else {
                                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.reason-too-long")));
                                        return true;
                                    }
                                }

                                ResultSet rsMotd = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", args[1]);
                                if (!rsMotd.next()) {
                                    new IpAddToMOTDBlacklistEvent(args[1]).callEvent();
                                    Keklist.getDatabase().onUpdate("INSERT INTO blacklistMotd (ip, byPlayer, unix) VALUES (?, ?, ?)", args[1], senderName, System.currentTimeMillis());
                                }

                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.added", args[1])));

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.notify.add", args[1], senderName)), "keklist.notify.blacklist");

                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-blacklisted", args[1])));
                                return true;
                            }
                        }

                        case BEDROCK -> {
                            FloodgateApi api = Keklist.getInstance().getFloodgateApi();

                            try {
                                UUID bedrockUUID = api.getUuidFor(args[1].replace(Keklist.getInstance().getConfig().getString("floodgate.prefix"), "")).get();
                                blacklistUser(sender, bedrockUUID, args[1], reason);
                            } catch (Exception ex) {

                                if (Keklist.getInstance().getConfig().getString("floodgate.api-key") != null) {
                                    Request request = new Request.Builder()
                                            .url("https://mcprofile.io/api/v1/bedrock/gamertag/" + args[1].replace(".", ""))
                                            .header("x-api-key", Keklist.getInstance().getConfig().getString("floodgate.api-key"))
                                            .build();

                                    client.newCall(request).enqueue(new BlacklistCommand.UserBlacklistAddCallback(sender, reason, type));
                                    return false;
                                } else
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("floodgate.api-key-not-set")));

                            }
                        }
                    }
                }

                case "remove" -> {
                    if (!sender.hasPermission("keklist.blacklist.remove")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return false;
                    }

                    switch (type) {
                        case TypeUtil.EntryType.JAVA, TypeUtil.EntryType.BEDROCK -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE name = ?", args[1]);
                            if (rs.next()) {
                                new PlayerRemovedFromBlacklist(args[1]).callEvent();
                                Keklist.getDatabase().onUpdate("DELETE FROM blacklist WHERE name = ?", args[1]);

                                if (Keklist.getWebhookManager() != null)
                                    Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_REMOVE, args[1], senderName, null, System.currentTimeMillis());

                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.removed", args[1])));

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.notify.remove", args[1], senderName)), "keklist.notify.blacklist");

                            } else {
                                ResultSet rsUserFix = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE name = ?", args[1] + " (Old Name)");
                                if (rsUserFix.next()) {
                                    new PlayerRemovedFromBlacklist(args[1]).callEvent();
                                    Keklist.getDatabase().onUpdate("DELETE FROM blacklist WHERE name = ?", args[1] + " (Old Name)");

                                    if (Keklist.getWebhookManager() != null)
                                        Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_REMOVE, args[1], senderName, null, System.currentTimeMillis());

                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.removed", args[1]) + " (Old Name)"));

                                    if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                        Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.notify.remove", args[1], senderName + "(Old Name)")), "keklist.notify.blacklist");

                                } else {
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.not-blacklisted", args[1])));
                                }
                            }
                        }

                        case TypeUtil.EntryType.IPv4, TypeUtil.EntryType.IPv6 -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", args[1]);
                            ResultSet rsMotd = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", args[1]);
                            if (rs.next() || rsMotd.next()) {
                                new IpRemovedFromBlacklistEvent(args[1]).callEvent();
                                new IpRemovedFromMOTDBlacklistEvent(args[1]).callEvent();

                                Keklist.getDatabase().onUpdate("DELETE FROM blacklistIp WHERE ip = ?", args[1]);
                                Keklist.getDatabase().onUpdate("DELETE FROM blacklistMotd WHERE ip = ?", args[1]);

                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.ip.removed", args[1])));

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.notify.remove", args[1], senderName)), "keklist.notify.blacklist");

                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.not-blacklisted", args[1])));
                                return true;
                            }
                        }
                    }
                }

                case "motd" -> {
                    if (!sender.hasPermission("keklist.blacklist.motd")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return false;
                    }

                    if (type.equals(TypeUtil.EntryType.IPv4)) {
                        ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", args[1]);
                        if (rs.next()) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.motd.already-blacklisted", args[1])));
                        } else {
                            new IpAddToMOTDBlacklistEvent(args[1]).callEvent();
                            Keklist.getDatabase().onUpdate("INSERT INTO blacklistMotd (ip, byPlayer, unix) VALUES (?, ?, ?)", args[1], senderName, System.currentTimeMillis());
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.motd.added", args[1])));

                            if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.notify.motd", args[1], senderName)), "keklist.notify.blacklist");
                        }
                    } else
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.motd.syntax")));
                }

                case "info" -> {
                    if (!sender.hasPermission("keklist.blacklist.info")) {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                        return false;
                    }

                    switch (type) {
                        case TypeUtil.EntryType.IPv4, TypeUtil.EntryType.IPv6 -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", args[1]);
                            ResultSet rsMotd = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", args[1]);

                            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));
                            LanguageUtil translations = Keklist.getTranslations();
                            MiniMessage miniMessage = Keklist.getInstance().getMiniMessage();

                            if (rs.next()) {
                                sendInfo(rs, sender, args[1]);
                            } else if (rsMotd.next()) {
                                String byPlayer = rsMotd.getString("byPlayer");
                                String unix = sdf.format(rsMotd.getLong("unix"));

                                sender.sendMessage(miniMessage.deserialize(translations.get("blacklist.info")));
                                sender.sendMessage(miniMessage.deserialize(translations.get("blacklist.info.entry", args[1])));
                                sender.sendMessage(miniMessage.deserialize(translations.get("blacklist.info.by", byPlayer)));
                                sender.sendMessage(miniMessage.deserialize(translations.get("blacklist.info.at", unix)));
                            } else
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.not-blacklisted", args[1])));

                        }

                        case TypeUtil.EntryType.JAVA, TypeUtil.EntryType.BEDROCK -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE name = ?", args[1]);

                            if (rs.next()) {
                                sendInfo(rs, sender, args[1]);
                            } else {
                                ResultSet rsUserFix = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE name = ?", args[1] + " (Old Name)");
                                if (rsUserFix.next()) {
                                    sendInfo(rsUserFix, sender, args[1]);
                                } else
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.not-blacklisted", args[1])));
                            }
                        }
                    }
                }

                default -> {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("invalid-syntax")));
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.usage.command")));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private void sendInfo(@NotNull ResultSet resultSet, @NotNull CommandSender sender, @NotNull String entry) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

            String byPlayer = resultSet.getString("byPlayer");
            String blacklistedReason = resultSet.getString("reason");
            blacklistedReason = blacklistedReason == null ? "No reason given" : blacklistedReason;
            String unix = sdf.format(resultSet.getLong("unix"));

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
        try {
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE uuid = ?", uuid.toString());
            ResultSet rsUserFix = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE name = ?", playerName);

            //User is not blacklisted
            if (!rs.next()) {
                if (rsUserFix.next()) {
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
                    ResultSet rsMotd = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", blacklisted.getAddress().getAddress().getHostAddress());
                    if (!rsMotd.next()) {
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
        private final CommandSender player;
        private final String reason;
        private final TypeUtil.EntryType type;

        public UserBlacklistAddCallback(CommandSender player, String reason, TypeUtil.EntryType type) {
            this.player = player;
            this.reason = reason;
            this.type = type;
        }

        @Override
        public void onResponse(@NotNull Call call, Response response) throws IOException {
            String body = response.body().string();
            if (checkForGoodResponse(body, type) != null) {
                player.sendMessage(checkForGoodResponse(body, type));

            } else if (response.code() == 429) {
                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("http.rate-limit")));
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

                blacklistUser(player, UUID.fromString(uuid.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5")), name, reason);
            }

        }

        @Override
        public void onFailure(@NotNull Call call, IOException e) {
            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("http.error")));
            player.sendMessage(Component.text(Keklist.getTranslations().get("http.detail", e.getMessage())));
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
    private void handleList(@NotNull CommandSender sender, int page) {
        try (ResultSet rs =
                     Keklist.getDatabase().onQuery("SELECT * FROM (SELECT uuid, byPlayer, unix FROM blacklist UNION ALL SELECT ip, byPlayer, unix FROM blacklistIp UNION SELECT ip, byPlayer, unix FROM blacklistMotd) LIMIT 8 OFFSET ?", (page - 1) * 8)) {

            if (!rs.next() || page < 1) {
                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.list.empty")));
                return;
            }

            StringBuilder listMessage = new StringBuilder();

            listMessage.append("\n").append(Keklist.getTranslations().get("blacklist.list.header")).append("\n\n");

            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

            do {
                String entry = rs.getString(1);
                String byPlayer = rs.getString(2);
                String date = sdf.format(new Date(rs.getLong(3)));

                TypeUtil.EntryType type = TypeUtil.getEntryType(entry);

                switch (type) {
                    case IPv4, IPv6, DOMAIN -> {
                        ResultSet rsMotd = Keklist.getDatabase().onQuery("SELECT CASE WHEN EXISTS (SELECT 1 FROM blacklistIp WHERE ip = ?) THEN NULL ELSE (SELECT 1 FROM blacklistMotd WHERE ip = ?) END", entry, entry);
                        boolean isMotd = false;

                        if (rsMotd.next())
                            isMotd = rsMotd.getInt(1) == 1;

                        listMessage.append(Keklist.getTranslations().get("blacklist.list.entry", date, isMotd ? entry + " (MOTD)" : entry, byPlayer)).append("\n");
                    }

                    case UUID -> {
                        ResultSet rsName = Keklist.getDatabase().onQuery("SELECT name FROM blacklist WHERE uuid = ?", entry);
                        String name = rsName.next() ? rsName.getString("name") : "Unknown";

                        listMessage.append(Keklist.getTranslations().get("blacklist.list.entry.player", date, entry, name, byPlayer)).append("\n");
                    }
                }

            } while (rs.next());

            listMessage.append("\n").append(Keklist.getTranslations().get("blacklist.list.footer", Math.max(page - 1, 0), page, page + 1));

            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(listMessage.toString()));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[]
            args) throws IllegalArgumentException {
        if (args.length < 2) {
            return List.of("add", "remove", "motd", "info", "list");
        } else if (args.length == 2) {
            try {
                switch (args[0]) {
                    case "remove", "info" -> {
                        if (!sender.hasPermission("keklist.blacklist.info")
                                || !sender.hasPermission("keklist.blacklist.remove"))
                            return Collections.emptyList();


                        List<String> list = new ArrayList<>();

                        ResultSet rsUser = Keklist.getDatabase().onQuery("SELECT name FROM blacklist");
                        while (rsUser.next()) {
                            list.add(rsUser.getString("name"));
                        }

                        ResultSet rsIp = Keklist.getDatabase().onQuery("SELECT ip FROM blacklistIp");
                        while (rsIp.next()) {
                            list.add(rsIp.getString("ip"));
                        }

                        ResultSet rsMotd = Keklist.getDatabase().onQuery("SELECT ip FROM blacklistMotd");
                        while (rsMotd.next()) {
                            if (list.contains(rsMotd.getString("ip"))) {
                                continue;
                            }

                            list.add(rsMotd.getString("ip") + "(motd)");
                        }

                        return list;
                    }

                    case "add" -> {
                        if (!sender.hasPermission("keklist.blacklist.add")) return Collections.emptyList();

                        List<String> completions = new ArrayList<>();

                        Bukkit.getOnlinePlayers().forEach(player -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE name = ?", player.getName());
                            try {
                                if (!rs.next()) {
                                    completions.add(player.getName());
                                }
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }

                        });

                        Bukkit.getOnlinePlayers().forEach(player -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", player.getAddress().getAddress().getHostAddress());
                            try {
                                if (!rs.next()) {
                                    completions.add(player.getAddress().getAddress().getHostAddress() + "(" + player.getName() + ")");
                                }
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        });

                        return completions;
                    }

                    case "motd" -> {
                        if (!sender.hasPermission("keklist.blacklist.motd")) return Collections.emptyList();

                        List<String> completions = new ArrayList<>();
                        Bukkit.getOnlinePlayers().forEach(player -> {
                            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", player.getAddress().getAddress().getHostAddress());

                            try {
                                if (!rs.next()) {
                                    completions.add(player.getAddress().getAddress().getHostAddress() + "(" + player.getName() + ")");
                                }
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        });

                        return completions;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Collections.emptyList();
    }
}