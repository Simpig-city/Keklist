package de.hdg.keklist.commandCompletions;

import de.hdg.keklist.database.DB;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BlacklistCompletor implements TabCompleter {

    //  /blacklist add(args[0]) <playername>(args[1]) <reason>(args[2..])

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            return List.of("add", "remove", "motd");
        } else if (args.length == 2) {
            try {
                switch (args[0]){
                    case "remove" ->{
                        List<String> list = new ArrayList<String>();

                        ResultSet rsUser = DB.onQuery("SELECT name FROM blacklist");
                        while(rsUser.next()){
                            list.add(rsUser.getString("name"));
                        }

                        ResultSet rsIp = DB.onQuery("SELECT ip FROM blacklistIp");
                        while(rsIp.next()){
                            list.add(rsIp.getString("ip"));
                        }

                        ResultSet rsMotd = DB.onQuery("SELECT ip FROM blacklistMotd");
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
        return null;
    }
}