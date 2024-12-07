package it.polimi.tiw.util;

import java.util.regex.Pattern;

/**
 * Utility class providing methods for string validation and manipulation.
 */
public class StringUtil {

    /**
     * Regular expression pattern for validating email addresses.
     * The email must contain letters, numbers, and optionally symbols (+, _, ., -),
     * followed by an '@' symbol, a domain name, and a top-level domain between 2 and 6 characters.
     */
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    /**
     * Compiled pattern for email validation based on the EMAIL_REGEX.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    /**
     * Regular expression pattern for validating usernames.
     * Usernames must be 1 to 32 characters long, containing lowercase letters, numbers, underscores, or periods.
     * The username cannot start or end with a period, nor have consecutive periods.
     */
    private static final String USERNAME_REGEX = "^(?!\\.)(?!.*\\.\\.)[a-z0-9._]{1,32}(?<!\\.)$";

    /**
     * Compiled pattern for username validation based on the USERNAME_REGEX.
     */
    private static final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME_REGEX);

    /**
     * Regular expression pattern for validating passwords.
     * Passwords must be at least 8 characters long.
     */
    private static final String PASSWORD_REGEX = "^.{8,}$";

    /**
     * Compiled pattern for password validation based on the PASSWORD_REGEX.
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    /**
     * Checks if a string is null or empty.
     * @param string the string to check
     * @return true if the string is null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    /**
     * Checks if a string contains spaces.
     * @param string the string to check
     * @return true if the string contains spaces, false otherwise
     */
    public static boolean hasSpaces(String string) {
        return string.contains(" ");
    }

    /**
     * Validates if a string's length falls within the specified range.
     * @param string the string to validate
     * @param minLength the minimum acceptable length
     * @param maxLength the maximum acceptable length
     * @return true if the string length is within the range, false otherwise
     */
    public static boolean isValidLength(String string, int minLength, int maxLength) {
        return (!isNullOrEmpty(string) && string.length() >= minLength && string.length() <= maxLength);
    }

    /**
     * Validates if the provided email has a valid format.
     * An email is considered valid if it is not null, not empty, does not contain spaces,
     * and matches the specified email pattern.
     * @param email the email address to validate
     * @return true if the email is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return (!isNullOrEmpty(email) && !hasSpaces(email) && EMAIL_PATTERN.matcher(email).matches() && isValidLength(email, 6, 64));
    }

    /**
     * Validates if the provided username has a valid format.
     * A username is considered valid if it is not null, not empty, does not contain spaces,
     * and matches the specified username pattern.
     * @param username the username to validate
     * @return true if the username is valid, false otherwise
     */
    public static boolean isValidUsername(String username) {
        return (!isNullOrEmpty(username) && !hasSpaces(username) && USERNAME_PATTERN.matcher(username).matches() && isValidLength(username, 1, 32));
    }

    /**
     * Validates if the provided password has a valid format.
     * A password is considered valid if it is not null, not empty, does not contain spaces,
     * and matches the specified password pattern.
     * @param password the password to validate
     * @return true if the password is valid, false otherwise
     */
    public static boolean isValidPassword(String password) {
        return (!isNullOrEmpty(password) && !hasSpaces(password) && PASSWORD_PATTERN.matcher(password).matches() && isValidLength(password, 8, 128));
    }

    /**
     * Validates if the provided album title is valid.
     * An album title is considered valid if it is not null and does not start with the '@' character.
     * @param title the album title to validate
     * @return true if the album title is valid, false otherwise
     */
    public static boolean isValidTitle(String title) {
        return (!isNullOrEmpty(title) && !title.startsWith("@") && isValidLength(title, 1, 64));
    }

    /**
     * Validates if the provided text is valid.
     * A text is considered valid if it is not null and its length is within the acceptable range.
     * @param text the text to validate
     * @return true if the text is valid, false otherwise
     */
    public static boolean isValidText(String text) {
        return (!isNullOrEmpty(text) && isValidLength(text, 1, 512));
    }

}