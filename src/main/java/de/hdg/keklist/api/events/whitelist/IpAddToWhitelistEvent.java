package de.hdg.keklist.api.events.whitelist;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a player is added to the whitelist.
 */
public class IpAddToWhitelistEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();
    private final String ip;

    public IpAddToWhitelistEvent(@NotNull String ip) {
        this.ip = ip;
    }

    /**
     * Returns the IP added to the whitelist.
     * This can be IPv4 or IPv6.
     *
     * @return The ip added
     */
    public @NotNull String getIp() {
        return ip;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
