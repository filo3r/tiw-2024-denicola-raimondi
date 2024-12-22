// Import StringUtil class
import StringUtil from './StringUtil.js';

/**
 * Handles user sign-up process.
 * Validates user inputs (email, username, passwords) using StringUtil methods.
 * If validation passes, sends sign-up data to the server via a POST request.
 * Displays appropriate error messages for invalid input or server errors.
 * Redirects the user upon successful sign-up if the server provides a redirect URL.
 */
async function signUp() {
    const email = document.getElementById("signUpEmail").value.trim();
    const username = document.getElementById("signUpUsername").value.trim();
    const password1 = document.getElementById("signUpPassword1").value;
    const password2 = document.getElementById("signUpPassword2").value;
    const errorDiv = document.getElementById("signUpError");
    // Reset errors
    errorDiv.textContent = "";
    errorDiv.classList.add("hidden");
    // Check email
    if (!StringUtil.isValidEmail(email)) {
        errorDiv.textContent = "Invalid email.";
        errorDiv.classList.remove("hidden");
        return;
    }
    // Check username
    if (!StringUtil.isValidUsername(username)) {
        errorDiv.textContent = "Invalid username.";
        errorDiv.classList.remove("hidden");
        return;
    }
    // Check passwords
    if (!StringUtil.isValidPassword(password1)) {
        errorDiv.textContent = "Invalid password.";
        errorDiv.classList.remove("hidden");
        return;
    }
    if (password1 !== password2) {
        errorDiv.textContent = "Passwords don't match.";
        errorDiv.classList.remove("hidden");
        return;
    }
    // Send data to server
    try {
        const response = await fetch("./index", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ action: "signUp", email: email, username: username, password1: password1, password2: password2 }),
        });
        const result = await response.json();
        if (response.ok) {
            if (result.redirect)
                window.location.href = result.redirect;
        } else {
            errorDiv.textContent = result.message || "Error during sign up.";
            errorDiv.classList.remove("hidden");
        }
    } catch (error) {
        errorDiv.textContent = "Server error.";
        errorDiv.classList.remove("hidden");
    }
}

/**
 * Handles user sign-in process.
 * Validates user inputs (email, password) using StringUtil methods.
 * If validation passes, sends sign-in data to the server via a POST request.
 * Displays appropriate error messages for invalid input or server errors.
 * Redirects the user upon successful sign-in if the server provides a redirect URL.
 */
async function signIn() {
    const email = document.getElementById("signInEmail").value.trim();
    const password = document.getElementById("signInPassword").value;
    const errorDiv = document.getElementById("signInError");
    errorDiv.textContent = "";
    errorDiv.classList.add("hidden");
    // Check email
    if (!StringUtil.isValidEmail(email)) {
        errorDiv.textContent = "Invalid email.";
        errorDiv.classList.remove("hidden");
        return;
    }
    // Check password
    if (!StringUtil.isValidPassword(password)) {
        errorDiv.textContent = "Invalid password.";
        errorDiv.classList.remove("hidden");
        return;
    }
    // Send data to server
    try {
        const response = await fetch("./index", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ action: "signIn", email: email, password: password }),
        });
        const result = await response.json();
        if (response.ok) {
            if (result.redirect)
                window.location.href = result.redirect;
        } else {
            errorDiv.textContent = result.message || "Error during sign in.";
            errorDiv.classList.remove("hidden");
        }
    } catch (error) {
        errorDiv.textContent = "Server error.";
        errorDiv.classList.remove("hidden");
    }
}

// Event listeners
document.getElementById("signUpButton").addEventListener("click", signUp);
document.getElementById("signInButton").addEventListener("click", signIn);