package de.hdg.keklist.api.events.blacklist;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when an IP is removed from the MOTD blacklist.
 */
public class IpRemovedFromMOTDBlacklistEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();
    private final String ip;

    public IpRemovedFromMOTDBlacklistEvent(@NotNull String ip) {
        this.ip = ip;
    }

    /**
     * Returns the IP that was removed from the MOTD blacklist.
     * This can be IPv4 or IPv6.
     *
     * @return The IP that was removed from the MOTD blacklist.
     */
    public @NotNull String getIp() {
        return ip;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
