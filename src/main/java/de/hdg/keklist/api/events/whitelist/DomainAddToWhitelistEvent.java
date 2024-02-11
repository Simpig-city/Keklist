package de.hdg.keklist.api.events.whitelist;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a hostname is added to the whitelist.
 */
public class DomainAddToWhitelistEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();
    private final String hostname;

    public DomainAddToWhitelistEvent(@NotNull String hostname) {
        this.hostname = hostname;
    }

    /**
     * Returns the hostname added to the whitelist.
     *
     * @return The hostname added
     */
    public @NotNull String getHostname() {
        return hostname;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
