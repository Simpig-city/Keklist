package de.hdg.keklist.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.api.events.blacklist.*;
import net.kyori.adventure.text.Component;
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
import java.util.*;

public class Blacklist extends Command {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();
    private static final TypeToken<Map<String, String>> token = new TypeToken<>() {};

    public Blacklist() {
        super("blacklist");
        setAliases(List.of("bl"));
        setPermission("keklist.blacklist");
        setUsage("/blacklist <add/remove/motd> [Spieler/IP] [Grund]");
        setDescription("Blacklistet einen Spieler oder eine IP");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Invalider Syntax!"));
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Benutze: /blacklist <add/remove/motd> [Spieler/IP] [Grund]"));
            return true;
        }

        try {
            String senderName = sender.getName();
            BlacklistType type;
            UUID bedrockUUID = null;

            if (args[1].matches("^[a-zA-Z0-9_]{2,16}$")) {
                type = BlacklistType.JAVA;
            } else if (args[1].matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
                type = BlacklistType.IPv4;
            } else if (args[1].matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$")) {
                type = BlacklistType.IPv6;
            } else {
                if (Keklist.getInstance().getFloodgateApi() != null) {
                    if (args[1].startsWith(Keklist.getInstance().getConfig().getString("floodgate.prefix"))) {
                        FloodgateApi api = Keklist.getInstance().getFloodgateApi();

                        bedrockUUID = api.getUuidFor(args[1].replace(Keklist.getInstance().getConfig().getString("floodgate.prefix"), "")).get();
                        type = BlacklistType.BEDROCK;
                    } else {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Ungültige IP oder Username!"));
                        return true;
                    }
                } else {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Ungültige IP oder Username!"));
                    return true;
                }
            }

            String reason = null;
            if (args.length > 2) {
                reason = String.join(" ", args).substring(args[0].length() + args[1].length() + 2);
            }

            switch (args[0]) {
                case "add" -> {
                    if (type.equals(BlacklistType.JAVA)) {
                        Request request = new Request.Builder().url("https://api.mojang.com/users/profiles/minecraft/" + args[1]).build();
                        client.newCall(request).enqueue(new UserBlacklistAddCallback((sender instanceof Player) ? (Player) sender : null, reason));
                    } else if (type.equals(BlacklistType.IPv4) || type.equals(BlacklistType.IPv6)) {
                        ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", args[1]);

                        if (!rs.next()) {
                            if (reason == null) {
                                new IpAddToBlacklistEvent(args[1], null).callEvent();
                                Keklist.getDatabase().onUpdate("INSERT INTO blacklistIp (ip, byPlayer, unix, reason) VALUES (?, ?, ?, ?)", args[1], senderName, System.currentTimeMillis(), reason);
                            } else {
                                if (reason.length() <= 1500) {
                                    new IpAddToBlacklistEvent(args[1], reason).callEvent();
                                    Keklist.getDatabase().onUpdate("INSERT INTO blacklistIp (ip, byPlayer, unix) VALUES (?, ?, ?)", args[1], senderName, System.currentTimeMillis());
                                } else {
                                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Der Grund ist zu lang!"));
                                    return true;
                                }
                            }

                            ResultSet rsMotd = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", args[1]);
                            if (!rsMotd.next()) {
                                new IpAddToMOTDBlacklistEvent(args[1]).callEvent();
                                Keklist.getDatabase().onUpdate("INSERT INTO blacklistMotd (ip, byPlayer, unix) VALUES (?, ?, ?)", args[1], senderName, System.currentTimeMillis());
                            }

                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + args[1] + " wurde erfolgreich zur Blacklist hinzugefügt!"));
                        } else {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Diese IP ist bereits geblacklistet!"));
                            return true;
                        }
                    } else if (type.equals(BlacklistType.BEDROCK)) {
                        new UUIDAddToBlacklistEvent(bedrockUUID, reason).callEvent();
                        blacklistUser(sender, bedrockUUID, args[1], reason);
                    }
                }

