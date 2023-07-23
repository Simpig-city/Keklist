package de.hdg.keklist.events;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.api.events.whitelist.PlayerRemovedFromWhitelistEvent;
import de.hdg.keklist.api.events.whitelist.UUIDAddToWhitelistEvent;
import de.hdg.keklist.extentions.WebhookManager;
import io.papermc.paper.event.server.WhitelistStateUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ServerWhitelistChangeEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onStateUpdate(WhitelistStateUpdateEvent event) throws SQLException {

        String playerName = event.getPlayerProfile().getName();
        UUID uuid = event.getPlayerProfile().getId();

        switch (event.getStatus()) {
            case ADDED -> {
                ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", event.getPlayerProfile().getId());
                ResultSet rsUserFix = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE name = ?", event.getPlayerProfile().getName());

                if (!rs.next()) {
                    if (rsUserFix.next()) {
                        Keklist.getDatabase().onUpdate("UPDATE whitelist SET name = ? WHERE name = ?", playerName + " (Old Name)", playerName);
                    }

                    new UUIDAddToWhitelistEvent(uuid).callEvent();

                    Keklist.getDatabase().onUpdate("INSERT INTO whitelist (uuid, name, byPlayer, unix) VALUES (?, ?, ?, ?)", uuid.toString(), playerName, "SYSTEM", System.currentTimeMillis());

                    if (Keklist.getWebhookManager() != null)
                        Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_ADD, playerName, "SYSTEM", System.currentTimeMillis());

                    if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                        Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.add", playerName, "SYSTEM")), "keklist.notify.whitelist");

                }
            }

            case REMOVED -> {
                ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", uuid.toString());

                if (rs.next()) {
                    new PlayerRemovedFromWhitelistEvent(playerName).callEvent();
                    Keklist.getDatabase().onUpdate("DELETE FROM whitelist WHERE uuid = ?", uuid.toString());

                    if (Keklist.getWebhookManager() != null)
                        Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_REMOVE, playerName, "SYSTEM", System.currentTimeMillis());

                    if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                        Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.notify.remove", playerName, "SYSTEM")), "keklist.notify.whitelist");

                }
            }
        }
    }
}