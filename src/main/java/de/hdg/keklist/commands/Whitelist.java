package de.hdg.keklist.commands;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.database.DB;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.Statement;

public class Whitelist implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Invalider Syntax!"));
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Benutze: /whitelist <add/remove> <Spieler>"));
        }

        String player = args[1];
        String senderName = sender.getName();
        long timestamp = System.currentTimeMillis() / 1000L;

        Connection conn = DB.getDB();
        Statement statement = null;

        try {
            switch (args[0]) {
                case "add" -> {
                    statement = conn.createStatement();
                    statement.executeUpdate("INSERT INTO whitelist (player, sender, timestamp) VALUES ('" + player + "', '" + senderName + "', '" + timestamp + "')");
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + player + " wurde erfolgreich zur Whitelist hinzugefügt!"));
                }

                case "remove" -> {
                    statement = conn.createStatement();
                    statement.executeUpdate("DELETE FROM whitelist WHERE player = '" + player + "'");
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<green>" + player + " wurde erfolgreich von der Whitelist entfernt!"));
                }

                default -> {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Invalider Syntax!"));
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize("<red>Benutze: /whitelist <add/remove> <Spieler>"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }
}
