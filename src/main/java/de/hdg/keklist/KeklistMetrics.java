package de.hdg.keklist;

import de.hdg.keklist.database.DB;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

public class KeklistMetrics {

    private final Metrics metrics;
    private final Keklist plugin;

    public KeklistMetrics(Metrics metrics, Keklist plugin) {
        this.metrics = metrics;
        this.plugin = plugin;

        initChars();
    }

    public void shutdown() {
        metrics.shutdown();
    }

    private void initChars() {
        metrics.addCustomChart(new SimplePie("keklist_language", () -> Keklist.getTranslations().getLanguageCode()));

        metrics.addCustomChart(new SimplePie("keklist_whitelist", () -> plugin.getConfig().getBoolean("whitelist.enabled") ? "enabled" : "disabled"));

        metrics.addCustomChart(new SimplePie("keklist_blacklist", () -> plugin.getConfig().getBoolean("blacklist.enabled") ? "enabled" : "disabled"));

        metrics.addCustomChart(new SingleLineChart("keklist_whitelisted", () -> {
            try (DB.QueryResult rsPlayers = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM whitelist");
                 DB.QueryResult rsIPs = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM whitelistIp")
            ) {
                int count = 0;
                if (rsPlayers.getResultSet().next())
                    count += rsPlayers.getResultSet().getInt(1);

                if (rsIPs.getResultSet().next())
                    count += rsIPs.getResultSet().getInt(1);

                return count;
            } catch (Exception ignored) {
            }
            return 0;
        }));

        metrics.addCustomChart(new SingleLineChart("keklist_blacklisted", () -> {
            try (DB.QueryResult rsPlayers = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM blacklist");
                 DB.QueryResult rsIPs = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM blacklistIp")
            ) {
                int count = 0;
                if (rsPlayers.getResultSet().next())
                    count += rsPlayers.getResultSet().getInt(1);

                if (rsIPs.getResultSet().next())
                    count += rsIPs.getResultSet().getInt(1);

                return count;
            } catch (Exception ignored) {
            }
            return 0;
        }));

        plugin.getLogger().info(Keklist.getTranslations().get("bstats.done"));
    }
}
