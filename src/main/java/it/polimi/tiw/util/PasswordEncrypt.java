package it.polimi.tiw.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for handling password hashing and verification using the BCrypt algorithm.
 * Provides methods to hash a plain-text password and verify if a plain-text password matches
 * a previously hashed password.
 */
public class PasswordEncrypt {

    /**
     * Hashes a plain-text password using BCrypt with a specified work factor.
     * @param password the plain-text password to be hashed
     * @return the hashed password as a String
     */
    public static String hashPassword(String password) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(10));
        return hashedPassword;
    }

    /**
     * Verifies if a plain-text password matches a given hashed password.
     * @param password       the plain-text password to verify
     * @param hashedPassword the hashed password to compare against
     * @return true if the plain-text password matches the hashed password, false otherwise
     */
    public static boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }

}