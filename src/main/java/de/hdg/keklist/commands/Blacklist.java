package de.hdg.keklist.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.hdg.keklist.Keklist;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Blacklist extends Command {

    private static final OkHttpClient client = new OkHttpClient();

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
            UUID uuid = null;


            if (args[1].matches("^[a-zA-Z0-9_]{2,16}$")) {
                type = BlacklistType.USERNAME;

                Request request = new Request.Builder().url("https://api.mojang.com/users/profiles/minecraft/" + args[1]).build();
                try (Response response = client.newCall(request).execute()) {
                    JsonElement element = JsonParser.parseString(response.body().string());

                    //Mojang API returned an error
                    if(!element.isJsonNull()){
                        if (element.getAsJsonObject().get("error") != null) {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Der Spieler existiert nicht! Mehr zum Fehler in der Konsole"));
                            Keklist.getInstance().getLogger().warning("Der Spieler " + args[1] + " existiert nicht!");
                            Keklist.getInstance().getLogger().warning("Details: " + element.getAsJsonObject().get("errorMessage").getAsString());
                            return true;
                        }
                    }else{
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
                type = BlacklistType.IPv4;
            } else if (args[1].matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$")) {
                type = BlacklistType.IPv6;
            } else {
                if(Keklist.getInstance().getFloodgateApi() != null){
                    if(args[1].startsWith(Keklist.getInstance().getConfig().getString("floodgate.prefix"))){
                        FloodgateApi api = Keklist.getInstance().getFloodgateApi();

                        uuid = api.getUuidFor(args[1].replace(Keklist.getInstance().getConfig().getString("floodgate.prefix"), "")).get();
                        type = BlacklistType.USERNAME;
                    }
                }

                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Ungültige IP oder Username!"));
                return true;
            }


            String reason = null;
            if (args.length > 2) {
                reason = String.join(" ", args).substring(args[0].length() + args[1].length() + 2);
            }


            switch (args[0]) {
                case "add" -> {
                    if (type.equals(BlacklistType.USERNAME)) {
                        ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE uuid = ?", uuid.toString());

                        //User is not blacklisted
                        if(!rs.next()) {
                            if (reason == null) {
                                Keklist.getDatabase().onUpdate("INSERT INTO blacklist (uuid, name, by, reason) VALUES (?, ?, ?, ?)", uuid.toString(), args[1], senderName, reason);
                            } else {
                                Keklist.getDatabase().onUpdate("INSERT INTO blacklist (uuid, name, by) VALUES (?, ?, ?, ?)", uuid.toString(), args[1], senderName);
                            }

                            Player blacklisted = Bukkit.getPlayer(args[1]);
                            if(blacklisted != null){
                                ResultSet rsMotd = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", blacklisted.getAddress().getAddress().getHostAddress());
                                if (!rsMotd.next()) {
                                    Keklist.getDatabase().onUpdate("INSERT INTO blacklistMotd (ip, by) VALUES (?, ?)", blacklisted.getAddress().getAddress().getHostAddress(), senderName);
                                }
                            }

                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + args[1] + " wurde erfolgreich zur Blacklist hinzugefügt!"));
                        }else
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>" + args[1] + " ist bereits auf der Blacklist!"));

                    } else if (type.equals(BlacklistType.IPv4) || type.equals(BlacklistType.IPv6)) {
                        ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", args[1]);

                        if (!rs.next()) {
                            if (reason == null) {
                                Keklist.getDatabase().onUpdate("INSERT INTO blacklistIp (ip, by, reason) VALUES (?, ?, ?)", args[1], senderName, reason);
                            } else {
                                Keklist.getDatabase().onUpdate("INSERT INTO blacklistIp (ip, by) VALUES (?, ?, ?)", args[1], senderName);
                            }

                            ResultSet rsMotd = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", args[1]);
                            if (!rsMotd.next()) {
                                Keklist.getDatabase().onUpdate("INSERT INTO blacklistMotd (ip, by) VALUES (?, ?)", args[1], senderName);
                            }

                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + args[1] + " wurde erfolgreich zur Blacklist hinzugefügt!"));
                        } else {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Diese IP ist bereits geblacklistet!"));
                            return true;
                        }
                    }
                }

                case "remove" -> {
                    if (type.equals(BlacklistType.USERNAME)) {
                        ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE name = ?", args[1]);
                        if (rs.next()) {
                            Keklist.getDatabase().onUpdate("DELETE FROM blacklist WHERE name = ?", args[1]);

                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + args[1] + " wurde erfolgreich von der Blacklist entfernt!"));
                        } else {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Dieser User ist nicht geblacklistet!"));
                            return true;
                        }

                    } else if (type.equals(BlacklistType.IPv4) || type.equals(BlacklistType.IPv6)) {
                        ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", args[1]);
                        ResultSet rsMotd = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", args[1]);
                        if (rs.next() || rsMotd.next()) {
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
                            Keklist.getDatabase().onUpdate("INSERT INTO blacklistMotd (ip, by) VALUES (?, ?)", args[1], senderName);
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

    /**
     * Types of blacklist entries
     * <p> USERNAME: Blacklist a player by their username
     * <p> IPv4: Blacklist a player by their IPv4 address
     * <p> IPv6: Blacklist a player by their IPv6 address
     */
    private enum BlacklistType {
        IPv4, IPv6, USERNAME
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            return List.of("add", "remove", "motd");
        } else if (args.length == 2) {
            try {
                switch (args[0]){
                    case "remove" ->{
                        List<String> list = new ArrayList<String>();

                        ResultSet rsUser = Keklist.getDatabase().onQuery("SELECT name FROM blacklist");
                        while(rsUser.next()){
                            list.add(rsUser.getString("name"));
                        }

                        ResultSet rsIp = Keklist.getDatabase().onQuery("SELECT ip FROM blacklistIp");
                        while(rsIp.next()){
                            list.add(rsIp.getString("ip"));
                        }

                        ResultSet rsMotd = Keklist.getDatabase().onQuery("SELECT ip FROM blacklistMotd");
                        while(rsMotd.next()){
                            if(list.contains(rsMotd.getString("ip"))){
                                continue;
                            }

                            list.add(rsMotd.getString("ip")+ "(motd)");
                        }

                        return list;
                    }

                    case "add" ->{
                        List<String> completions = new ArrayList<String>();

                        Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
                        Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getAddress().getAddress().getHostAddress() +"(" + player.getName() + ")"));

                        return completions;
                    }

                    case "motd" ->{
                        List<String> completions = new ArrayList<String>();
                        Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getAddress().getAddress().getHostAddress() +"(" + player.getName() + ")"));

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