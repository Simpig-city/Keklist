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

        metrics.addCustomChart(new SimplePie("keklist_whitelist", () -> {
            return plugin.getConfig().getBoolean("whitelist.enabled") ? "enabled" : "disabled";
        }));

        metrics.addCustomChart(new SimplePie("keklist_blacklist", () -> {
            return plugin.getConfig().getBoolean("blacklist.enabled") ? "enabled" : "disabled";
        }));

        metrics.addCustomChart(new SimplePie(" keklist_motd_blacklist", () -> {
            return plugin.getConfig().getBoolean("blacklist.enabled") ? "enabled" : "disabled";
        }));

        metrics.addCustomChart(new SingleLineChart("keklist_whitelisted", () -> {
            ResultSet rsPlayers = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM whitelist");
            ResultSet rsIPs = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM whitelistIp");
            try {
                if (rsPlayers.next() && rsIPs.next()) {
                    return rsPlayers.getInt(1) + rsIPs.getInt(1);
                }
            } catch (Exception ignored) {}
            return 0;
        }));

        metrics.addCustomChart(new SingleLineChart("keklist_blacklisted", () -> {
            ResultSet rsPlayers = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM blacklist");
            ResultSet rsIPs = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM blacklistIp");
            try {
                if (rsPlayers.next() && rsIPs.next()) {
                    return rsPlayers.getInt(1) + rsIPs.getInt(1);
                }
            } catch (Exception ignored) {}
            return 0;
        }));

        plugin.getLogger().info(Keklist.getLanguage().get("bstats.done"));
    }
}
