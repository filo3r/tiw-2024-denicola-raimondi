package it.polimi.tiw.dao;

import it.polimi.tiw.model.User;
import it.polimi.tiw.util.PasswordEncrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static it.polimi.tiw.util.PasswordEncrypt.hashPassword;

public class UserDAO {

    private Connection connection;

    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Create a new user in User's table
     * @param username
     * @param email
     * @param password
     * @throws SQLException
     */
    public int createUser(String username, String email, String password) throws SQLException {
        int row = 0;
        String query = "INSERT INTO User (username, email, password) VALUES (?, ?, ?)";
        PreparedStatement preparedStatement = null;

        try{
            String hashedPassword = PasswordEncrypt.hashPassword(password);

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, email);
            preparedStatement.setString(3, hashedPassword);
            row = preparedStatement.executeUpdate();
        }catch (SQLException e){
            throw new SQLException(e);
        }finally{
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (Exception e1) {
                throw new SQLException("Failed to close PreparedStatement", e1);
            }
        }

        //row = 1 --> success
        return row;
    }

    /**
     * Retrieves a user by username from the database.
     * @param username the username to search for
     * @return a User object populated with username and email if found, otherwise null
     * @throws SQLException if a database access error occurs
     */
    public User getUserByUsername(String username) throws SQLException {
        String query = "SELECT username, email FROM User WHERE username = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet result = preparedStatement.executeQuery()) {
            preparedStatement.setString(1, username);
            if (result.next()) {
                User user = new User();
                user.setUsername(result.getString("username"));
                user.setEmail(result.getString("email"));
                return user;
            }
        } catch (SQLException e) {
            throw new SQLException("Error retrieving user by username", e);
        }
        return null;
    }

    /**
     * Check if a user with a username exists
     * @param username username to check
     * @return true if it exists, false otherwise
     * @throws SQLException an error occurred
     */
    public boolean checkByUsername(String username) throws SQLException {
        String query = "SELECT username FROM User WHERE username = ? LIMIT 1";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            try (ResultSet result = preparedStatement.executeQuery()) {
                // Return true if any row is found
                return result.next();
            }
        } catch (SQLException e) {
            throw new SQLException("Error checking username availability", e);
        }
    }

    /**
     * Check if a user with a specified email exists in the database.
     * @param email the email address to check for existence.
     * @return true if an existing user with the specified email is found, false otherwise.
     * @throws SQLException if an error occurs during the database query execution.
     */
    public boolean checkByEmail(String email) throws SQLException {
        String query = "SELECT email FROM User WHERE mail = ? LIMIT 1";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, email);
            try (ResultSet result = preparedStatement.executeQuery()) {
                // Return true if the email exists in the database
                return result.next();
            }
        } catch (SQLException e) {
            throw new SQLException("Error checking email availability", e);
        }
    }

    /**
     * Validates user credentials against the database by comparing hashed passwords.
     * @param username the username of the user
     * @param password the plaintext password to validate
     * @return a User object if credentials are valid, null otherwise
     * @throws SQLException if a database error occurs
     */
    public User checkCredentials(String username, String password) throws SQLException {
        String query = "SELECT username, email, password FROM User WHERE username = ?";
        try (PreparedStatement pstatement = connection.prepareStatement(query)) {
            pstatement.setString(1, username);
            try (ResultSet result = pstatement.executeQuery()) {
                if (result.next()) {
                    String hashedPassword = result.getString("password");
                    if (PasswordEncrypt.checkPassword(password, hashedPassword)) {
                        User user = new User();
                        user.setUsername(result.getString("username"));
                        user.setEmail(result.getString("email"));
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Error checking user credentials", e);
        }
        return null;
    }


}
