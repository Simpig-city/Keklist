package de.hdg.keklist.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.api.events.whitelist.IpAddToWhitelistEvent;
import de.hdg.keklist.api.events.whitelist.IpRemovedFromWhitelistEvent;
import de.hdg.keklist.api.events.whitelist.UUIDAddToWhitelistEvent;
import de.hdg.keklist.api.events.whitelist.UUIDRemovedFromWhitelistEvent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Whitelist extends Command {

    private static final OkHttpClient client = new OkHttpClient();

    public Whitelist() {
        super("whitelist");
        setPermission("keklist.whitelist");
        setDescription("Whitelist commands");
        setAliases(List.of("wl"));
        setUsage("/whitelist <add/remove> <Spieler/IP>");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Invalider Syntax!"));
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Benutze: /whitelist <add/remove> <Spieler/IP>"));
            return true;
        }

        try {
            String senderName = sender.getName();
            WhiteListType type;
            UUID uuid = null;

            if (args[1].matches("^[a-zA-Z0-9_]{2,16}$")) {
                type = WhiteListType.USERNAME;

                Request request = new Request.Builder().url("https://api.mojang.com/users/profiles/minecraft/" + args[1]).build();
                try (Response response = client.newCall(request).execute()) {

                    String responseString = response.body().string();
                    JsonElement element = JsonParser.parseString(responseString);

                    //Mojang API returned an error or user not found
                    if (!element.isJsonNull()) {
                        if (element.getAsJsonObject().get("error") != null) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Der Spieler existiert nicht! Mehr zum Fehler in der Konsole"));
                            Keklist.getInstance().getLogger().warning("Der Spieler " + args[1] + " existiert nicht!");
                            Keklist.getInstance().getLogger().warning("Details: " + element.getAsJsonObject().get("errorMessage").getAsString());
                            return true;
                        }
                    } else {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Der Spieler existiert nicht! Mehr zum Fehler in der Konsole"));
                        Keklist.getInstance().getLogger().warning("Der Spieler " + args[1] + " existiert nicht!");
                        Keklist.getInstance().getLogger().warning("Details: response is null");
                        return true;
                    }

                    uuid = UUID.fromString(element.getAsJsonObject().get("id").getAsString().replaceFirst(
                            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                            "$1-$2-$3-$4-$5"));
                }
            } else if (args[1].matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
                type = WhiteListType.IPv4;
            } else if (args[1].matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$")) {
                type = WhiteListType.IPv6;
            } else {
                if (Keklist.getInstance().getFloodgateApi() != null) {
                    if (args[1].startsWith(Keklist.getInstance().getConfig().getString("floodgate.prefix"))) {
                        FloodgateApi api = Keklist.getInstance().getFloodgateApi();

                        uuid = api.getUuidFor(args[1].replace(Keklist.getInstance().getConfig().getString("floodgate.prefix"), "")).get();
                        type = WhiteListType.USERNAME;
                    } else {
                        sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Ungültige IP oder Username! <grey><o>Vielleicht überprüfe den Floodgate Prefix"));
                        return true;
                    }
                } else {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Ungültige IP oder Username! <grey><o>Vielleicht überprüfe den Floodgate Prefix"));
                    return true;
                }
            }


            switch (args[0]) {
                case "add" -> {
                    if (type.equals(WhiteListType.USERNAME)) {
                        ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", uuid.toString());

                        if (!rs.next()) {
                            new UUIDAddToWhitelistEvent(uuid).callEvent();
                            Keklist.getDatabase().onUpdate("INSERT INTO whitelist (uuid, name, byPlayer, unix) VALUES (?, ?, ?, ?)", uuid.toString(), args[1], senderName, System.currentTimeMillis());
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + args[1] + " wurde erfolgreich zur Whitelist hinzugefügt!"));

                        } else
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Dieser User ist bereits gewhitelistet!"));

                    } else if (type.equals(WhiteListType.IPv4) || type.equals(WhiteListType.IPv6)) {
                        ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp WHERE ip = ?", args[1]);

                        if (!rs.next()) {
                            new IpAddToWhitelistEvent(args[1]).callEvent();
                            Keklist.getDatabase().onUpdate("INSERT INTO whitelistIp (ip, byPlayer, unix) VALUES (?, ?, ?)", args[1], senderName, System.currentTimeMillis());
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + args[1] + " wurde erfolgreich zur Whitelist hinzugefügt!"));
                        } else
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Diese IP ist bereits gewhitelistet!"));

                    }

                    return true;
                }

                case "remove" -> {
                    if (type.equals(WhiteListType.USERNAME)) {
                        ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE name = ?", args[1]);
                        if (rs.next()) {
                            new UUIDRemovedFromWhitelistEvent(uuid).callEvent();
                            Keklist.getDatabase().onUpdate("DELETE FROM whitelist WHERE name = ?", args[1]);
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + args[1] + " wurde erfolgreich von der Whitelist entfernt!"));
                        } else {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Dieser User ist nicht gewhitelistet!"));
                            return true;
                        }
                    } else if (type.equals(WhiteListType.IPv4) || type.equals(WhiteListType.IPv6)) {
                        ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp WHERE ip = ?", args[1]);
                        if (rs.next()) {
                            new IpRemovedFromWhitelistEvent(args[1]).callEvent();
                            Keklist.getDatabase().onUpdate("DELETE FROM whitelistIp WHERE ip = ?", args[1]);
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + args[1] + " wurde erfolgreich von der Whitelist entfernt!"));
                        } else {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Diese IP ist nicht gewhitelistet!"));
                            return true;
                        }
                    }

                    return true;
                }

                default -> {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Invalider Syntax!"));
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Benutze: /whitelist <add/remove/list> [Spieler/IP]"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Types of whitelist entries
     * <p> USERNAME: Whitelist a player by their username
     * <p> IPv4: Whitelist a player by their IPv4 address
     * <p> IPv6: Whitelist a player by their IPv6 address
     */
    private enum WhiteListType {
        IPv4, IPv6, USERNAME
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            return List.of("add", "remove");
        } else if (args.length == 2) {
            try {
                switch (args[0]) {
                    case "remove" -> {
                        List<String> list = new ArrayList<>();

                        ResultSet rsUser = Keklist.getDatabase().onQuery("SELECT name FROM whitelist");
                        while (rsUser.next()) {
                            list.add(rsUser.getString("name"));
                        }

                        ResultSet rsIp = Keklist.getDatabase().onQuery("SELECT ip FROM whitelistIp");
                        while (rsIp.next()) {
                            list.add(rsIp.getString("ip"));
                        }

                        return list;
                    }

                    case "add" -> {
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
                                ;
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        });

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
