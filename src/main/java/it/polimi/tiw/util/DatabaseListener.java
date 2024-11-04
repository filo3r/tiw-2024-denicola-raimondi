package it.polimi.tiw.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseListener is a ServletContextListener that manages the lifecycle of the DatabaseConnectionPool.
 * It initializes the connection pool when the web application starts and shuts it down when the application is stopped.
 */
public class DatabaseListener implements ServletContextListener {

    /**
     * Called when the web application context is initialized.
     * This method creates the singleton instance of DatabaseConnectionPool and stores it as a context attribute
     * to be shared across the application.
     * @param contextEvent the ServletContextEvent that provides access to the ServletContext
     */
    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        try {
            // Obtain the singleton instance of DatabaseConnectionPool
            DatabaseConnectionPool databaseConnectionPool = DatabaseConnectionPool.getInstance();
            // Store the connection pool in the ServletContext for global access in the application
            contextEvent.getServletContext().setAttribute("databaseConnectionPool", databaseConnectionPool);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize connection pool", e);
        }
    }

    /**
     * Called when the web application context is destroyed.
     * This method retrieves the DatabaseConnectionPool instance from the context attributes and shuts it down
     * to release all database connections and resources.
     * @param contextEvent the ServletContextEvent that provides access to the ServletContext
     */
    @Override
    public void contextDestroyed(ServletContextEvent contextEvent) {
        // Retrieve the DatabaseConnectionPool instance from the ServletContext
        DatabaseConnectionPool databaseConnectionPool = (DatabaseConnectionPool) contextEvent.getServletContext().getAttribute("databaseConnectionPool");
        // If the connection pool exists, shut it down to release resources
        if (databaseConnectionPool != null) {
            databaseConnectionPool.shutdown();
        }
        // Explicitly unregister the JDBC driver
        deregisterJdbcDriver();
    }

    /**
     * Deregisters the JDBC driver to prevent potential memory leaks when the application is undeployed.
     */
    private void deregisterJdbcDriver() {
        try {
            Driver driver = DriverManager.getDriver(DatabaseConnectionPool.getDatabaseUrl());
            if (driver != null) {
                DriverManager.deregisterDriver(driver);
            }
        } catch (SQLException e) {
            System.err.println("Failed to deregister JDBC driver: " + e.getMessage());
        }
    }

}