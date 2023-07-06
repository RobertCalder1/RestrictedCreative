package eextr0.restrictedcreative.storage;

import eextr0.restrictedcreative.RestrictedCreative;
import org.bukkit.Bukkit;

import java.sql.*;

public class Database {
    private RestrictedCreative main;

    private String host, name, user, pass, type, table_blocks, table_invs;
    private int port;
    private boolean ssl;
    private Connection connection;

    public Database(RestrictedCreative main, String type) {
        this.main = main;

        this.type = type != null ? type : main.getSettings().getString("database.type").toLowerCase();
        this.host = main.getSettings().getString("database.host");
        this.name = main.getSettings().getString("database.database");
        this.user = main.getSettings().getString("database.username");
        this.pass = main.getSettings().getString("database.password");
        this.port = main.getSettings().getInt("database.port");
        this.ssl = main.getSettings().isEnabled("database.ssl");
        this.table_blocks = main.getSettings().getString("database.table.blocks");
        this.table_invs = main.getSettings().getString("database.table.inventories");

        openConnection();
    }

    /**
     * Return table name
     */
    public String getBlocksTable() {
        return table_blocks;
    }

    /**
     * Return table name
     */
    public String getInvsTable() {
        return table_invs;
    }

    /**
     * Return current connection
     */
    public Connection getConnection() {
        if (connection == null) {
            openConnection();
            return getConnection();
        }

        if (isValidConnection()) {
            return connection;
        } else {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            openConnection();
            return getConnection();
        }
    }

    /**
     * Check if connection is valid
     */
    public boolean isValidConnection() {
        try {
            switch (type) {
                case ("mysql"):
                    return connection.isValid(1);
                case ("sqlite"):
                    return !connection.isClosed();
                default:
                    log("Incompatible database type provided: '" + type + "'. Compatible are: MySQL and SQLite.");
                    return false;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Open a connection of provided type (MySQL or SQLite)
     */
    private void openConnection() {
        switch (type) {
            case ("mysql"):
                // Creates MySQL connection
                openMySQLConnection();
                break;
            case ("sqlite"):
                // Creates MySQL connection
                openSQLiteConnection();
                break;
            default:
                log("Incompatible database type provided: '" + type + "'. Compatible are: MySQL and SQLite.");
                break;
        }
    }

    /**
     * Open MySQL connection
     */
    private void openMySQLConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            synchronized (this) {
                if (connection != null && !connection.isClosed()) {
                    return;
                }
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.name
                        + "?autoReconnect=true&useSSL=" + this.ssl, this.user, this.pass);
            }
        } catch (SQLException e) {
            log("Could not connect to database, check config:");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            log("com.mysql.jdbc.Driver is not installed.");
        }
    }

    /**
     * Open SQLite connection
     */
    private void openSQLiteConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            synchronized (this) {
                if (connection != null && !connection.isClosed()) {
                    return;
                }
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager
                        .getConnection("jdbc:sqlite:" + main.getDataFolder().getPath() + "/" + this.name + ".db");
            }
        } catch (SQLException e) {
            log("Could not connect to database:");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            log("org.sqlite.JDBC is not installed.");
        }
    }

    /**
     * Close current connection
     */
    public void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            log("Could not close connection:");
            e.printStackTrace();
        }
    }

    /**
     * Return PreparedStatement
     */
    public PreparedStatement getStatement(String sql) {
        try {
            return connection.prepareStatement(sql);
        } catch (SQLException e) {
            log("Could not prepare SQL statement:");
            e.printStackTrace();

            return null;
        }
    }

    /**
     * Excecute update
     */
    public void executeUpdate(String sql) {
        try {
            getStatement(sql).executeUpdate();
        } catch (SQLException e) {
            log("Could not execute SQL statement:");
            e.printStackTrace();
        }
    }

    /**
     * Excecute query
     */
    public ResultSet executeQuery(String sql) {
        try {
            return getStatement(sql).executeQuery();
        } catch (SQLException e) {
            log("Could not execute SQL statement:");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Set autocommit
     */
    public void setAutoCommit(boolean status) {
        try {
            getConnection().setAutoCommit(status);
        } catch (SQLException e) {
            log("Could not change autocommit status:");
            e.printStackTrace();
        }
    }

    /**
     * Commit
     */
    public void commit() {
        try {
            getConnection().commit();
        } catch (SQLException e) {
            log("Could not commit:");
            e.printStackTrace();
        }
    }

    /**
     * Print error to console
     */
    private void log(String msg) {
        Bukkit.getLogger().severe(msg);
    }
}
