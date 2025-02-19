package de.hdg.keklist.extentions;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.database.DB;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIExtension extends PlaceholderExpansion {
    private final Keklist plugin;

    public PlaceholderAPIExtension(@NotNull Keklist plugin) {
        this.plugin = plugin;
        plugin.getLogger().info(Keklist.getTranslations().get("placeholder.registered"));
    }

    @Override
    public @NotNull String getIdentifier() {
        return plugin.getPluginMeta().getName().toLowerCase();
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        switch (identifier) {
            case "version" -> {
                return plugin.getPluginMeta().getVersion();
            }

            case "whitelisted" -> {
                try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", player.getUniqueId().toString())
                ) {
                    return String.valueOf(rs.getResultSet().next());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            case "blacklisted" -> {
                try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE uuid = ?", player.getUniqueId().toString())
                ) {
                    return String.valueOf(rs.getResultSet().next());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return null;
    }
}
