package de.hdg.keklist.database;

import de.hdg.keklist.Keklist;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class DB {

    private Connection connection;
    private final DBType type;
    private final Keklist plugin;

    public DB(DBType dbType, Keklist plugin) {
        this.plugin = plugin;
        type = dbType;
    }

    public void connect() {
        try {
            if(type.equals(DBType.SQLITE)){
                File file = new File(Keklist.getInstance().getDataFolder(), "database.db");
                if (!file.exists())
                    file.createNewFile();

                String url = "jdbc:sqlite:" + file.getPath();
                connection = DriverManager.getConnection(url);

                createTables();
            }

            if(type.equals(DBType.MARIADB)){
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

                createTables();
            }

        } catch (SQLException | java.io.IOException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("Could not find the MariaDB driver!");
            throw new RuntimeException(e);
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void onUpdate(@NotNull final String statement, Object... preparedArgs) {
        if(isConnected()) {
            new FutureTask(new Runnable() {
                PreparedStatement preparedStatement;

                public void run() {
                    try {
                        this.preparedStatement = connection.prepareStatement(statement);

                        for(int i = 0; i < preparedArgs.length; i++) {
                            this.preparedStatement.setObject(i+1, preparedArgs[i]);
                        }

                        this.preparedStatement.executeUpdate();
                        this.preparedStatement.close();
                    } catch (SQLException throwable) {
                        throwable.printStackTrace();
                    }
                }
            }, null).run();
        }else {
            connect();
            onUpdate(statement, preparedArgs);
        }
    }

    @Nullable
    public ResultSet onQuery(@NotNull final String query, Object... preparedArgs) {
        if (isConnected()) {
            try {
                FutureTask<ResultSet> task = new FutureTask<>(new Callable<ResultSet>() {
                    PreparedStatement ps;

                    public ResultSet call() throws Exception {
                        this.ps = connection.prepareStatement(query);

                        for(int i = 0; i < preparedArgs.length; i++) {
                            this.ps.setObject(i+1, preparedArgs[i]);
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

    private void createTables(){
        onUpdate("CREATE TABLE IF NOT EXISTS whitelist (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16), byPlayer VARCHAR(16), unix BIGINT(13))");
        onUpdate("CREATE TABLE IF NOT EXISTS whitelistIp (ip VARCHAR(39) PRIMARY KEY, byPlayer VARCHAR(16), unix BIGINT(13))");

        onUpdate("CREATE TABLE IF NOT EXISTS blacklist (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16), byPlayer VARCHAR(16), unix BIGINT(13), reason VARCHAR(1500) DEFAULT 'No reason given')");
        onUpdate("CREATE TABLE IF NOT EXISTS blacklistIp (ip VARCHAR(39) PRIMARY KEY, byPlayer VARCHAR(16), unix BIGINT(13), reason VARCHAR(1500) DEFAULT 'No reason given')");
        onUpdate("CREATE TABLE IF NOT EXISTS blacklistMotd (ip VARCHAR(39) PRIMARY KEY, byPlayer VARCHAR(16), unix BIGINT(13))");
    }

    public enum DBType {
        MARIADB, SQLITE
    }
}
