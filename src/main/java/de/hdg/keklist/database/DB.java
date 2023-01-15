package de.hdg.keklist.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DB {
    private static Connection connection;

    public static void connect() {
        connection = null;
        //connect to sqlite database in ./keklist.db
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:keklist.db");
            init();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            //create an table called whitelist with 5 collums: uuid, name, by, ip, unix
            statement.execute("CREATE TABLE IF NOT EXISTS whitelist (uuid VARCHAR(36), name VARCHAR(16), by VARCHAR(16), ip VARCHAR(15), unix BIGINT)");
            //create an table called blacklist with 5 collums: uuid, name, by, ip, unix
            statement.execute("CREATE TABLE IF NOT EXISTS blacklist (uuid VARCHAR(36), name VARCHAR(16), by VARCHAR(16), ip VARCHAR(15), unix BIGINT)");
            //create an table called phrases with 1 column called phrase
            statement.execute("CREATE TABLE IF NOT EXISTS phrases (phrase VARCHAR(2550))");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection getDB() {
        return connection;
    }
}
