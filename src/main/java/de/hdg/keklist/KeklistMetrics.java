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

    public void shutdown(){
        metrics.shutdown();
    }

    private void initChars(){
        metrics.addCustomChart(new SimplePie("keklist_language", () -> {
            return Keklist.getLanguage().getLanguageCode();
        }));

        metrics.addCustomChart(new SingleLineChart("keklist_whitelisted", () -> {
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM whitelist");
            try {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (Exception ignored) {}
            return 0;
        }));

        metrics.addCustomChart(new SingleLineChart("keklist_whitelistedIPs", () -> {
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM whitelistIp");
            try {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (Exception ignored) {}
            return 0;
        }));

        metrics.addCustomChart(new SingleLineChart("keklist_blacklisted", () -> {
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM blacklist");
            try {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (Exception ignored) {}
            return 0;
        }));

        metrics.addCustomChart(new SingleLineChart("keklist_blacklistedIPs", () -> {
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM blacklistIp");
            try {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (Exception ignored) {}
            return 0;
        }));

        metrics.addCustomChart(new SingleLineChart("keklist_blacklistedMOTD", () -> {
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM blacklistMotd");
            try {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (Exception ignored) {}
            return 0;
        }));

        plugin.getLogger().info(Keklist.getLanguage().get("bstats.done"));
    }
}
