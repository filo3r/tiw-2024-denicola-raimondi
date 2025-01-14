// Import StringUtil class
import StringUtil from "./StringUtil.js";
// Import spa.js
import { forceHashChange } from "./spa.js";

/**
 * Initializes event listeners for the image page.
 * @param {Object} data - The data object containing album and image information.
 */
export function initImagePageEventListeners(data) {
    // Select all thumbnails
    const imageItems = document.querySelectorAll('.image-item');
    // Add the "mouseover" and "mouseout" events to each thumbnail
    imageItems.forEach(img => {
        img.addEventListener("mouseover", () => {
            // Retrieve image index
            const imageIndex = parseInt(img.getAttribute("data-image-index"), 10);
            // Opens the modal with the image data
            openImageModal(data, imageIndex);
        });
    });
    // Close the modal by clicking on close button
    const closeButton = document.getElementById("closeModalButton");
    if (closeButton) {
        closeButton.addEventListener("click", () => {
            closeImageModal();
        });
    }
    // Add comment
    const addCommentForm = document.getElementById("addCommentForm");
    if (addCommentForm) {
        addCommentForm.addEventListener("submit", addComment)
    }
}

/**
 * Opens the modal with the data for a specific image.
 * @param {Object} data - The data object containing album and image information.
 * @param {number} imageIndex - The index of the image to display in the modal.
 */
function openImageModal(data, imageIndex) {
    // Retrieve modal
    const modal = document.getElementById("imageModal");
    if (!modal)
        return;
    // Retrieve image
    const image = data.album.images[imageIndex];
    if (!image)
        return;
    // Reset error or success messages
    const errorDiv = document.getElementById("addCommentError");
    const successDiv = document.getElementById("addCommentSuccess");
    errorDiv.textContent = "";
    successDiv.textContent = "";
    errorDiv.classList.add("hidden");
    successDiv.classList.add("hidden");
    // Set the modal fields
    const modalFullImage = document.getElementById("modalFullImage");
    modalFullImage.src = `./uploads?imageId=${image.imageId}`;
    document.getElementById("modalImageTitle").textContent = image.imageTitle;
    document.getElementById("imageTitle").textContent = image.imageTitle;
    document.getElementById("imageDescription").textContent = image.imageText;
    document.getElementById("imageUploader").textContent = image.imageUploader;
    document.getElementById("imageDate").textContent = image.imageDate;
    // Comments
    const commentsList = document.getElementById("commentsList");
    commentsList.innerHTML = '';
    if (image.comments && image.comments.length > 0) {
        image.comments.forEach(comment => {
            const li = document.createElement("li");
            li.textContent = `${comment.commentAuthor}: ${comment.commentText}`;
            commentsList.appendChild(li);
        });
    }
    // Adds listener on add comment form
    const addCommentForm = document.getElementById("addCommentForm");
    if (addCommentForm) {
        // Remove any previous listeners
        addCommentForm.replaceWith(addCommentForm.cloneNode(true));
        const newAddCommentForm = document.getElementById("addCommentForm");
        // Save dataset
        newAddCommentForm.dataset.albumId = data.album.albumId;
        newAddCommentForm.dataset.imageId = image.imageId;
        // Listener
        newAddCommentForm.addEventListener("submit", addComment);
    }
    // Show modal
    modal.style.display = "block";
}

/**
 * Closes the image modal.
 */
function closeImageModal() {
    const modal = document.getElementById("imageModal");
    if (!modal)
        return;
    modal.style.display = "none";
}

/**
 * Handles adding a comment to an image.
 * @param {Event} event - The event triggered by submitting the comment form.
 */
async function addComment(event) {
    event.preventDefault();
    // Retrieve the <form> element that triggered the event and read the dataset
    const form = event.currentTarget;
    const albumId = form.dataset.albumId;
    const imageId = form.dataset.imageId;
    // Comment text
    const commentText = document.getElementById("commentText").value.trim();
    // Reset errors
    const errorDiv = document.getElementById("addCommentError");
    const successDiv = document.getElementById("addCommentSuccess");
    errorDiv.textContent = "";
    successDiv.textContent = "";
    errorDiv.classList.add("hidden");
    successDiv.classList.add("hidden");
    // Check comment
    if (!StringUtil.isValidText(commentText)) {
        errorDiv.textContent = "Invalid comment text.";
        errorDiv.classList.remove("hidden");
        return;
    }
    // Send data to server
    try {
        const response = await fetch(`./image?albumId=${encodeURIComponent(albumId)}&imageId=${encodeURIComponent(imageId)}`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({action: "addComment", commentText: commentText}),
        });
        const result = await response.json();
        if (response.ok) {
            if (result.message) {
                sessionStorage.setItem("addCommentSuccess", result.message);
            }
            if (result.redirect) {
                forceHashChange(result.redirect);
            }
        } else {
            errorDiv.textContent = result.message || "Error adding comment.";
            errorDiv.classList.remove("hidden");
        }
    } catch (error) {
        errorDiv.textContent = "Internal server error. Please try again.";
        errorDiv.classList.remove("hidden");
    }
}