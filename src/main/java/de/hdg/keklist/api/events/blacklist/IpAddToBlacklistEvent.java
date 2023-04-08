package de.hdg.keklist.api.events.blacklist;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This event is called when a IP is added to the blacklist.
 */
public class IpAddToBlacklistEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();
    private final String ip;
    private final String reason;

    public IpAddToBlacklistEvent(@NotNull String ip, @Nullable String reason) {
        this.ip = ip;
        this.reason = reason;
    }

    /**
     * Returns the IP that was added to the blacklist.
     * This can be IPv4 or IPv6.
     *
     * @return The IP that was added to the blacklist.
     */
    public String getIp() {
        return ip;
    }

    /**
     * Returns the reason why the IP was added to the blacklist.
     *
     * @return The reason why the IP was added to the blacklist or null if no reason was given.
     */
    public String getReason() {
        return reason;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
