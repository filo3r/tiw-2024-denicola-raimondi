// Import StringUtil class
import StringUtil from './StringUtil.js';
// Import spa.js
import { forceHashChange } from './spa.js';

/**
 * Handles the creation of a new album.
 * Validates the album title and sends a request to the server to create the album.
 * Displays success or error messages based on the server's response.
 * @param {Event} event - The event triggered by submitting the create album form.
 */
async function createAlbum(event) {
    event.preventDefault();
    const albumTitle = document.getElementById("albumTitle").value;
    const errorDiv = document.getElementById("createAlbumError");
    // Reset errors
    errorDiv.textContent = "";
    errorDiv.classList.add("hidden");
    // Check album title
    if (!StringUtil.isValidTitle(albumTitle)) {
        errorDiv.textContent = "Invalid album title.";
        errorDiv.classList.remove("hidden");
        return;
    }
    // Send data to server
    try {
        const response = await fetch("./home", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ action: "createAlbum", albumTitle: albumTitle }),
        });
        const result = await response.json();
        if (response.ok) {
            if (result.message) {
                sessionStorage.setItem("createAlbumSuccess", result.message);
            }
            if (result.redirect) {
                forceHashChange(result.redirect);
            }
        } else {
            errorDiv.textContent = result.message || "Error creating album.";
            errorDiv.classList.remove("hidden");
        }
    } catch (error) {
        errorDiv.textContent = "Server error.";
        errorDiv.classList.remove("hidden");
    }
}

/**
 * Handles the addition of an image to an album.
 * Validates the image title, description, file, and album selection.
 * Converts the image file to Base64 and sends the data to the server.
 * Displays success or error messages based on the server's response.
 * @param {Event} event - The event triggered by submitting the add image form.
 */
async function addImage(event) {
    event.preventDefault();
    const imageTitle = document.getElementById("imageTitle").value;
    const imageText = document.getElementById("imageText").value;
    const imageFile = document.getElementById("imageFile")
    const albumSelect = Array.from(document.querySelectorAll('input[name="albumSelect"]:checked')).map(cb => cb.value);
    const errorDiv = document.getElementById("addImageError");
    // Reset errors
    errorDiv.textContent = "";
    errorDiv.classList.add("hidden");
    // Check image title
    if (!StringUtil.isValidTitle(imageTitle)) {
        errorDiv.textContent = "Invalid image title.";
        errorDiv.classList.remove("hidden");
        return;
    }
    // Check image text
    if (!StringUtil.isValidText(imageText)) {
        errorDiv.textContent = "Invalid image description.";
        errorDiv.classList.remove("hidden");
        return;
    }
    // Check image file
    if (!imageFile.files || imageFile.files.length === 0) {
        errorDiv.textContent = "Image not uploaded or empty.";
        errorDiv.classList.remove("hidden");
        return;
    }
    const file = imageFile.files[0];
    if (file.size > 1024 * 1024 * 100) {
        errorDiv.textContent = "Image is too large. Maximum allowed size is 100 MB.";
        errorDiv.classList.remove("hidden");
        return;
    }
    const allowedTypes = ["image/jpeg", "image/png", "image/webp", "image/jpg"];
    if (!allowedTypes.includes(file.type)) {
        errorDiv.textContent = "Invalid image type. Only JPG, JPEG, PNG or WEBP images are allowed.";
        errorDiv.classList.remove("hidden");
        return;
    }
    // Reading the file in base 64
    const fileReader = new FileReader();
    fileReader.onloadend = async () => {
        const base64DataUrl = fileReader.result;
        const base64String = base64DataUrl.split(",")[1];
        // Send data to server
        try {
            // Send data
            const response = await fetch("./home", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    action: "addImage",
                    imageTitle: imageTitle,
                    imageText: imageText,
                    imageFile: base64String,
                    imageMimeType: file.type,
                    albumSelect: albumSelect
                }),
            });
            const result = await response.json();
            if (response.ok) {
                if (result.message) {
                    sessionStorage.setItem("addImageSuccess", result.message);
                }
                if (result.redirect) {
                    forceHashChange(result.redirect);
                }
            } else {
                errorDiv.textContent = result.message || "Error adding image.";
                errorDiv.classList.remove("hidden");
            }
        } catch (error) {
            errorDiv.textContent = "Server error.";
            errorDiv.classList.remove("hidden");
        }
    };
    fileReader.readAsDataURL(file);
}

/**
 * Handles the logout process from the home page.
 * Sends a logout request to the server and redirects the user if necessary.
 */
async function logoutHome() {
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
 * Initializes event listeners for the homepage.
 * Binds specific form submission events to their respective handler functions.
 * Ensures proper functionality for album creation, image addition, and logout.
 */
export function initHomePageEventListeners() {
    // Get the form for creating a new album and bind its submit event
    const createAlbumForm = document.getElementById("createAlbumForm");
    if (createAlbumForm) {
        createAlbumForm.addEventListener("submit", createAlbum);
    }
    // Get the form for adding an image and bind its submit event
    const addImageForm = document.getElementById("addImageForm");
    if (addImageForm) {
        addImageForm.addEventListener("submit", addImage);
    }
    // Get the form for logging out and bind its submit event
    const logoutHomeForm = document.getElementById("logoutHomeForm");
    if (logoutHomeForm) {
        logoutHomeForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            await logoutHome();
        });
    }
}