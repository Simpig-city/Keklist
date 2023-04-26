package de.hdg.keklist.api.events.blacklist;

import de.hdg.keklist.Keklist;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * This event is called when a player is added to the blacklist.
 * <b>Only called when UUID gets removed by API</b>
 */
public class UUIDRemovedFromBlacklistEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();
    private final UUID uuid;

    public UUIDRemovedFromBlacklistEvent(@NotNull UUID uuid) {
        this.uuid = uuid;

        if (Keklist.getPlanHook() != null)
            Keklist.getPlanHook().getCaller().ifPresent(caller -> {
                caller.updatePlayerData(uuid, null);
                caller.updateServerData();
            });
    }

    public UUIDRemovedFromBlacklistEvent(@NotNull Player player) {
        this.uuid = player.getUniqueId();
    }

    /**
     * Returns the UUID that was removed from the blacklist.
     *
     * @return The UUID that was removed from the blacklist.
     */
    public @NotNull UUID getUUID() {
        return uuid;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
