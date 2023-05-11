package de.hdg.keklist.api.events.whitelist;

import de.hdg.keklist.api.KeklistAPI;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a player is removed from the whitelist.
 * <b>Only called when removed by Plugin itself. This can be a floodgate name {@link KeklistAPI#getFloodgatePrefix()}</b>
 */
public class PlayerRemovedFromWhitelistEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final String playerName;

    public PlayerRemovedFromWhitelistEvent(@NotNull String playerName) {
        this.playerName = playerName;

        Bukkit.getOfflinePlayer(playerName).setWhitelisted(false);
    }

    /**
     * Get the name of the player which was removed from the whitelist.
     *
     * @return The name of the player removed from the whitelist.
     */
    public @NotNull String getPlayerName() {
        return playerName;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
