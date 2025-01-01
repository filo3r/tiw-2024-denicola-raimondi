// Import home.js
import { initHomePageEventListeners } from './home.js';
// Import album.js
import { initAlbumPageEventListeners } from './album.js';

/**
 * Event listener that initializes the router once the DOM is fully loaded.
 */
document.addEventListener('DOMContentLoaded', () => {
    router();
});

/**
 * Event listener that handles URL hash changes and updates the view accordingly.
 */
window.addEventListener("hashchange", router);

/**
 * Forces a hash change and triggers the "hashchange" event.
 * @param {string} hash - The hash to set in the URL.
 */
export function forceHashChange(hash) {
    window.location.hash = hash;
    window.dispatchEvent(new Event("hashchange"));
}

function getQueryParams(path) {
    const queryIndex = path.indexOf("?");
    if (queryIndex !== -1) {
        const queryString = path.substring(queryIndex + 1);
        const params = new URLSearchParams(queryString);
        return Object.fromEntries(params.entries());
    }
    return {};
}

/**
 * Router function to handle navigation based on the URL hash.
 */
function router() {
    const path = window.location.hash || "#home";
    switch(true) {
        // Home Page
        case path === "#home":
            loadHomePage();
            break;
        // Album Page
        case path.startsWith("#album"):
            const queryParams = getQueryParams(path);
            let albumId = queryParams.albumId || 0;
            let page = queryParams.page || 0;
            albumId = parseInt(albumId, 10);
            if (isNaN(albumId) || albumId <= 0) {
                window.location.hash = "#home";
                return;
            }
            page = parseInt(page, 10);
            if (isNaN(page) || page < 0) {
                page = 0;
            }
            loadAlbumPage(albumId, page);
            break;
        // Default
        default:
            window.location.hash = "#home";
            break;
    }
}

/**
 * Loads the Home Page by fetching its content and dynamically rendering it.
 * Displays a loading message during the fetch operation.
 */
async function loadHomePage() {
    const spa = document.getElementById("spa");
    spa.innerHTML = `<p>Loading...</p>`;
    try {
        const response = await fetch("./home");
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || "Error loading home page. Please try again.");
        }
        const data = await response.json();
        spa.innerHTML = buildHomeHTML(data);
        initHomePageEventListeners();
        showSuccessMessage();
    } catch (error) {
        spa.innerHTML = `<p>${error.message}</p>`;
    }
}

/**
 * Builds the HTML structure for the Home Page using the provided data.
 * @param {Object} data - Data object containing user, albums, and stats information.
 * @returns {string} The generated HTML content for the Home Page.
 */
