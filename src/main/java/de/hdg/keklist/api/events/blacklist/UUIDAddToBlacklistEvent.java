package de.hdg.keklist.api.events.blacklist;

import de.hdg.keklist.Keklist;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * This event is called when a UUID is added to the blacklist.
 * <p>
 * This can be a floodgate UUID
 * </p>
 */
public class UUIDAddToBlacklistEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();
    private final UUID uuid;
    private final String reason;

    public UUIDAddToBlacklistEvent(@NotNull Player player, @Nullable String reason) {
        this.uuid = player.getUniqueId();
        this.reason = reason;

        if (Keklist.getPlanHook() != null)
            Keklist.getPlanHook().getCaller().ifPresent(caller -> {
                caller.updatePlayerData(uuid, player.getName());
                caller.updateServerData();
            });
    }

    public UUIDAddToBlacklistEvent(@NotNull UUID uuid, @Nullable String reason) {
        this.uuid = uuid;
        this.reason = reason;

        if (Keklist.getPlanHook() != null)
            Keklist.getPlanHook().getCaller().ifPresent(caller -> {
                caller.updatePlayerData(uuid, null);
                caller.updateServerData();
            });
    }

    /**
     * Returns the UUID that was added to the blacklist.
     *
     * @return The UUID that was added to the blacklist.
     */
    public @NotNull UUID getUUID() {
        return uuid;
    }

    /**
     * Returns the reason why the UUID was added to the blacklist.
     *
     * @return The reason why the UUID was added to the blacklist or null if no reason was given.
     */
    public @Nullable String getReason() {
        return reason;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
