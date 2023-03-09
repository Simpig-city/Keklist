package de.hdg.keklist.commandCompletions;

import de.hdg.keklist.Keklist;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WhitelistCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            return List.of("add", "remove");
        } else if (args.length == 2) {
            try {
                switch (args[0]) {
                    case "remove" -> {
                        List<String> list = new ArrayList<String>();

                        ResultSet rsUser = Keklist.getDatabase().onQuery("SELECT name FROM whitelist");
                        while (rsUser.next()){
                            list.add(rsUser.getString("name"));
                        }

                        ResultSet rsIp = Keklist.getDatabase().onQuery("SELECT ip FROM whitelistIp");
                        while (rsIp.next()) {
                            list.add(rsIp.getString("ip"));
                        }

                        return list;
                    }

                    case "add" -> {
                        List<String> completions = new ArrayList<String>();

                        Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
                        Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getAddress().getAddress().getHostAddress() + "(" + player.getName() + ")"));

                        return completions;
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }
}