function buildHomeHTML(data) {
    if (!data || !data.user || !data.myAlbums || !data.otherAlbums || !data.userStats) {
        return `<p>Error loading home page. Please try again.</p>`;
    }
    // Dynamic Home HTML
    let html = `
    <h1>Image Gallery</h1>
    <nav class="home-page-navbar">
        <ul>
            <li><label for="myAlbumsPanel">My Albums</label></li>
            <li><label for="otherAlbumsPanel">Other's Albums</label></li>
            <li><label for="createAlbumPanel">Create Album</label></li>
            <li><label for="addImagePanel">Add Image</label></li>
            <li><label for="profilePanel">Profile</label></li>
        </ul>
    </nav>
    <!-- Hidden radio inputs -->
    <input type="radio" name="panel" id="myAlbumsPanel" checked hidden>
    <input type="radio" name="panel" id="otherAlbumsPanel" hidden>
    <input type="radio" name="panel" id="createAlbumPanel" hidden>
    <input type="radio" name="panel" id="addImagePanel" hidden>
    <input type="radio" name="panel" id="profilePanel" hidden>
    `;
    // My Albums Content
    html += `
    <div class="content" id="myAlbumsContent">
        <h2>My Albums</h2>
        <div class="album-container">
    `;
    if (data.myAlbums.length > 0) {
        data.myAlbums.forEach((album) => {
            html += `
            <a href="#album?albumId=${album.albumId}&page=0">
                <div class="album">
                    <p>${album.albumTitle}</p>
                    <p>${album.albumCreator}</p>
                </div>
            </a>
            `;
        });
    } else {
        html += `<p>There are no albums to display.</p>`;
    }
    html += `
        </div>
    </div>
    `;
    // Other's Albums Content
    html += `
    <div class="content" id="otherAlbumsContent">
        <h2>Other's Albums</h2>
        <div class="album-container">
    `;
    if (data.otherAlbums.length > 0) {
        data.otherAlbums.forEach((album) => {
            html += `
            <a href="#album?albumId=${album.albumId}&page=0">
                <div class="album">
                    <p>${album.albumTitle}</p>
                    <p>${album.albumCreator}</p>
                </div>
            </a>
            `;
        });
    } else {
        html += `<p>There are no albums to display.</p>`;
    }
    html += `
        </div>
    </div>
    `;
    // Create Album Content
    html += `
    <div class="content" id="createAlbumContent">
        <h2>Create Album</h2>
        <form id="createAlbumForm">
            <label for="albumTitle">Album Title:</label>
            <input type="text" id="albumTitle" name="albumTitle" required minlength="1" maxlength="64">
            <button type="submit" id="createAlbumButton">Create</button>
        </form>
        <div class="error-message hidden" id="createAlbumError"></div>
        <div class="success-message hidden" id="createAlbumSuccess"></div>
    </div>
    `;
    // Add Image Content
    html += `
    <div class="content" id="addImageContent">
        <h2>Add Image</h2>
        <form id="addImageForm">
            <label for="imageTitle">Image Title:</label>
            <input type="text" id="imageTitle" name="imageTitle" required minlength="1" maxlength="64">
            <label for="imageText">Image Description:</label>
            <textarea rows="5" cols="50" id="imageText" name="imageText" required minlength="1" maxlength="512"></textarea>
            <label for="imageFile">Select Image:</label>
            <input type="file" id="imageFile" name="imageFile" accept=".jpg,.jpeg,.png,.webp" required>
            <label>Choose Albums:</label>
            <div class="albumsCheckbox">
                <!-- User's Personal Album -->
                <label>
                    <input type="checkbox" name="albumSelect" value="${data.userAlbumId}" checked disabled>
                    <span>@${data.user.username}</span>
                </label>
                <!-- Other Albums -->
                ${data.myAlbums
        .map((album) =>
            album.albumId !== data.userAlbumId
                ? `
                    <label>
                        <input type="checkbox" name="albumSelect" value="${album.albumId}">
                        <span>${album.albumTitle}</span>
                    </label>
                    `
                : ""
        )
        .join("")}
            </div>
            <button type="submit" id="addImageButton">Add</button>
        </form>
        <div class="error-message hidden" id="addImageError"></div>
        <div class="success-message hidden" id="addImageSuccess"></div>
    </div>
    `;
    // Profile Content
    html += `
    <div class="content" id="profileContent">
        <h2>Profile</h2>
        <div class="profile-container">
            <p><strong>Email:</strong> ${data.user.email}</p>
            <p><strong>Username:</strong> ${data.user.username}</p>
            <p><strong>Number of Albums:</strong> ${data.userStats.numAlbums}</p>
            <p><strong>Number of Images:</strong> ${data.userStats.numImages}</p>
            <p><strong>Number of Comments:</strong> ${data.userStats.numComments}</p>
            <form id="logoutHomeForm">
                <button type="submit" id="logoutHomeButton">Logout</button>
            </form>
        </div>
    </div>
    `;
    return html;
}

