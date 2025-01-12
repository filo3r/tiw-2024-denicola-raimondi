// Import spa.js
import { forceHashChange } from "./spa.js";

/**
 * Sends a POST request to log out the user and redirects them to the appropriate page.
 * The function sends a POST request to the server at "./album" with the action "logoutAlbum".
 * If the server response is successful and includes a "redirect" URL, the browser will
 * navigate to that URL.
 * @async
 * @function logoutAlbum
 * @param {number} albumId - The ID of the album to include in the logout request.
 * @returns {Promise<void>} Resolves when the request is complete.
 */
async function logoutAlbum(albumId) {
    try {
        const response = await fetch(`./album?albumId=${encodeURIComponent(albumId)}`, {
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
 * @param {number} albumId - The ID of the album to include in the request.
 * @returns {Promise<void>} Resolves when the request is complete.
 */
async function returnToHome(albumId) {
    try {
        const response = await fetch(`./album?albumId=${encodeURIComponent(albumId)}`, {
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

/**
 * Initializes drag-and-drop functionality for reordering images within an album.
 * Adds event listeners to enable editing and saving a custom image order.
 * @function initDragAndDrop
 * @param {number} albumId - The ID of the album for which drag-and-drop functionality is enabled.
 */
function initDragAndDrop(albumId) {
    const editButton = document.getElementById("editOrderButton");
    const saveButton = document.getElementById("saveOrderButton");
    const orderList = document.getElementById("imagesOrderList");
    const errorDiv = document.getElementById("saveOrderError");
    errorDiv.textContent = "";
    errorDiv.classList.add("hidden");
    let draggedItem = null;
    // Disable drag-and-drop by default
    if (orderList) {
        Array.from(orderList.children).forEach((item) => {
            item.setAttribute("draggable", "false");
        });
    }
    // Enable drag-and-drop
    if (editButton && saveButton) {
        // Enable drag-and-drop editing mode
        editButton.addEventListener("click", () => {
            editButton.style.display = "none";
            saveButton.style.display = "inline-block";
            // Enable drag-and-drop for the list items
            Array.from(orderList.children).forEach((item) => {
                item.setAttribute("draggable", "true");
            });
            // Add drag-and-drop event listeners
            orderList.addEventListener("dragstart", (e) => {
                if (e.target && e.target.classList.contains("image-order-item")) {
                    draggedItem = e.target;
                    e.target.style.opacity = "0.5";
                }
            });
            orderList.addEventListener("dragend", (e) => {
                if (e.target && e.target.classList.contains("image-order-item")) {
                    e.target.style.opacity = "1";
                }
            });
            orderList.addEventListener("dragover", (e) => {
                e.preventDefault();
                const hoveredItem = e.target;
                if (hoveredItem && hoveredItem.classList.contains("image-order-item") && hoveredItem !== draggedItem) {
                    const bounding = hoveredItem.getBoundingClientRect();
                    const offset = e.clientY - bounding.top;
                    if (offset > bounding.height / 2) {
                        hoveredItem.after(draggedItem);
                    } else {
                        hoveredItem.before(draggedItem);
                    }
                }
            });
        });
        // Save the new order
        saveButton.addEventListener("click", async () => {
            const sortedImageIds = Array.from(orderList.children).map((item) => parseInt(item.dataset.id, 10));
            // Send the new order to the server
            try {
                const response = await fetch(`./album?albumId=${encodeURIComponent(albumId)}`, {
                    method: "POST",
                    headers: {"Content-Type": "application/json"},
                    body: JSON.stringify({ action: "saveOrder", sortedImageIds: sortedImageIds }),
                });
                // Disable drag-and-drop
                Array.from(orderList.children).forEach((item) => {
                    item.setAttribute("draggable", "false");
                });
                saveButton.style.display = "none";
                editButton.style.display = "inline-block";
                const result = await response.json();
                if (response.ok) {
                    if (result.message) {
                        sessionStorage.setItem("saveOrderSuccess", result.message);
                    }
                    if (result.redirect) {
                        forceHashChange(result.redirect);
                    }
                } else {
                    errorDiv.textContent = result.message || "Error saving images' order.";
                    errorDiv.classList.remove("hidden");
                }
            } catch (error) {
                errorDiv.textContent = "Server error.";
                errorDiv.classList.remove("hidden");
            }
        })
    }
}

/**
 * Initializes event listeners for actions on the album page.
 * Binds event listeners for the return-to-home and logout forms, and sets up drag-and-drop
 * functionality for customizing the order of images.
 * @function initAlbumPageEventListeners
 * @param {number} albumId - The ID of the album to initialize the event listeners for.
 */
export function initAlbumPageEventListeners(albumId) {
    // Get the form for return to home and bind its submit event
    const returnToHomeForm = document.getElementById("returnToHomeForm");
    if (returnToHomeForm) {
        returnToHomeForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await returnToHome(albumId);
        });
    }
    // Get the form for logging out and bind its submit event
    const logoutAlbumForm = document.getElementById("logoutAlbumForm");
    if (logoutAlbumForm) {
        logoutAlbumForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await logoutAlbum(albumId);
        });
    }
    // Initialize drag-and-drop functionality to customize the order of the images
    initDragAndDrop(albumId);
}