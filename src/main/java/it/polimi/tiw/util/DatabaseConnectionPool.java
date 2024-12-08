package it.polimi.tiw.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * DatabaseConnectionPool is a singleton class that manages a pool of reusable database connections.
 * This class optimizes database access by maintaining a set of preallocated connections,
 * reducing the overhead of frequently creating and closing connections. It supports expanding
 * the pool when needed and includes periodic cleanup of idle or invalid connections.
 */
public class DatabaseConnectionPool {

    /** The singleton instance of the DatabaseConnectionPool */
    private static DatabaseConnectionPool instance;

    /** A blocking queue that holds available database connections in the pool */
    private BlockingQueue<Connection> availableConnections;

    /** The driver class name for connecting to the database */
    private static String databaseDriver;

    /** The URL used for connecting to the database */
    private static String databaseUrl;

    /** The username for connecting to the database */
    private static String databaseUsername;

    /** The password for connecting to the database */
    private static String databasePassword;

    /** The initial number of connections to create in the pool */
    private static final int INITIAL_POOL_SIZE = 20;

    /** The maximum number of connections allowed in the pool */
    private static final int MAX_POOL_SIZE = 100;

    /** The number of additional connections to add when expanding the pool */
    private static final int INCREASE_POOL_SIZE = 5;

    /** The timeout in seconds for obtaining a connection from the pool */
    private static final int CONNECTION_TIMEOUT = 3;

    /** The timeout in minutes for cleaning up idle or invalid connections */
    private static final int CLEAN_UP_TIMEOUT = 5;

    /**
     * A scheduled executor for periodically cleaning up idle connections in the pool.
     * Use a custom ThreadFactory to create a new thread, based on Java's default factory.
     * The single-threaded executor ensures that tasks are executed sequentially, one at a time,
     * avoiding thread concurrency issues.
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        return t;
    });

    /**
     * Private constructor that initializes the connection pool, loads database properties,
     * and pre-allocates connections up to the INITIAL_POOL_SIZE.
     * @throws SQLException if a database access error occurs during pool initialization
     */
    private DatabaseConnectionPool() throws SQLException {
        loadDatabaseProperties();
        this.availableConnections = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            this.availableConnections.add(createConnection());
        }
        startCleanUpTask();
    }

    /**
     * Loads database configuration properties from a properties file and initializes the database driver.
     */
    private static void loadDatabaseProperties() {
        try {
            Properties properties = new Properties();
            // Load the database properties from the configuration file
            try (InputStream inputStream = DatabaseConnectionPool.class.getClassLoader().getResourceAsStream("properties/database.properties")) {
                if (inputStream == null)
                    throw new IOException("Database properties file not found");
                properties.load(inputStream);
            }
            // Assign properties values to respective fields
            databaseDriver = properties.getProperty("database.driver");
            databaseUrl = properties.getProperty("database.url");
            databaseUsername = properties.getProperty("database.username");
            databasePassword = properties.getProperty("database.password");
            // Load the database driver
            Class.forName(databaseDriver);
        } catch (IOException e) {
            System.err.println("Error reading properties file: " + e.getMessage());
            System.exit(1);
        } catch (ClassNotFoundException e) {
            System.err.println("Error loading database driver: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Returns the singleton instance of the DatabaseConnectionPool, creating it if it does not already exist.
     * @return the singleton instance of the DatabaseConnectionPool
     * @throws SQLException if a database access error occurs
     */
    public static synchronized DatabaseConnectionPool getInstance() throws SQLException {
        if (instance == null)
            instance = new DatabaseConnectionPool();
        return instance;
    }

    /**
     * Creates a new database connection using the configured database URL, username, and password.
     * @return a new Connection object
     * @throws SQLException if a database access error occurs
     */
    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
    }

    /**
     * Retrieves a connection from the pool, expanding the pool if necessary and within the maximum pool size.
     * @return a valid Connection object from the pool
     * @throws SQLException if a database access error occurs or if interrupted while waiting for a connection
     */
    public Connection getConnection() throws SQLException {
        Connection connection = null;
        try {
            // Try to retrieve a connection from the pool within the specified timeout
            connection = availableConnections.poll(CONNECTION_TIMEOUT, TimeUnit.SECONDS);
            // If no connection is available and pool expansion is possible, add new connections
            if (connection == null && availableConnections.size() + INCREASE_POOL_SIZE <= MAX_POOL_SIZE) {
                synchronized (this) {
                    for (int i = 0; i < INCREASE_POOL_SIZE; i++) {
                        availableConnections.offer(createConnection());
                    }
                }
                // Retrieve a newly created connection from the pool
                connection = availableConnections.take();
            }
            // Check if the retrieved connection is valid
            if (connection != null && !connection.isValid(CONNECTION_TIMEOUT)) {
                connection.close();
                availableConnections.remove(connection);
                connection = createConnection();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a connection.", e);
        }
        return connection;
    }

    /**
     * Returns a connection back to the pool if it is valid, allowing it to be reused.
     * @param connection the Connection object to release back to the pool
     */
    public void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                // Only add the connection back to the pool if it is still valid
                if (connection.isValid(CONNECTION_TIMEOUT))
                    availableConnections.offer(connection);
            } catch (SQLException e) {
                System.err.println("Error while releasing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Starts a scheduled task for cleaning up idle and invalid connections in the pool,
     * ensuring the pool maintains a minimum number of valid connections.
     */
    private void startCleanUpTask() {
        // Schedule a task to clean up idle connections at regular intervals
        scheduler.scheduleAtFixedRate(this::cleanUpIdleConnections, CLEAN_UP_TIMEOUT, CLEAN_UP_TIMEOUT, TimeUnit.MINUTES);
    }

    /**
     * Removes idle or invalid connections from the pool and replenishes it if the pool size falls below INITIAL_POOL_SIZE.
     */
    private void cleanUpIdleConnections() {
        // Remove connections that are invalid or closed
        availableConnections.removeIf(connection -> {
            try {
                return !connection.isValid(CONNECTION_TIMEOUT) || connection.isClosed();
            } catch (SQLException e) {
                return true;
            }
        });
        // Replenish the pool if it falls below the initial size
        if (availableConnections.size() < INITIAL_POOL_SIZE) {
            synchronized (this) {
                while (availableConnections.size() < INITIAL_POOL_SIZE) {
                    try {
                        availableConnections.offer(createConnection());
                    } catch (SQLException e) {
                        System.err.println("Error while creating new connections during clean up: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Shuts down the connection pool, closing all connections and deregistering the database driver.
     * This method is typically called when the application is shutting down.
     */
    public void shutdown() {
        // Stop the scheduler for cleanup tasks
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Close all available connections in the pool
        for (Connection connection : availableConnections) {
            try {
                if (connection != null && !connection.isClosed())
                    connection.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection during shutdown: " + e.getMessage());
            }
        }
        availableConnections.clear();
        // Deregister the database driver
        try {
            Driver driver = DriverManager.getDriver(databaseUrl);
            if (driver != null)
                DriverManager.deregisterDriver(driver);
        } catch (SQLException e) {
            System.err.println("Error deregistering JDBC driver: " + e.getMessage());
        }
        // Nullify the singleton instance
        instance = null;
    }

    /**
     * Returns the configured database URL.
     * @return the database URL
     */
    public static String getDatabaseUrl() {
        return databaseUrl;
    }

}