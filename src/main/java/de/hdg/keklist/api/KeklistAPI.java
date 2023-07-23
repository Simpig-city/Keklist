package de.hdg.keklist.api;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.api.events.blacklist.*;
import de.hdg.keklist.api.events.whitelist.IpAddToWhitelistEvent;
import de.hdg.keklist.api.events.whitelist.IpRemovedFromWhitelistEvent;
import de.hdg.keklist.api.events.whitelist.UUIDAddToWhitelistEvent;
import de.hdg.keklist.api.events.whitelist.UUIDRemovedFromWhitelistEvent;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.extentions.WebhookManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * API for the Keklist plugin
 *
 * @author SageSphinx63920
 * @since 1.0
 */
public class KeklistAPI {

    private final Keklist plugin;
    private final String API_INFO = "ADDED_BY_API";

    /**
     * Creates the keklist api instance
     * <p>
     * Only used internally
     *
     * @param plugin Main plugin instance
     */
    private KeklistAPI(Keklist plugin) {
        this.plugin = plugin;
    }

    /**
     * <b>DO NOT CALL THIS METHOD</b>
     * <b>IT IS ONLY FOR INTERNAL USE</b>
     * <b>USE {@link Keklist#getApi()} INSTEAD</b>
     * <p>
     * Creates a new KeklistAPI instance
     * </p>
     *
     * @param plugin Keklist instance
     * @return KeklistAPI instance
     */
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull KeklistAPI makeApi(Keklist plugin) {
        return new KeklistAPI(plugin);
    }


    /* Getter form Database */

    /**
     * Returns whenever if the player is blacklisted
     * <p>
     * This method is a shortcut for {@link #isBlacklisted(UUID)}
     * </p>
     *
     * @param player Player to check
     * @return true if the player is blacklisted
     */
    public boolean isBlacklisted(@NotNull Player player) {
        return isBlacklisted(player.getUniqueId());
    }

