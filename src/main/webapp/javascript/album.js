/**
 * Sends a POST request to log out the user and redirects them to the appropriate page.
 * The function sends a POST request to the server at "./home" with the action "logoutHome".
 * If the server response is successful and includes a "redirect" URL, the browser will
 * navigate to that URL.
 * @async
 * @function logoutAlbum
 * @returns {Promise<void>} Resolves when the request is complete.
 */
async function logoutAlbum() {
    try {
        const response = await fetch("./home", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ action: "logoutHome" }),
        });
        const result = await response.json();
        if (response.ok) {
            if (result.redirect)
                window.location.href = result.redirect;
        }
    } catch (error) {}
}

/**
 * Sends a POST request to return to the home page and redirects the user.
 * The function sends a POST request to the server at "./album" with the action "returnToHome".
 * If the server response is successful and includes a "redirect" URL, the browser will
 * navigate to that URL.
 * @async
 * @function returnToHome
 * @returns {Promise<void>} Resolves when the request is complete.
 */
async function returnToHome() {
    try {
        const response = await fetch("./album", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ action: "returnToHome" }),
        });
        const result = await response.json();
        if (response.ok) {
            if (result.redirect)
                window.location.href = result.redirect;
        }
    } catch (error) {}
}

/**
 * Adds an event listener to the form with ID "logoutAlbumForm".
 * When the form is submitted, the default form submission behavior is prevented,
 * and the `logoutAlbum` function is executed.
 */
document.getElementById("logoutAlbumForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    await logoutAlbum();
});

/**
 * Adds an event listener to the form with ID "returnToHomeForm".
 * When the form is submitted, the default form submission behavior is prevented,
 * and the `returnToHome` function is executed.
 */
document.getElementById("returnToHomeForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    await returnToHome();
});