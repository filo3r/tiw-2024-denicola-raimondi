package it.polimi.tiw.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import it.polimi.tiw.dao.AlbumDAO;
import it.polimi.tiw.dao.CommentDAO;
import it.polimi.tiw.dao.ImageDAO;
import it.polimi.tiw.model.Comment;
import it.polimi.tiw.model.User;
import it.polimi.tiw.util.StringUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * ImageServlet handles HTTP POST requests related to individual images.
 * It provides functionalities for adding comments to images and validating
 * the relationship between images and albums.
 */
public class ImageServlet extends HttpServlet {

    /**
     * Unique identifier for Serializable class to ensure compatibility
     * during the deserialization process. Changing this value can cause
     * deserialization issues if there are any modifications to the class structure.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Gson instance for parsing and serializing JSON data.
     */
    private final Gson gson = new Gson();

    /**
     * Handles HTTP POST requests for actions related to the image page.
     * Validates the user's session, parses the request to identify the action,
     * and supports adding comments to images.
     * If the action is "addComment", the method processes the comment addition.
     * Otherwise, it redirects the user to the album page.
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @throws ServletException if an error occurs during request handling.
     * @throws IOException      if an I/O error occurs during request handling.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        // Set JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // Get user
        User user = (User) session.getAttribute("user");
        String username = user.getUsername();
        // Get image ID and album ID from request
        ArrayList<Integer> imageAndAlbumIds = null;
        try {
            imageAndAlbumIds = getImageAndAlbumIds(request, response);
            if (imageAndAlbumIds == null || imageAndAlbumIds.isEmpty() || imageAndAlbumIds.contains(-1)) {
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", null, response);
            return;
        }
        // Add Comment
        try {
            JsonObject jsonRequest = gson.fromJson(request.getReader(), JsonObject.class);
            String action = jsonRequest.get("action").getAsString();
            if ("addComment".equals(action))
                handleAddComment(jsonRequest, response, username, imageAndAlbumIds);
            else if ("deleteImage".equals(action))
                handleDeleteImage(response, username, imageAndAlbumIds);
            else
                response.sendRedirect(request.getContextPath() + "/spa#album?albumId=" + imageAndAlbumIds.get(1) + "&page=0");
        } catch (JsonSyntaxException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid JSON format. Please try again.", null, response);
            e.printStackTrace();
        }
    }

    /**
     * Retrieves and validates the image and album IDs from the request parameters.
     * Ensures that:
     * - The album ID is valid and exists in the database.
     * - The image ID is valid and exists in the database.
     * - The image belongs to the specified album.
     * If validation fails, the method sends an appropriate error response or
     * redirect to the client.
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @return a list containing the image ID and album ID if validation is successful,
     *         or null if validation fails.
     * @throws SQLException      if a database error occurs during validation.
     * @throws ServletException  if an error occurs during request handling.
     * @throws IOException       if an I/O error occurs during response handling.
     */
    private ArrayList<Integer> getImageAndAlbumIds(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        // Get imageId and albumId
        String imageIdParam = request.getParameter("imageId");
        String albumIdParam = request.getParameter("albumId");
        int imageId = -1;
        int albumId = -1;
        ArrayList<Integer> imageAndAlbumIds = new ArrayList<>();
        imageAndAlbumIds.add(imageId);
        imageAndAlbumIds.add(albumId);
        if (albumIdParam == null || albumIdParam.isEmpty()) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid album id.", "#home", response);
            return null;
        }
        try {
            albumId = Integer.parseInt(albumIdParam);
        } catch (NumberFormatException e) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid album id.", "#home", response);
            return null;
        }
        AlbumDAO albumDAO = new AlbumDAO();
        boolean albumExists = albumDAO.doesAlbumExist(albumId);
        if (albumExists) {
            imageAndAlbumIds.set(1, albumId);
        } else {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid album id.", "#home", response);
            return null;
        }
        if (imageIdParam == null || imageIdParam.isEmpty()) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid image id.", "#album?albumId=" + albumId + "&page=0", response);
            return null;
        }
        try {
            imageId = Integer.parseInt(imageIdParam);
        } catch (NumberFormatException e) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid image id.", "#album?albumId=" + albumId + "&page=0", response);
            return null;
        }
        ImageDAO imageDAO = new ImageDAO();
        boolean imageExists = imageDAO.doesImageExist(imageId);
        if (!imageExists) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid image id.", "#album?albumId=" + albumId + "&page=0", response);
            return null;
        }
        boolean imageBelongToAlbum = imageDAO.doesImageBelongToAlbum(imageId, albumId);
        if (imageBelongToAlbum) {
            imageAndAlbumIds.set(0, imageId);
        } else {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid image id.", "#album?albumId=" + albumId + "&page=0", response);
            return null;
        }
        return imageAndAlbumIds;
    }

    /**
     * Handles the addition of a comment to an image.
     * Validates the comment text and inserts the comment into the database if valid.
     * Sends an appropriate success or error response based on the outcome.
     * @param jsonRequest        the JSON object containing the request data.
     *                           Expected to contain "action" and "commentText" fields.
     * @param response           the HTTP response object.
     * @param username           the username of the logged-in user.
     * @param imageAndAlbumIds   the list containing the image ID and album ID.
     * @throws ServletException if an error occurs during request handling.
     * @throws IOException      if an I/O error occurs during response handling.
     */
    private void handleAddComment(JsonObject jsonRequest, HttpServletResponse response, String username, ArrayList<Integer> imageAndAlbumIds) throws ServletException, IOException {
        String commentText = jsonRequest.get("commentText").getAsString();
        if (!StringUtil.isValidText(commentText)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid comment text.", null, response);
            return;
        }
        Comment comment = new Comment(imageAndAlbumIds.get(0), username, commentText);
        try {
            ImageDAO imageDAO = new ImageDAO();
            boolean imageExists = imageDAO.doesImageExist(imageAndAlbumIds.get(0));
            if (!imageExists)
                return;
            CommentDAO commentDAO = new CommentDAO();
            boolean success = commentDAO.addComment(comment);
            if (success) {
                sendSuccessResponse(HttpServletResponse.SC_OK, "Comment added successfully.", "#album?albumId=" + imageAndAlbumIds.get(1) + "&page=0", response);
            } else {
                sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", null, response);
            }
        } catch (SQLException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", null, response);
            e.printStackTrace();
        }
    }

    /**
     * Handles the deletion of an image.
     * Validates that the image exists, belongs to the logged-in user, and is part of the specified album.
     * Deletes the image from both the database and the disk storage if validation passes.
     * Sends an appropriate success or error response based on the outcome.
     * @param response         the HTTP response object.
     * @param username         the username of the logged-in user.
     * @param imageAndAlbumIds a list containing the image ID and album ID.
     * @throws ServletException if an error occurs during request handling.
     * @throws IOException      if an I/O error occurs during response handling.
     */
    private void handleDeleteImage(HttpServletResponse response, String username, ArrayList<Integer> imageAndAlbumIds) throws ServletException, IOException {
        try {
            ImageDAO imageDAO = new ImageDAO();
            boolean imageExists = imageDAO.doesImageExist(imageAndAlbumIds.get(0));
            if (!imageExists)
                return;
            boolean imageBelongToUser = imageDAO.doesImageBelongToUser(imageAndAlbumIds.get(0), username);
            if (!imageBelongToUser)
                return;
            String imagePathString = imageDAO.getImagePathById(imageAndAlbumIds.get(0));
            if (imagePathString == null)
                return;
            boolean successDatabase = imageDAO.deleteImageById(imageAndAlbumIds.get(0));
            if (successDatabase) {
                deleteImageFromDisk(imagePathString);
                sendSuccessResponse(HttpServletResponse.SC_OK, "Image deleted successfully.", "#album?albumId=" + imageAndAlbumIds.get(1) + "&page=0", response);
            } else {
                sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", null, response);
            }
        } catch (SQLException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", null, response);
            e.printStackTrace();
        }
    }

    /**
     * Deletes an image file from the server's disk storage.
     * @param imagePathString the path of the image file to be deleted.
     * @return true if the file was successfully deleted, false otherwise.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs.
     */
    private boolean deleteImageFromDisk(String imagePathString) throws ServletException, IOException {
        // Get the image path
        Path imagePath = Paths.get(imagePathString);
        try {
            // Attempt to delete the file
            if (Files.exists(imagePath)) {
                Files.delete(imagePath);
                // Verify the file has been deleted
                if (Files.exists(imagePath))
                    return false;
            } else {
                return false;
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends an error response with the specified status, message, and redirect URL.
     * @param status   the HTTP status code
     * @param message  the error message
     * @param redirect the redirect URL
     * @param response the HTTP response object
     * @throws IOException if an I/O error occurs during response writing
     */
    private void sendErrorResponse(int status, String message, String redirect, HttpServletResponse response) throws IOException {
        JsonObject jsonObject = new JsonObject();
        if (message != null)
            jsonObject.addProperty("message", message);
        if (redirect != null)
            jsonObject.addProperty("redirect", redirect);
        response.setStatus(status);
        response.getWriter().write(gson.toJson(jsonObject));
    }

    /**
     * Sends a success response with the specified status, message, and redirect URL.
     * @param status    the HTTP status code.
     * @param message   the success message.
     * @param redirect  the redirect URL.
     * @param response  the HTTP response object.
     * @throws IOException if an I/O error occurs during response writing.
     */
    private void sendSuccessResponse(int status, String message, String redirect, HttpServletResponse response) throws IOException {
        JsonObject jsonObject = new JsonObject();
        if (message != null)
            jsonObject.addProperty("message", message);
        if (redirect != null)
            jsonObject.addProperty("redirect", redirect);
        response.setStatus(status);
        response.getWriter().write(gson.toJson(jsonObject));
    }

}