                case "remove" -> {
                    if (type.equals(BlacklistType.JAVA) || type.equals(BlacklistType.BEDROCK)) {
                        ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE name = ?", args[1]);
                        if (rs.next()) {
                            new PlayerRemovedFromBlacklist(args[1]).callEvent();
                            Keklist.getDatabase().onUpdate("DELETE FROM blacklist WHERE name = ?", args[1]);

                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + args[1] + " wurde erfolgreich von der Blacklist entfernt!"));
                        } else {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Dieser User ist nicht geblacklistet!"));
                        }
                    } else if (type.equals(BlacklistType.IPv4) || type.equals(BlacklistType.IPv6)) {
                        ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", args[1]);
                        ResultSet rsMotd = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", args[1]);
                        if (rs.next() || rsMotd.next()) {
                            new IpRemovedFromBlacklistEvent(args[1]).callEvent();
                            new IpRemovedFromMOTDBlacklistEvent(args[1]).callEvent();

                            Keklist.getDatabase().onUpdate("DELETE FROM blacklistIp WHERE ip = ?", args[1]);
                            Keklist.getDatabase().onUpdate("DELETE FROM blacklistMotd WHERE ip = ?", args[1]);

                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + args[1] + " wurde erfolgreich von der (MOTD-)Blacklist entfernt!"));
                        } else {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Diese IP ist nicht geblacklistet!"));
                            return true;
                        }
                    }
                }

                case "motd" -> {
                    if (type.equals(BlacklistType.IPv4)) {
                        ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", args[1]);
                        if (rs.next()) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Diese IP wurde bereits zu der blacklist motd hinzugefügt!"));
                        } else {
                            new IpAddToMOTDBlacklistEvent(args[1]).callEvent();
                            Keklist.getDatabase().onUpdate("INSERT INTO blacklistMotd (ip, byPlayer, unix) VALUES (?, ?, ?)", args[1], senderName, System.currentTimeMillis());
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + args[1] + " wurde erfolgreich zur Blacklist Motd hinzugefügt!"));
                        }
                    } else
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Der Befehl /blacklist motd ist nur für IPs verfügbar!"));
                }

                default -> {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Invalider Syntax!"));
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Benutze: /blacklist <add/remove> <Spieler/IP> <reason>"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private void blacklistUser(CommandSender from, UUID uuid, String playerName, String reason) {
        try {
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE uuid = ?", uuid.toString());

            //User is not blacklisted
            if (!rs.next()) {
                if (reason == null) {
                    new UUIDAddToBlacklistEvent(uuid, null).callEvent();
                    Keklist.getDatabase().onUpdate("INSERT INTO blacklist (uuid, name, byPlayer, unix, reason) VALUES (?, ?, ?, ?, ?)", uuid.toString(), playerName, from.getName(), System.currentTimeMillis(), reason);
                } else {
                    if (reason.length() <= 1500) {
                        new UUIDAddToBlacklistEvent(uuid, reason).callEvent();
                        Keklist.getDatabase().onUpdate("INSERT INTO blacklist (uuid, name, byPlayer, unix) VALUES (?, ?, ?, ?)", uuid.toString(), playerName, from.getName(), System.currentTimeMillis());
                    } else {
                        from.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Der Grund darf nicht länger als 1500 Zeichen sein!"));
                        return;
                    }
                }

                Player blacklisted = Bukkit.getPlayer(playerName);
                if (blacklisted != null) {
                    ResultSet rsMotd = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", blacklisted.getAddress().getAddress().getHostAddress());
                    if (!rsMotd.next()) {
                        new IpAddToMOTDBlacklistEvent(blacklisted.getAddress().getAddress().getHostAddress()).callEvent();
                        Keklist.getDatabase().onUpdate("INSERT INTO blacklistMotd (ip, byPlayer, unix) VALUES (?, ?, ?)", blacklisted.getAddress().getAddress().getHostAddress(), from.getName(), System.currentTimeMillis());
                    }
                }

                from.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + playerName + " wurde erfolgreich zur Blacklist hinzugefügt!"));
            } else
                from.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>" + playerName + " ist bereits auf der Blacklist!"));

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private class UserBlacklistAddCallback implements Callback {
        private final Player player;
        private final String reason;

        public UserBlacklistAddCallback(Player player, String reason) {
            this.player = player;
            this.reason = reason;
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if (player.isOnline()) {
                if (checkForGoodResponse(response.body().string()) != null) {
                    player.sendMessage(checkForGoodResponse(response.body().string()));
                    return;
                } else {
                    Map<String, String> map = gson.fromJson(response.body().string(), token);
                    String uuid = map.get("id");
                    String name = map.get("name");

                    blacklistUser(player, UUID.fromString(uuid), name, reason);
                }
            }
        }

        @Override
        public void onFailure(Call call, IOException e) {
            if (player.isOnline()) {
                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Etwas ist schiefgelaufen!"));
                player.sendMessage(Component.text("Details: " + e.getMessage()));
            }
        }
    }

    private Component checkForGoodResponse(String response) {
        JsonElement element = JsonParser.parseString(response);

        if (!element.isJsonNull()) {
            if (element.getAsJsonObject().get("error") != null) {
                return Keklist.getInstance().getMiniMessage().deserialize("<red>Der Spieler existiert nicht! Mehr zum Fehler in der Konsole");
            }
        } else {
            return Component.text("Response is null! Report this to the developer!");
        }

        return null;
    }

    /**
     * Types of blacklist entries
     * <p> USERNAME: Blacklist a player by their username
     * <p> IPv4: Blacklist a player by their IPv4 address
     * <p> IPv6: Blacklist a player by their IPv6 address
     */
    private enum BlacklistType {
        IPv4, IPv6, JAVA, BEDROCK
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            return List.of("add", "remove", "motd");
        } else if (args.length == 2) {
            try {
                switch (args[0]) {
                    case "remove" -> {
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