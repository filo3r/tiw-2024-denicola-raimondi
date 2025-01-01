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
        const response = await fetch("./album", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ action: "logoutAlbum" }),
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
                window.location.hash = result.redirect;
        }
    } catch (error) {}
}

export function initAlbumPageEventListeners() {
    // Get the form for return to home and bind its submit event
    const returnToHomeForm = document.getElementById("returnToHomeForm");
    if (returnToHomeForm) {
        returnToHomeForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await returnToHome();
        });
    }
    // Get the form for logging out and bind its submit event
    const logoutAlbumForm = document.getElementById("logoutAlbumForm");
    if (logoutAlbumForm) {
        logoutAlbumForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await logoutAlbum();
        });
    }
}