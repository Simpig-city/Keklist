package de.hdg.keklist.commandCompletions;

import de.hdg.keklist.database.DB;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class WhitelistCompletor implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove");
        }else if (args.length == 2) {
            List<String> list = new ArrayList<String>();

            try {
            ResultSet rsUser = DB.onQuery("SELECT name FROM whitelist");
            while(rsUser.next()){

                    list.add(rsUser.getString("name"));

            }

            ResultSet rsIp = DB.onQuery("SELECT ip FROM whitelistIp");
            while(rsIp.next()){
                list.add(rsIp.getString("ip"));
            }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return list;
        }

        return null;
    }
}