async function loadAlbumPage(albumId, page) {
    const spa = document.getElementById("spa");
    spa.innerHTML = `<p>Loading...</p>`;
    try {
        const response = await fetch(`./album?albumId=${encodeURIComponent(albumId)}`);
        if (!response.ok) {
            const errorData = await response.json();
            if (errorData.redirect) {
                window.location.hash = errorData.redirect;
                return;
            } else {
                throw new Error(errorData.message || "Error loading album page. Please try again.");
            }
        }
        const data = await response.json();
        // Check page
        const maxPage = Math.ceil(data.album.images.length / data.pageSize) - 1;
        if (page > maxPage)
            page = 0;
        spa.innerHTML = buildAlbumHTML(data, page);
        initAlbumPageEventListeners();
        showSuccessMessage();
    } catch (error) {
        spa.innerHTML = `<p>${error.message}</p>`;
    }
}

function buildAlbumHTML(data, page) {
    if (!data || !data.album || !data.album.images || !data.pageSize) {
        return `<p>Error loading album page. Please try again.</p>`;
    }
    // Calculate navigation flags
    const hasPrevious = page > 0;
    const hasNext = (page + 1) * data.pageSize < data.album.images.length;
    // Start building the HTML
    let html = `
    <!-- Navbar -->
    <header class="albumpage-navbar">
        <nav class="navbar-content">
            <!-- Home page -->
            <form id="returnToHomeForm">
                <button type="submit" id="returnToHomeButton">Home</button>
            </form>
            <!-- Album Title -->
            <div class="navbar-title">${data.album.albumTitle}</div>
            <!-- Logout -->
            <form id="logoutAlbumForm">
                <button type="submit" id="logoutAlbumButton">Logout</button>
            </form>
        </nav>
    </header>
    <!-- Navigation section -->
    <div class="navigation">
        <div class="nav-placeholder previous">
            ${
        hasPrevious
            ? `<a href="#album?albumId=${data.album.albumId}&page=${page - 1}" class="nav-button">Previous</a>`
            : ""
    }
        </div>
        <div class="nav-placeholder next">
            ${
        hasNext
            ? `<a href="#album?albumId=${data.album.albumId}&page=${page + 1}" class="nav-button">Next</a>`
            : ""
    }
        </div>
    </div>
    <!-- Images container -->
    <div class="images-container">
    `;
    // Calculate the starting index for the current page
    const startIndex = page * data.pageSize;
    const endIndex = Math.min(startIndex + data.pageSize, data.album.images.length);
    let imagesOnPage = 0;
    // Display the images for the current page
    for (let i = startIndex; i < endIndex; i++) {
        const image = data.album.images[i];
        html += `
        <div class="image-cell">
            <img src="./uploads?imageId=${image.imageId}" alt="${image.imageTitle}" class="image-item">
            <div class="image-title">${image.imageTitle}</div>
        </div>
        `;
        imagesOnPage++;
    }
    // Add empty cells if there are less images than pageSize
    for (let i = imagesOnPage; i < data.pageSize; i++) {
        html += `
        <div class="image-cell">
            <div class="empty-cell"></div>
        </div>
        `;
    }
    // Close the images container
    html += `
    </div>
    `;
    return html;
}

/**
 * Displays success messages.
 * Messages are retrieved from sessionStorage and cleared after being shown.
 */
function showSuccessMessage() {
    // createAlbum
    const createAlbumSuccess = sessionStorage.getItem("createAlbumSuccess");
    if (createAlbumSuccess) {
        const successDiv = document.getElementById("createAlbumSuccess");
        if (successDiv) {
            successDiv.textContent = createAlbumSuccess;
            successDiv.classList.remove("hidden");
        }
        sessionStorage.removeItem("createAlbumSuccess");
    }
    // addImage
    const addImageSuccess = sessionStorage.getItem("addImageSuccess");
    if (addImageSuccess) {
        const successDiv = document.getElementById("addImageSuccess");
        if (successDiv) {
            successDiv.textContent = addImageSuccess;
            successDiv.classList.remove("hidden");
        }
        sessionStorage.removeItem("addImageSuccess");
    }
}