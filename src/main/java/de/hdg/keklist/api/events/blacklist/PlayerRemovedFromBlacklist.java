package de.hdg.keklist.api.events.blacklist;

import de.hdg.keklist.api.KeklistAPI;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a player is removed from the blacklist.
 * <b>Only called when removed by Plugin itself. This can be a floodgate name {@link KeklistAPI#getFloodgatePrefix()}</b>
 */
public class PlayerRemovedFromBlacklist extends Event {
    private static final HandlerList handlerList = new HandlerList();
    private final String playerName;

    public PlayerRemovedFromBlacklist(@NotNull String playerName) {
        this.playerName = playerName;
    }

    /**
     * Returns the name of the player that was removed from the blacklist.
     *
     * @return The name of the player that was removed from the blacklist.
     */
    public @NotNull String getPlayerName() {
        return playerName;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
