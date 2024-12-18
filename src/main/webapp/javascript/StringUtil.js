/**
 * Utility class providing methods for string validation and manipulation.
 * This class includes validation methods for emails, usernames, passwords,
 * titles, and generic texts, ensuring they meet specific format and length requirements.
 */
class StringUtil {

    /**
     * Regular expression for email validation.
     * Matches emails containing letters, numbers, and optional symbols (+, _, ., -),
     * followed by an '@' symbol, a domain name, and a top-level domain between 2 and 6 characters.
     */
    static EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,6}$/;

    /**
     * Regular expression for username validation.
     * Matches usernames 1 to 32 characters long, containing lowercase letters, numbers, underscores, or periods.
     * Usernames cannot start or end with a period, nor have consecutive periods.
     */
    static USERNAME_REGEX = /^(?!\.)(?!.*\.\.)[a-z0-9._]{1,32}(?<!\.)$/;

    /**
     * Regular expression for password validation.
     * Matches passwords with a minimum length of 8 characters.
     */
    static PASSWORD_REGEX = /^.{8,}$/;

    /**
     * Regular expression for title validation.
     * Matches titles that do not start with '@' and do not contain '<' or '>'.
     */
    static TITLE_REGEX = /^(?!@)[^<>]*$/;

    /**
     * Regular expression for text validation.
     * Matches texts that do not contain the characters '<' or '>'.
     */
    static TEXT_REGEX = /^[^<>]*$/;

    /**
     * Checks if a string is null or empty.
     * @param {string} string - The string to check.
     * @returns {boolean} - True if the string is null or empty, false otherwise.
     */
    static isNullOrEmpty(string) {
        return string == null || string.trim().length === 0;
    }

    /**
     * Checks if a string contains spaces.
     * @param {string} string - The string to check.
     * @returns {boolean} - True if the string contains spaces, false otherwise.
     */
    static hasSpaces(string) {
        return string.includes(' ');
    }

    /**
     * Validates if a string's length falls within the specified range.
     * @param {string} string - The string to validate.
     * @param {number} minLength - The minimum acceptable length.
     * @param {number} maxLength - The maximum acceptable length.
     * @returns {boolean} - True if the string length is within the range, false otherwise.
     */
    static isValidLength(string, minLength, maxLength) {
        return !this.isNullOrEmpty(string) && string.length >= minLength && string.length <= maxLength;
    }

    /**
     * Validates if the provided email has a valid format.
     * @param {string} email - The email address to validate.
     * @returns {boolean} - True if the email is valid, false otherwise.
     */
    static isValidEmail(email) {
        return (
            !this.isNullOrEmpty(email) &&
            !this.hasSpaces(email) &&
            this.EMAIL_REGEX.test(email) &&
            this.isValidLength(email, 6, 64)
        );
    }

    /**
     * Validates if the provided username has a valid format.
     * @param {string} username - The username to validate.
     * @returns {boolean} - True if the username is valid, false otherwise.
     */
    static isValidUsername(username) {
        return (
            !this.isNullOrEmpty(username) &&
            !this.hasSpaces(username) &&
            this.USERNAME_REGEX.test(username) &&
            this.isValidLength(username, 1, 32)
        );
    }

    /**
     * Validates if the provided password has a valid format.
     * @param {string} password - The password to validate.
     * @returns {boolean} - True if the password is valid, false otherwise.
     */
    static isValidPassword(password) {
        return (
            !this.isNullOrEmpty(password) &&
            !this.hasSpaces(password) &&
            this.PASSWORD_REGEX.test(password) &&
            this.isValidLength(password, 8, 128)
        );
    }

    /**
     * Validates if the provided album title is valid.
     * @param {string} title - The album title to validate.
     * @returns {boolean} - True if the album title is valid, false otherwise.
     */
    static isValidTitle(title) {
        return (
            !this.isNullOrEmpty(title) &&
            this.TITLE_REGEX.test(title) &&
            this.isValidLength(title, 1, 64)
        );
    }

    /**
     * Validates if the provided text is valid.
     * @param {string} text - The text to validate.
     * @returns {boolean} - True if the text is valid, false otherwise.
     */
    static isValidText(text) {
        return (
            !this.isNullOrEmpty(text) &&
            this.TEXT_REGEX.test(text) &&
            this.isValidLength(text, 1, 512)
        );
    }

}

// Export the class for use in other files
export default StringUtil;