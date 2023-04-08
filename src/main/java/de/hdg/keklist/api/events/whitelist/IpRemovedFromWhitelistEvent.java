package de.hdg.keklist.api.events.whitelist;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a player is removed from the whitelist.
 */
public class IpRemovedFromWhitelistEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();
    private final String ip;

    public IpRemovedFromWhitelistEvent(@NotNull String ip) {
        this.ip = ip;
    }

    /**
     * Returns the IP removed from the whitelist.
     * This can be IPv4 or IPv6.
     *
     * @return The ip removed from the whitelist
     */
    public @NotNull String getIp() {
        return ip;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
