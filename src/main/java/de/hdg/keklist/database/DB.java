package de.hdg.keklist.database;

import de.hdg.keklist.Keklist;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DB {

    private Connection connection;
    private final @Getter DBType type;
    private final Keklist plugin;
    private final AtomicInteger count = new AtomicInteger(0);

    public DB(DBType dbType, Keklist plugin) {
        this.plugin = plugin;
        type = dbType;
    }

    public void connect() {
        if (count.get() >= 4) {
            plugin.getLogger().severe(Keklist.getTranslations().get("database.connect-fail"));
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        try {
            switch (type) {
                case SQLITE -> {
                    File file = new File(Keklist.getInstance().getDataFolder(), "database.db");
                    if (!file.exists())
                        file.createNewFile();

                    String url = "jdbc:sqlite:" + file.getPath();
                    connection = DriverManager.getConnection(url);
                }

                case MARIADB -> {
                    Class.forName("org.mariadb.jdbc.Driver");

                    String url = "jdbc:mariadb://";

                    String host = plugin.getConfig().getString("mariadb.host");
                    String port = plugin.getConfig().getString("mariadb.port");
                    String database = plugin.getConfig().getString("mariadb.database");
                    String username = plugin.getConfig().getString("mariadb.username");
                    String password = plugin.getConfig().getString("mariadb.password");
                    String options = plugin.getConfig().getString("mariadb.options");

                    url += host + ":" + port + "/" + database + options;
                    connection = DriverManager.getConnection(url, username, password);
                }
            }

            createTables();
            count.incrementAndGet();
        } catch (SQLException | IOException ex) {
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(plugin);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe(Keklist.getTranslations().get("database.driver-missing"));
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    public boolean isConnected() {
        return (connection != null);
    }

    public void disconnect() {
        try {
            if (connection != null)
                connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onUpdate(@NotNull @Language("SQL") final String statement, Object... preparedArgs) {
        if (isConnected()) {
            new FutureTask(new Runnable() {
                PreparedStatement preparedStatement;

                public void run() {
                    try {
                        this.preparedStatement = connection.prepareStatement(statement);

                        for (int i = 0; i < preparedArgs.length; i++) {
                            this.preparedStatement.setObject(i + 1, preparedArgs[i]);
                        }

                        this.preparedStatement.executeUpdate();
                        this.preparedStatement.close();
                    } catch (SQLException throwable) {
                        throwable.printStackTrace();
                    }
                }
            }, null).run();
        } else {
            connect();
            onUpdate(statement, preparedArgs);
        }
    }

    @NotNull
    public CompletableFuture<@Nullable ResultSet> onQueryAsync(@NotNull @Language("SQL") final String query, @Nullable Object... preparedArgs) {
        if (!isConnected()) {
            return CompletableFuture.failedFuture(new SQLException("Not connected"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(query)) {

                if (preparedArgs != null) {
                    for (int i = 0; i < preparedArgs.length; i++) {
                        ps.setObject(i + 1, preparedArgs[i]);
                    }
                }

                return ps.executeQuery();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }).exceptionally(throwable -> {
            Keklist.getInstance().getSLF4JLogger().error("Failed to execute update asynchronously", throwable);

            return null;
        }).whenComplete((resultSet, throwable) -> {
            if (throwable != null) {
                Keklist.getInstance().getSLF4JLogger().error("Failed to execute query asynchronously", throwable); // Keep in english for us to debug easier as this should not be seen by any user anyway
            }

            try {
                resultSet.close();
            } catch (SQLException e) {
                Keklist.getInstance().getSLF4JLogger().error("Failed to close result set", e);

            }
        }).orTimeout(15, TimeUnit.SECONDS);
    }

    @Nullable
    public ResultSet onQuery(@NotNull @Language("SQL") final String query, Object... preparedArgs) {
        if (isConnected()) {
            try {
                FutureTask<ResultSet> task = new FutureTask<>(new Callable<>() {
                    PreparedStatement ps;

                    public ResultSet call() throws Exception {
                        this.ps = connection.prepareStatement(query);

                        for (int i = 0; i < preparedArgs.length; i++) {
                            this.ps.setObject(i + 1, preparedArgs[i]);
                        }

                        return this.ps.executeQuery();
                    }
                });

                task.run();
                return task.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            connect();
            return onQuery(query);
        }
        return null;
    }

    public void reconnect() {
        disconnect();
        connect();
    }

    private void createTables() {
        onUpdate("CREATE TABLE IF NOT EXISTS whitelist (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16) UNIQUE, byPlayer VARCHAR(16), unix BIGINT(13))");
        onUpdate("CREATE TABLE IF NOT EXISTS whitelistIp (ip VARCHAR(39) PRIMARY KEY, byPlayer VARCHAR(16), unix BIGINT(13))");
        onUpdate("CREATE TABLE IF NOT EXISTS whitelistDomain (domain VARCHAR(253) PRIMARY KEY, byPlayer VARCHAR(16), unix BIGINT(13))");
        onUpdate("CREATE TABLE IF NOT EXISTS whitelistLevel (entry VARCHAR(253) PRIMARY KEY, whitelistLevel INTEGER NOT NULL DEFAULT 0, byPlayer VARCHAR(16) NOT NULL)");

        onUpdate("CREATE TABLE IF NOT EXISTS blacklist (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16) UNIQUE, byPlayer VARCHAR(16), unix BIGINT(13), reason VARCHAR(1500) DEFAULT 'No reason given')");
        onUpdate("CREATE TABLE IF NOT EXISTS blacklistIp (ip VARCHAR(39) PRIMARY KEY, byPlayer VARCHAR(16), unix BIGINT(13), reason VARCHAR(1500) DEFAULT 'No reason given')");
        onUpdate("CREATE TABLE IF NOT EXISTS blacklistMotd (ip VARCHAR(39) PRIMARY KEY, byPlayer VARCHAR(16), unix BIGINT(13))");

        onUpdate("CREATE TABLE IF NOT EXISTS lastSeen (uuid VARCHAR(36) PRIMARY KEY, ip VARCHAR(39) NOT NULL, protocolId INT(5) NOT NULL DEFAULT -2, brand VARCHAR(1000) NOT NULL DEFAULT 'unknown', lastSeen BIGINT(13))");
        onUpdate("CREATE TABLE IF NOT EXISTS mfa (uuid VARCHAR(36) PRIMARY KEY, secret VARCHAR(1000), recoveryCodes VARCHAR(1000))");
    }

    /**
     * Database types supported by the plugin
     */
    public enum DBType {
        MARIADB, SQLITE
    }
}
