package de.hdg.keklist.util.mfa;

import de.hdg.keklist.Keklist;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

public class CommandEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        if (event.getMessage().toLowerCase().startsWith("keklist 2fa verify"))
            return;

        if (!Keklist.getInstance().getConfig().getBoolean("2fa.enabled"))
            return;

        if(Keklist.getInstance().getConfig().getBoolean("2fa.enforce-settings") && !MFAUtil.hasMFAEnabled(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.require")));
            return;
        }

        if(!MFAUtil.hasMFAEnabled(event.getPlayer()))
            return;

        if (Keklist.getInstance().getConfig().getBoolean("2fa.require-2fa-on-any-command")) {
            if (!MFAUtil.hasVerified(event.getPlayer())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.not-verified")));
            }
        } else if (Keklist.getInstance().getConfig().getBoolean("2fa.require-2fa-on-command.enabled") &&
                Keklist.getInstance().getConfig().getStringList("2fa.require-2fa-on-command.commands").stream().anyMatch(command -> command.trim().toLowerCase().startsWith(event.getMessage().trim().toLowerCase()))
                && !MFAUtil.hasVerified(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.not-verified")));
        }
    }
}
