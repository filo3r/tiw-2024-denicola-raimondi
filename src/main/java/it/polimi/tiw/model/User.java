package it.polimi.tiw.model;

/**
 * Represents a user with a username, email, and password.
 * This class provides methods to retrieve and update these fields.
 */
public class User {

    /**
     * The username of the user.
     */
    private String username;

    /**
     * The email address of the user.
     */
    private String email;

    /**
     * The password of the user.
     */
    private String password;

    /**
     * Constructs a new User without parameters.
     */
    public User() {}

    /**
     * Constructs a new User with the specified username, email, and password.
     * @param username the username of the user
     * @param email    the email address of the user
     * @param password the password of the user
     */
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    /**
     * Retrieves the username of the user.
     * @return the username of the user
     */
    public String getUsername() {
        return username;
    }

    /**
     * Updates the username of the user.
     * @param username the new username of the user
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Retrieves the email address of the user.
     * @return the email address of the user
     */
    public String getEmail() {
        return email;
    }

    /**
     * Updates the email address of the user.
     * @param email the new email address of the user
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Retrieves the password of the user.
     * @return the password of the user
     */
    public String getPassword() {
        return password;
    }

    /**
     * Updates the password of the user.
     * @param password the new password of the user
     */
    public void setPassword(String password) {
        this.password = password;
    }

}