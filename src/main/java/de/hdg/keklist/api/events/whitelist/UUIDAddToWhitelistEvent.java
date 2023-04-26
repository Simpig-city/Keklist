package de.hdg.keklist.api.events.whitelist;

import de.hdg.keklist.Keklist;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * This event is called when a player is added to the whitelist.
 */
public class UUIDAddToWhitelistEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final UUID uuid;

    public UUIDAddToWhitelistEvent(@NotNull Player player) {
        this.uuid = player.getUniqueId();

        Bukkit.getOfflinePlayer(uuid).setWhitelisted(true);

        if (Keklist.getPlanHook() != null)
            Keklist.getPlanHook().getCaller().ifPresent(caller -> {
                caller.updatePlayerData(uuid, null);
                caller.updateServerData();
            });
    }

    public UUIDAddToWhitelistEvent(@NotNull UUID uuid) {
        this.uuid = uuid;

        Bukkit.getOfflinePlayer(uuid).setWhitelisted(true);

        if (Keklist.getPlanHook() != null)
            Keklist.getPlanHook().getCaller().ifPresent(caller -> {
                caller.updatePlayerData(uuid, null);
                caller.updateServerData();
            });
    }

    /**
     * Get the UUID which was added to the whitelist.
     *
     * @return The UUID added to the whitelist.
     */
    public @NotNull UUID getUUID() {
        return uuid;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
