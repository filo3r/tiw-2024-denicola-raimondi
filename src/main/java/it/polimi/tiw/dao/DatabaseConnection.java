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
     */
    private static Connection connection = null;

    /**
     * Establishes and returns a connection to the database.
     * The method ensures that only one connection is used throughout the application (Singleton pattern).
     * @return Connection object to the database
     */
    public static synchronized Connection getConnection() {
        if (connection == null) {
            // Load properties from database.properties file
            try (InputStream inputStream = DatabaseConnection.class.getClassLoader().getResourceAsStream("database/database.properties")) {
                // Check if the properties file is found
                if (inputStream == null)
                    throw new IOException("Database property file not found.");
                // Load properties from the input stream
                Properties properties = new Properties();
                properties.load(inputStream);
                // Retrieve database connection properties
                String driver = properties.getProperty("database.driver");
                String url = properties.getProperty("database.url");
                String username = properties.getProperty("database.user");
                String password = properties.getProperty("database.password");
                // Load the database driver class
                Class.forName(driver);
                // Establish the connection to the database
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
        return connection;
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