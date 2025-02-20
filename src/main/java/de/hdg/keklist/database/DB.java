package de.hdg.keklist.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.hdg.keklist.Keklist;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.Arrays;

/**
 * Class for handling database connections.
 */
public class DB {

    @Getter
    private final DBType type;
    private final Keklist plugin;
    private HikariDataSource dataSource;
    private String sqliteJdbcUrl;

    /**
     * Constructor for the database connection.
     *
     * @param dbType The type of database to connect to.
     * @param plugin The plugin instance.
     */
    public DB(@NotNull DBType dbType, @NotNull Keklist plugin) {
        this.plugin = plugin;
        this.type = dbType;
    }

    /**
     * Connects to the database.
     */
    public void connect() {
        try {
            switch (type) {
                case SQLITE -> {
                    Class.forName("org.sqlite.JDBC");

                    File file = new File(plugin.getDataFolder(), "database.db");
                    // SQLite will create the file automatically if needed
                    sqliteJdbcUrl = "jdbc:sqlite:" + file.getAbsolutePath() + "?journal_mode=WAL&busy_timeout=5000"; // WAL mode for better performance
                }

                case H2 -> {
                    Class.forName("org.h2.Driver");
                    File file = new File(plugin.getDataFolder(), "database.h2db");

                    HikariConfig config = new HikariConfig();

                    config.setJdbcUrl("jdbc:h2:file:" + file.getAbsolutePath());
                    config.setDriverClassName("org.h2.Driver");
                    config.setConnectionTestQuery("SELECT 1");
                    config.setMaximumPoolSize(10);
                    config.setMinimumIdle(2);
                    config.setIdleTimeout(600000);
                    config.setMaxLifetime(1800000);
                    config.setConnectionTimeout(30000);

                    dataSource = new HikariDataSource(config);
                }

                case MARIADB -> {
                    Class.forName("org.mariadb.jdbc.Driver");
                    HikariConfig config = new HikariConfig();

                    String host = plugin.getConfig().getString("database.mariadb.host");
                    String port = plugin.getConfig().getString("database.mariadb.port");
                    String database = plugin.getConfig().getString("database.mariadb.database");
                    String username = plugin.getConfig().getString("database.mariadb.username");
                    String password = plugin.getConfig().getString("database.mariadb.password");
                    String options = plugin.getConfig().getString("database.mariadb.options");

                    String url = "jdbc:mariadb://" + host + ":" + port + "/" + database + options;
                    config.setJdbcUrl(url);
                    config.setDriverClassName("org.mariadb.jdbc.Driver");
                    config.setUsername(username);
                    config.setPassword(password);
                    config.setConnectionTestQuery("SELECT 1");
                    config.setMaximumPoolSize(25);
                    config.setMinimumIdle(5);

                    dataSource = new HikariDataSource(config);
                }
            }

            createTables();
            updateTables();
        } catch (Exception ex) {
            plugin.getSLF4JLogger().error(Keklist.getTranslations().get("database.connection-fail"), ex);
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    /**
     * Returns a QueryResult wrapper containing the ResultSet and underlying resources.
     * Caller must call close() on the returned QueryResult when done.
     *
     * @param query  The SQL query to execute.
     *               Use ? as a placeholder for parameters.
     *               Example: "SELECT * FROM whitelist WHERE uuid = ?"
     * @param params Optional parameters to bind to the query.
     * @return QueryResult wrapper containing the ResultSet and underlying resources.
     * @throws RuntimeException if an SQLException occurs.
     */
    @NotNull
    public QueryResult onQuery(@NotNull @Language("SQL") final String query, @Nullable Object... params) {
        if (!isConnected())
            connect();

        try {
            Connection connection = (dataSource != null)
                    ? dataSource.getConnection()
                    : DriverManager.getConnection(sqliteJdbcUrl);
            PreparedStatement ps = connection.prepareStatement(query);

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }

            return new QueryResult(connection, ps, ps.executeQuery());
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Executes an SQL update statement.
     */
    public void onUpdate(@NotNull @Language("SQL") final String statement, @Nullable Object... params) {
        if (!isConnected())
            connect();

        try (Connection connection = (dataSource != null)
                ? dataSource.getConnection()
                : DriverManager.getConnection(sqliteJdbcUrl);
             PreparedStatement ps = connection.prepareStatement(statement)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getSLF4JLogger().error(Keklist.getTranslations().get("database.error", statement, Arrays.toString(params)), ex);
        }
    }

    /**
     * Creates the necessary tables if they do not exist.
     */
    private void createTables() {
        onUpdate("CREATE TABLE IF NOT EXISTS whitelist (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16) UNIQUE, byPlayer VARCHAR(16), unix BIGINT)");
        onUpdate("CREATE TABLE IF NOT EXISTS whitelistIp (ip VARCHAR(39) PRIMARY KEY, byPlayer VARCHAR(16), unix BIGINT)");
        onUpdate("CREATE TABLE IF NOT EXISTS whitelistDomain (domain VARCHAR(253) PRIMARY KEY, byPlayer VARCHAR(16), unix BIGINT)");
        onUpdate("CREATE TABLE IF NOT EXISTS whitelistLevel (entry VARCHAR(253) PRIMARY KEY, whitelistLevel INTEGER NOT NULL DEFAULT 0, byPlayer VARCHAR(16) NOT NULL)");

        onUpdate("CREATE TABLE IF NOT EXISTS blacklist (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16) UNIQUE, byPlayer VARCHAR(16), unix BIGINT, reason VARCHAR(1500) DEFAULT 'No reason given')");
        onUpdate("CREATE TABLE IF NOT EXISTS blacklistIp (ip VARCHAR(39) PRIMARY KEY, byPlayer VARCHAR(16), unix BIGINT, reason VARCHAR(1500) DEFAULT 'No reason given')");
        onUpdate("CREATE TABLE IF NOT EXISTS blacklistMotd (ip VARCHAR(39) PRIMARY KEY, byPlayer VARCHAR(16), unix BIGINT)");

        onUpdate("CREATE TABLE IF NOT EXISTS lastSeen (uuid VARCHAR(36) PRIMARY KEY, ip VARCHAR(39) NOT NULL, protocolId INT NOT NULL DEFAULT -2, brand VARCHAR(1000) NOT NULL DEFAULT 'unknown', lastSeen BIGINT)");
        onUpdate("CREATE TABLE IF NOT EXISTS mfa (uuid VARCHAR(36) PRIMARY KEY, secret VARCHAR(1000), recoveryCodes VARCHAR(1000))");
    }

    /**
     * Updates the tables if necessary.
     */
    private void updateTables() {
        // Add ALTER TABLE statements here to update the database schema after release
    }

    /**
     * Checks if the database connection is active.
     *
     * @return True if the connection is active, false otherwise.
     */
    public boolean isConnected() {
        return (dataSource != null && !dataSource.isClosed()) || (sqliteJdbcUrl != null);
    }

    /**
     * Disconnects from the database.
     */
    public void disconnect() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    /**
     * Reconnects the database
     */
    public void reconnect() {
        disconnect();
        connect();
    }

    /**
     * Wrapper class that holds a ResultSet along with its underlying resources.
     * Use try-with-resources or call close() when finished processing.
     */
    public record QueryResult(@NotNull Connection connection, @NotNull PreparedStatement preparedStatement,
                               @Getter @NotNull ResultSet resultSet) implements AutoCloseable {

        @Override
        public void close() throws SQLException {
            if (!resultSet.isClosed())
                resultSet.close();

            if (!preparedStatement.isClosed())
                preparedStatement.close();

            if (!connection.isClosed())
                connection.close();
        }
    }

    /**
     * Enum for supported database types.
     */
    public enum DBType {
        MARIADB, SQLITE, H2
    }
}
