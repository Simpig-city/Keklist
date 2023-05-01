package de.hdg.keklist.api.events.whitelist;

import de.hdg.keklist.Keklist;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * This event is called when a player is removed from the whitelist.
 */
public class UUIDRemovedFromWhitelistEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final UUID uuid;

    public UUIDRemovedFromWhitelistEvent(@NotNull Player player) {
        this.uuid = player.getUniqueId();

        Bukkit.getOfflinePlayer(uuid).setWhitelisted(false);

        if (Keklist.getPlanHook() != null)
            Keklist.getPlanHook().getCaller().ifPresent(caller -> {
                caller.updatePlayerData(uuid, null);
                caller.updateServerData();
            });
    }

    public UUIDRemovedFromWhitelistEvent(@NotNull UUID uuid) {
        this.uuid = uuid;

        Bukkit.getOfflinePlayer(uuid).setWhitelisted(false);

        if (Keklist.getPlanHook() != null)
            Keklist.getPlanHook().getCaller().ifPresent(caller -> {
                caller.updatePlayerData(uuid, null);
                caller.updateServerData();
            });
    }

    /**
     * Get the UUID which was removed from the whitelist.
     *
     * @return The UUID removed from the whitelist.
     */
    public @NotNull UUID getUUID() {
        return uuid;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
