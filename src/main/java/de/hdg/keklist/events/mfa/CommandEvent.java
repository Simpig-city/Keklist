package de.hdg.keklist.events.mfa;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.util.mfa.MFAUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class CommandEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        if (event.getMessage().toLowerCase().substring(1).startsWith("keklist 2fa"))
            return;

        if (!Keklist.getInstance().getConfig().getBoolean("2fa.enabled"))
            return;

        if (Keklist.getInstance().getConfig().getBoolean("2fa.enforce-settings") && !MFAUtil.hasMFAEnabled(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.not-verified")));
            return;
        }

        boolean match = containsAllPatternsInOrder(Keklist.getInstance().getConfig().getStringList("2fa.require-2fa-on-command.commands"), event.getMessage().substring(1).trim().toLowerCase().split(" "));

        if (!MFAUtil.hasMFAEnabled(event.getPlayer()))
            return;

        if (Keklist.getInstance().getConfig().getBoolean("2fa.require-2fa-on-any-command")) {
            if (!MFAUtil.hasVerified(event.getPlayer())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.not-verified")));
            }
        } else if (Keklist.getInstance().getConfig().getBoolean("2fa.require-2fa-on-command.enabled")
                && match
                && !MFAUtil.hasVerified(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.not-verified")));
        }
    }

    /* This subcommand matcher was pain asf and not even ai could help me with it. BUT it works :) */

    /**
     * Checks if the command with any arguments matches any of the blocked commands
     *
     * @param mfaCommands a list of blocked commands to check against
     * @param commandWithArgs the command with arguments
     * @return true if the (sub)command matches any command in the list, false otherwise
     */
    private boolean containsAllPatternsInOrder(@NotNull List<String> mfaCommands, @NotNull String... commandWithArgs) {
        for (String blockedCommand : mfaCommands) {
            String[] commandSplit = blockedCommand.split(" ");

            int blockedCommandLength = commandSplit.length;

            // Don't need to check any further if the typed command is shorter than the blocked command
            if (commandWithArgs.length < blockedCommandLength) {
                continue;
            }

            if (checkCommandWithArgs(commandSplit, commandWithArgs)) {
                return true;
            }

        }
        return false;
    }

    /**
     * Checks if the command with any arguments matches the blocked command
     *
     * @param blockedCommand the first part of the blocked command
     * @param commandWithArgs the command with arguments
     * @return true if the (sub)command matches, false otherwise
     */
    private boolean checkCommandWithArgs(@NotNull String[] blockedCommand, @NotNull String[] commandWithArgs) {
        String[] commandWithArgsStripped = Arrays.copyOfRange(commandWithArgs, 0, blockedCommand.length); // Strip anything off the command that is not needed. Length check is done before this method is called

        if(!commandWithArgsStripped[0].equalsIgnoreCase(blockedCommand[0])) {
            return false; // If first element is not the same, return false immediately
        }

        boolean match;
        for (int i = 0; i < commandWithArgsStripped.length; i++) { // Check if the rest of the elements match
            match = blockedCommand[i].equalsIgnoreCase(commandWithArgsStripped[i]);

            if (i == commandWithArgsStripped.length - 1) { // If we are at the last element, return the match
                return match;
            }
        }

        return false; // If we reach this point, the command is not a match
    }
}
