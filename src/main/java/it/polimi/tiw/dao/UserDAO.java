package it.polimi.tiw.dao;

import it.polimi.tiw.model.User;
import it.polimi.tiw.util.DatabaseConnectionPool;
import it.polimi.tiw.util.PasswordEncrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for performing CRUD operations on the User entity.
 * This class provides methods to register a new user, log in a user,
 * check if a username or email is taken, and retrieve a username by email.
 */
public class UserDAO {

    /**
     * Connection pool to manage database connections efficiently
     */
    private final DatabaseConnectionPool databaseConnectionPool;

    /**
     * Initializes the UserDAO by obtaining an instance of the DatabaseConnectionPool.
     * @throws SQLException if there is a database access error
     */
    public UserDAO() throws SQLException {
        this.databaseConnectionPool = DatabaseConnectionPool.getInstance();
    }

    /**
     * Registers a new user in the database.
     * @param user the User object containing the details to register
     * @return true if the user was registered successfully, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean registerUser(User user) throws SQLException {
        String query = "INSERT INTO User (username, email, password) VALUES (?, ?, ?)";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            int rowsInserted = statement.executeUpdate();
            return rowsInserted > 0;
        } finally {
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
    }

    /**
     * Authenticates a user by checking the password against the stored hash in the database.
     * @param user the User object containing the email and password to authenticate
     * @return true if the user is authenticated successfully, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean loginUser(User user) throws SQLException {
        String query = "SELECT password FROM User WHERE email = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, user.getEmail());
            result = statement.executeQuery();
            if (result.next()) {
                String hashedPassword = result.getString("password");
                return PasswordEncrypt.checkPassword(user.getPassword(), hashedPassword);
            }
        } finally {
            if (result != null)
                result.close();
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
        return false;
    }

    /**
     * Checks if a username is already registered in the database.
     * @param username the username to check
     * @return true if the username is taken, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean isUsernameTaken(String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM User WHERE username = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            result = statement.executeQuery();
            if (result.next())
                return result.getInt(1) > 0;
        } finally {
            if (result != null)
                result.close();
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
        return false;
    }

    /**
     * Checks if an email is already registered in the database.
     * @param email the email address to check
     * @return true if the email is taken, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean isEmailTaken(String email) throws SQLException {
        String query = "SELECT COUNT(*) FROM User WHERE email = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, email);
            result = statement.executeQuery();
            if (result.next())
                return result.getInt(1) > 0;
        } finally {
            if (result != null)
                result.close();
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
        return false;
    }

    /**
     * Retrieves the username associated with a given email address.
     * @param email the email address to look up
     * @return the username associated with the email, or null if not found
     * @throws SQLException if a database access error occurs
     */
    public String getUsernameByEmail(String email) throws SQLException {
        String query = "SELECT username FROM User WHERE email = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = databaseConnectionPool.getConnection();
            statement = connection.prepareStatement(query);
            statement.setString(1, email);
            result = statement.executeQuery();
            if (result.next())
                return result.getString("username");
        } finally {
            if (result != null)
                result.close();
            if (statement != null)
                statement.close();
            if (connection != null)
                databaseConnectionPool.releaseConnection(connection);
        }
        return null;
    }

}