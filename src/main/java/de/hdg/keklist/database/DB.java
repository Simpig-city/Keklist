package de.hdg.keklist.database;

import de.hdg.keklist.Keklist;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class DB {

    private static Connection connection;
    private static DBType type;

    public DB(DBType dbType) {
      type = dbType;
    }

    public void connect() {
        connection = null;
        try {
            File file = new File(Keklist.getInstance().getDataFolder(), "database.db");
            if (!file.exists())
                file.createNewFile();

            String url = "jdbc:sqlite:" + file.getPath();
            connection = DriverManager.getConnection(url);

            createTables();
        } catch (SQLException | java.io.IOException ex) {
            ex.printStackTrace();
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
    public void onUpdate(final String statement, Object... preparedArgs) {
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
            }, 1).run();
        }else {
            connect();
            onUpdate(statement, preparedArgs);
        }
    }

    @Nullable
    public ResultSet onQuery(final String query, Object... preparedArgs) {
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
        onUpdate("CREATE TABLE IF NOT EXISTS whitelist (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16), by VARCHAR(16), unix INTEGER DEFAULT " + System.currentTimeMillis() + ")");
        onUpdate("CREATE TABLE IF NOT EXISTS whitelistIp (ip VARCHAR PRIMARY KEY, by VARCHAR(16), unix INTEGER DEFAULT " + System.currentTimeMillis() + ")");

        onUpdate("CREATE TABLE IF NOT EXISTS blacklist (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16), by VARCHAR(16), unix INTEGER DEFAULT " + System.currentTimeMillis() + ", reason VARCHAR DEFAULT 'No reason given')");
        onUpdate("CREATE TABLE IF NOT EXISTS blacklistIp (ip VARCHAR PRIMARY KEY, by VARCHAR(16), unix INTEGER DEFAULT " + System.currentTimeMillis() + ", reason VARCHAR DEFAULT 'No reason given')");
        onUpdate("CREATE TABLE IF NOT EXISTS blacklistMotd (ip VARCHAR PRIMARY KEY, by VARCHAR(16), unix INTEGER DEFAULT " + System.currentTimeMillis() + ")");
    }

    public enum DBType {
        MARIADB, SQLITE
    }
}
