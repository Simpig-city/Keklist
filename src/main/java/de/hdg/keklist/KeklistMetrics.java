package de.hdg.keklist;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

import java.sql.ResultSet;

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
        metrics.addCustomChart(new SimplePie("keklist_language", () -> {
            return Keklist.getTranslations().getLanguageCode();
        }));

        metrics.addCustomChart(new SimplePie("keklist_whitelist", () -> {
            return plugin.getConfig().getBoolean("whitelist.enabled") ? "enabled" : "disabled";
        }));

        metrics.addCustomChart(new SimplePie("keklist_blacklist", () -> {
            return plugin.getConfig().getBoolean("blacklist.enabled") ? "enabled" : "disabled";
        }));

        metrics.addCustomChart(new SingleLineChart("keklist_whitelisted", () -> {
            ResultSet rsPlayers = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM whitelist");
            ResultSet rsIPs = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM whitelistIp");
            try {
                int count = 0;
                if (rsPlayers.next())
                    count += rsPlayers.getInt(1);

                if (rsIPs.next())
                    count += rsIPs.getInt(1);

                return count;
            } catch (Exception ignored) {}
            return 0;
        }));

        metrics.addCustomChart(new SingleLineChart("keklist_blacklisted", () -> {
            ResultSet rsPlayers = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM blacklist");
            ResultSet rsIPs = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM blacklistIp");
            try {
                int count = 0;
                if (rsPlayers.next())
                    count += rsPlayers.getInt(1);

                if (rsIPs.next())
                    count += rsIPs.getInt(1);

                return count;
            } catch (Exception ignored) {}
            return 0;
        }));

        plugin.getLogger().info(Keklist.getTranslations().get("bstats.done"));
    }
}
