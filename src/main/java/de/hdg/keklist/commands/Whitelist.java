package de.hdg.keklist.commands;

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
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Invalider Syntax!");
            sender.sendMessage(ChatColor.RED + "Benutze: /whitelist <add/remove> <Spieler>");
            return true;
        }else if (args.length == 1) {
            sender.sendMessage(ChatColor.RED + "Invalider Syntax!");
            sender.sendMessage(ChatColor.RED + "Benutze: /whitelist <add/remove> <Spieler>");
            return true;
        }

        String player = args[2];
        String senderName = sender.getName();
        long timestamp = System.currentTimeMillis() / 1000L;

        Connection conn = DB.getDB();
        Statement statement = null;

        try {
            if(args[1].equalsIgnoreCase("add")) {
                statement = conn.createStatement();
                statement.executeUpdate("INSERT INTO whitelist (player, by, unix) VALUES ('" + player + "', '" + senderName + "', '" + timestamp + "')");
                sender.sendMessage(ChatColor.GREEN + "Der Spieler " + player + " wurde erfolgreich zur Whitelist hinzugefügt!");
            }else if (args[1].equalsIgnoreCase("remove")) {
                statement = conn.createStatement();
                statement.executeUpdate("DELETE FROM whitelist WHERE player = '" + player + "'");
                sender.sendMessage(ChatColor.GREEN + "Der Spieler " + player + " wurde erfolgreich von der Whitelist entfernt!");
            }else {
                sender.sendMessage(ChatColor.RED + "Invalider Syntax!");
                sender.sendMessage(ChatColor.RED + "Benutze: /whitelist <add/remove> <Spieler>");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }
}