    /**
     * Returns whenever if the uuid is blacklisted
     *
     * @param uuid UUID to check
     * @return true if the uuid is blacklisted
     */
    public boolean isBlacklisted(@NotNull UUID uuid) {
        try {
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE uuid = ?", uuid.toString());
            return rs.next();
        } catch (SQLException e) {
            Keklist.getInstance().getLogger().severe("Database is not available");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns whenever if the ip is blacklisted
     * <b>Only IPv4 and IPv6 are allowed here</b>
     *
     * @param ip IP to check
     * @return true if the ip is blacklisted
     * @throws IllegalArgumentException if the ip is not valid
     */
    public boolean isBlacklisted(@NotNull String ip) {
        if (ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") || ip.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"))
            throw new IllegalArgumentException("IP is not valid");

        try {
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", ip);
            return rs.next();
        } catch (SQLException e) {
            Keklist.getInstance().getLogger().severe("Database is not available");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns whenever if the ip is blacklisted on the MOTD blacklist
     * <b>Only IPv4 and IPv6 are allowed here</b>
     *
     * @param ip IP to check
     * @return true if the ip is blacklisted
     * @throws IllegalArgumentException if the ip is not valid
     */
    public boolean isMOTDBlacklisted(@NotNull String ip) {
        if (ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") || ip.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"))
            throw new IllegalArgumentException("IP is not valid");

        try {
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", ip);
            return rs.next();
        } catch (SQLException e) {
            Keklist.getInstance().getLogger().severe("Database is not available");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns whenever if the player is whitelisted
     * <p>
     * This method is a shortcut for {@link #isWhitelisted(UUID)}
     * </p>
     *
     * @param player Player to check
     * @return true if the player is whitelisted
     */
    public boolean isWhitelisted(@NotNull Player player) {
        return isWhitelisted(player.getUniqueId());
    }

    /**
     * Returns whenever if the uuid is whitelisted
     *
     * @param uuid UUID to check
     * @return true if the uuid is whitelisted
     */
    public boolean isWhitelisted(@NotNull UUID uuid) {
        try {
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", uuid.toString());
            return rs.next();
        } catch (SQLException e) {
            Keklist.getInstance().getLogger().severe("Database is not available");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns whenever if the ip is whitelisted
     * <b>Only IPv4 and IPv6 are allowed here</b>
     *
     * @param ip IP to check
     * @return true if the ip is whitelisted
     * @throws IllegalArgumentException if the ip is not valid
     */
    public boolean isWhitelisted(@NotNull String ip) {
        if (ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") || ip.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"))
            throw new IllegalArgumentException("IP is not valid");

        try (ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp WHERE ip = ?", ip)) {
            return rs.next();
        } catch (SQLException e) {
            Keklist.getInstance().getLogger().severe("Database is not available");
            e.printStackTrace();
            return false;
        }
    }


    /* Setter for Database */

    /**
     * Whitelists a player, if not already whitelisted
     *
     * @param player Player to whitelist
     */
    public void whitelist(@NotNull Player player) {
        whitelist(player.getUniqueId(), player.getName());
    }

    /**
     * Whitelists an uuid, if not already whitelisted
     *
     * @param uuid       UUID to whitelist
     * @param playerName Name of the player, if available
     */
    public void whitelist(@NotNull UUID uuid, @Nullable String playerName) {
        if (isWhitelisted(uuid)) return;
        awaitSync(() -> new UUIDAddToWhitelistEvent(uuid).callEvent());

        Keklist.getDatabase().onUpdate("INSERT INTO whitelist (uuid, name, byPlayer, unix) VALUES (?, ?, ?, ?)", uuid.toString(), playerName == null ? API_INFO : playerName, API_INFO, System.currentTimeMillis());

        if (Keklist.getWebhookManager() != null)
            Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_ADD, playerName == null ? uuid.toString() : playerName, "API", System.currentTimeMillis());
    }

    /**
     * Whitelists an ip, if not already whitelisted
     * <b>Only IPv4 and IPv6 are allowed here</b>
     *
     * @param ip IP to whitelist
     * @throws IllegalArgumentException if the ip is not valid
     */
    public void whitelist(@NotNull String ip) {
        if (ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") || ip.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"))
            throw new IllegalArgumentException("IP is not valid");
        if (isWhitelisted(ip)) return;
        awaitSync(() -> new IpAddToWhitelistEvent(ip).callEvent());

        Keklist.getDatabase().onUpdate("INSERT INTO whitelistIp (ip, byPlayer, unix) VALUES (?, ?, ?)", ip, API_INFO, System.currentTimeMillis());

        if (Keklist.getWebhookManager() != null)
            Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_ADD, ip, "API", System.currentTimeMillis());
    }

    /**
     * Blacklists a player with a reason, if not already blacklisted
     *
     * @param player The player to blacklist
     * @param reason The reason for the blacklist
     */
    public void blacklist(@NotNull Player player, @Nullable String reason) {
        blacklist(player.getUniqueId(), player.getName(), reason);
    }

    /**
     * Adds an uuid to the blacklist with a reason, if not already blacklisted
     *
     * @param uuid       The uuid to blacklist
     * @param playerName The name of the player to blacklist, if available
     * @param reason     The reason for the blacklist
     */
    public void blacklist(@NotNull UUID uuid, @Nullable String playerName, @Nullable String reason) {
        if (isBlacklisted(uuid)) return;

        String webhookEntry = playerName == null ? uuid.toString() : playerName;
        if (reason == null) {
            awaitSync(() -> new UUIDAddToBlacklistEvent(uuid, null).callEvent());
            Keklist.getDatabase().onUpdate("INSERT INTO blacklist (uuid, name, byPlayer, unix, reason) VALUES (?, ?, ?, ?, ?)", uuid.toString(), playerName == null ? API_INFO : playerName, API_INFO, System.currentTimeMillis(), reason);

            if (Keklist.getWebhookManager() != null)
                Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_ADD, webhookEntry, "API", null, System.currentTimeMillis());
        } else {
            if (reason.length() <= 1500) {
                awaitSync(() -> new UUIDAddToBlacklistEvent(uuid, reason).callEvent());
                Keklist.getDatabase().onUpdate("INSERT INTO blacklist (uuid, name, byPlayer, unix) VALUES (?, ?, ?, ?)", uuid.toString(), playerName == null ? API_INFO : playerName, API_INFO, System.currentTimeMillis());

                if (Keklist.getWebhookManager() != null)
                    Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_ADD, webhookEntry, "API", reason, System.currentTimeMillis());
            } else {
                throw new IllegalArgumentException("Reason is too long! (Max. 1500 characters)");
            }
        }
    }

    /**
     * Adds an ip to the blacklist with a reason, if not already blacklisted
     * <b>Only IPv4 and IPv6 are allowed here</b>
     *
     * @param ip     The ip to blacklist
     * @param reason The reason for the blacklist
     * @throws IllegalArgumentException if the ip is not valid
     */
    public void blacklist(@NotNull String ip, @Nullable String reason) {
        if (ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") || ip.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"))
            throw new IllegalArgumentException("IP is not valid");
        if (isBlacklisted(ip)) return;

        if (reason == null) {
            awaitSync(() -> new IpAddToBlacklistEvent(ip, null).callEvent());
            Keklist.getDatabase().onUpdate("INSERT INTO blacklistIp (ip, byPlayer, unix) VALUES (?, ?, ?)", ip, API_INFO, System.currentTimeMillis());

            if (Keklist.getWebhookManager() != null)
                Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_ADD, ip, "API", null, System.currentTimeMillis());
        } else {
            if (reason.length() <= 1500) {
                awaitSync(() -> new IpAddToBlacklistEvent(ip, reason).callEvent());
                Keklist.getDatabase().onUpdate("INSERT INTO blacklistIp (ip, byPlayer, unix, reason) VALUES (?, ?, ?, ?)", ip, API_INFO, System.currentTimeMillis(), reason);

                if (Keklist.getWebhookManager() != null)
                    Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_ADD, ip, "API", reason, System.currentTimeMillis());
            } else {
                throw new IllegalArgumentException("Reason is too long! (Max. 1500 characters)");
            }
        }
    }

    /**
     * Blacklists an IP for the MOTD, if not already blacklisted
     * <b>Only IPv4 and IPv6 are allowed</b>
     * <p>
     * Throws an {@link IllegalArgumentException} if the IP is not valid.
     * </p>
     *
     * @param ip The IPv4 or IPv6 to blacklist
     */
    public void blacklistMOTD(@NotNull String ip) {
        if (isMOTDBlacklisted(ip)) return;
        awaitSync(() -> new IpAddToMOTDBlacklistEvent(ip).callEvent());

        Keklist.getDatabase().onUpdate("INSERT INTO blacklistMotd (ip, byPlayer, unix) VALUES (?, ?, ?)", ip, API_INFO, System.currentTimeMillis());
    }

    /**
     * Removes a player from the whitelist, if whitelisted
     *
     * @param player Player to remove from whitelist
     */
    public void removeWhitelist(@NotNull Player player) {
        removeWhitelist(player.getUniqueId());
    }

    /**
     * Removes an uuid from the whitelist, if whitelisted
     *
     * @param uuid UUID to remove from whitelist
     */
    public void removeWhitelist(@NotNull UUID uuid) {
        if (!isWhitelisted(uuid)) return;
        awaitSync(() -> new UUIDRemovedFromWhitelistEvent(uuid).callEvent());

        Keklist.getDatabase().onUpdate("DELETE FROM whitelist WHERE uuid = ?", uuid.toString());

        if (Keklist.getWebhookManager() != null)
            Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_REMOVE, uuid.toString(), "API", System.currentTimeMillis());
    }

    /**
     * Removes an ip from the whitelist, if whitelisted
     * <b>Only IPv4 and IPv6 are allowed here</b>
     * <p>
     * Throws an {@link IllegalArgumentException} if the IP is not valid.
     * </p>
     *
     * @param ip IP to remove from whitelist
     */
    public void removeWhitelist(@NotNull String ip) {
        if (!isWhitelisted(ip)) return;
        awaitSync(() -> new IpRemovedFromWhitelistEvent(ip).callEvent());

        Keklist.getDatabase().onUpdate("DELETE FROM whitelistIp WHERE ip = ?", ip);

        if (Keklist.getWebhookManager() != null)
            Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_REMOVE, ip, "API", System.currentTimeMillis());
    }

    /**
     * Removes a player from the blacklist, if blacklisted
     *
     * @param player Player to remove from blacklist
     */
    public void removeBlacklist(@NotNull Player player) {
        removeBlacklist(player.getUniqueId());
    }

    /**
     * Removes an uuid from the blacklist, if blacklisted
     *
     * @param uuid UUID to remove from blacklist
     */
    public void removeBlacklist(@NotNull UUID uuid) {
        if (!isBlacklisted(uuid)) return;
        awaitSync(() -> new UUIDRemovedFromBlacklistEvent(uuid).callEvent());

        Keklist.getDatabase().onUpdate("DELETE FROM blacklist WHERE uuid = ?", uuid.toString());

        if (Keklist.getWebhookManager() != null)
            Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_REMOVE, uuid.toString(), "API", null, System.currentTimeMillis());
    }

    /**
     * Removes an ip from the blacklist, if blacklisted
     * <b>Only IPv4 and IPv6 are allowed here</b>
     * <p>
     * Throws an {@link IllegalArgumentException} if the IP is not valid.
     * </p>
     *
     * @param ip IP to remove from blacklist
     */
    public void removeBlacklist(@NotNull String ip) {
        if (!isBlacklisted(ip)) return;
        awaitSync(() -> new IpRemovedFromBlacklistEvent(ip).callEvent());

        Keklist.getDatabase().onUpdate("DELETE FROM blacklistIp WHERE ip = ?", ip);

        if (Keklist.getWebhookManager() != null)
            Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_REMOVE, ip, "API", null, System.currentTimeMillis());
    }

    /**
     * Removes an ip from the blacklist for the MOTD, if blacklisted
     * <b>Only IPv4 and IPv6 are allowed here</b>
     * <p>
     * Throws an {@link IllegalArgumentException} if the IP is not valid.
     * </p>
     *
     * @param ip IP to remove from blacklist
     */
    public void removeBlacklistMOTD(@NotNull String ip) {
        if (!isMOTDBlacklisted(ip)) return;
        awaitSync(() -> new IpRemovedFromMOTDBlacklistEvent(ip).callEvent());

        Keklist.getDatabase().onUpdate("DELETE FROM blacklistMotd WHERE ip = ?", ip);
    }


    /* Config values */

    /**
     * Return whenever the whitelist is enabled
     *
     * @return true if enabled
     */
    public boolean isWhitelistEnabled() {
        return plugin.getConfig().getBoolean("whitelist.enabled");
    }

    /**
     * Return whenever the blacklist is enabled
     *
     * @return true if enabled
     */
    public boolean isBlacklistEnabled() {
        return plugin.getConfig().getBoolean("blacklist.enabled");
    }

    /**
     * Returns the Prefix for Floodgate Players set in the config
     *
     * @return The set player prefix
     */
    @NotNull
    public String getFloodgatePrefix() {
        return Objects.requireNonNull(plugin.getConfig().getString("floodgate.prefix"));
    }

    /**
     * Return whenever mariadb is enabled
     *
     * @return true if enabled
     */
    public boolean isMariaDB() {
        return plugin.getConfig().getBoolean("mariadb.enabled");
    }

    /**
     * Returns the Database Type as {@link DB.DBType}
     *
     * @return The Database Type
     */
    public DB.DBType getDBType() {
        return isMariaDB() ? DB.DBType.MARIADB : DB.DBType.SQLITE;
    }


    /**
     *  Returns the language for the plugin set in the config
     *
     * @return the language code
     */
    @NotNull
    public String getLanguage() {
        return Keklist.getTranslations().getLanguageCode();
    }

    /**
     * This is used to execute a runnable on the main thread and await the result
     * <p>
     * Mainly used for the events to be async safe
     *
     * @param callable The runnable to execute in the main thread
     */
    private <T> void awaitSync(Callable<T> callable) {
        if (Bukkit.isPrimaryThread()) {
            try {
                callable.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Future<T> future = Bukkit.getScheduler().callSyncMethod(plugin, callable);
            try {
                //Await the result for maybe adding cancelable events in the future
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
