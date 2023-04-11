package de.hdg.keklist.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class KeklistCommand extends Command {
    protected KeklistCommand(@NotNull String name) {
        super("keklist");
        setAliases(List.of("kek"));
        setPermission("keklist.manage");
        setUsage("/keklist <blacklist/whitelist> [feature] <enable/disable>");
        setDescription("Command to manage the Keklist plugin");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        return false;
    }
}
