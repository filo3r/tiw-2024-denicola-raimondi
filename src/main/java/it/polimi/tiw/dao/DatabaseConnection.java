package it.polimi.tiw.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * This class is responsible for establishing a connection to the database.
 * It uses the Singleton pattern to ensure that only one connection is used throughout the application.
 */
public class DatabaseConnection {

    /**
     * The single instance of the database connection used throughout the application.
     * Declared as volatile to ensure visibility of changes across threads,
     * preventing caching issues and guaranteeing that all threads see the most up-to-date instance.
     */
    private static volatile Connection connection = null;

    /**
     * Returns a connection to the database, ensuring that only one connection is used throughout the application.
     * The Singleton pattern is applied to prevent multiple connections being opened.
     * @return Connection object to the database
     */
    public static synchronized Connection getConnection() {
        // Check if the connection is already initialized
        if (connection == null) {
            // Double-check locking to initialize connection only once
            synchronized (DatabaseConnection.class) {
                if (connection == null) {
                    initializeConnection();
                }
            }
        }
        return connection;
    }

    /**
     * Initializes the database connection by loading database properties and establishing
     * a connection using the provided driver, URL, username, and password.
     */
    private static void initializeConnection() {
        try {
            // Load database properties from a properties file
            Properties properties = loadDatabaseProperties();
            // Retrieve driver, URL, username, and password from properties
            String driver = properties.getProperty("database.driver");
            String url = properties.getProperty("database.url");
            String username = properties.getProperty("database.username");
            String password = properties.getProperty("database.password");
            // Load the database driver class
            Class.forName(driver);
            // Establish the database connection using the DriverManager
            connection = DriverManager.getConnection(url, username, password);
        } catch (IOException e) {
            System.err.println("Error reading properties file: " + e.getMessage());
            System.exit(1);
        } catch (ClassNotFoundException e) {
            System.err.println("Error loading database driver: " + e.getMessage());
            System.exit(1);
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Loads the database properties from a properties file.
     * @return Properties object containing database configuration settings
     * @throws IOException if the properties file is not found or cannot be read
     */
    private static Properties loadDatabaseProperties() throws IOException {
        Properties properties = new Properties();
        // Load the properties file from the classpath
        try (InputStream inputStream = DatabaseConnection.class.getClassLoader().getResourceAsStream("database/database.properties")) {
            if (inputStream == null)
                throw new IOException("Database property file not found.");
            // Load properties from the input stream
            properties.load(inputStream);
        }
        return properties;
    }

    /**
     * Closes the database connection if it is open.
     * Logs a message if the connection is closed successfully or if an error occurs during closure.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

}