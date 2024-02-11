package de.hdg.keklist.api.events.whitelist;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a hostname is removed from the whitelist.
 */
public class DomainRemovedFromWhitelistEvent extends Event {


    private static final HandlerList handlerList = new HandlerList();
    private final String hostname;

    public DomainRemovedFromWhitelistEvent(@NotNull String hostname) {
        this.hostname = hostname;
    }

    /**
     * Returns the hostname removed from the whitelist.
     *
     * @return The hostname removed from the whitelist
     */
    public @NotNull String getHostname() {
        return hostname;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
