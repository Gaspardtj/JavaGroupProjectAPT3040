package library.dao;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private final Connection connection;

    private DatabaseConnection() {
        connection = openConnection();
    }

    public static Connection openConnection() {
        Properties config = new Properties();
        Path path = Paths.get("database.local.properties");
        try {
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { config.load(reader); }
            }
            String url = setting(config, "LIBRARY_DB_URL", "db.url", "jdbc:postgresql://localhost:5432/library_management_system");
            String user = setting(config, "LIBRARY_DB_USER", "db.user", "library_app");
            String password = setting(config, "LIBRARY_DB_PASSWORD", "db.password", "");
            DriverManager.setLoginTimeout(10);
            return DriverManager.getConnection(url, user, password);
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Cannot connect to PostgreSQL. Check database.local.properties and run setup-database.ps1. " + e.getMessage(), e);
        }
    }
    private static String setting(Properties config, String env, String key, String fallback) {
        String value = System.getenv(env);
        return value != null ? value : config.getProperty(key, fallback);
    }
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) instance = new DatabaseConnection();
        return instance;
    }
    public Connection getConnection() { return connection; }
    public static synchronized void closeConnection() {
        if (instance != null) {
            try { instance.connection.close(); }
            catch (SQLException e) { System.err.println("Could not close database: " + e.getMessage()); }
            finally { instance = null; }
        }
    }
}
