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
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * ImageServlet handles requests related to individual images.
 * It provides functionalities to display images, add comments, delete images,
 * and navigate back to albums or the home page.
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
     * Supports actions such as adding comments, deleting images, and navigating.
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
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
            if (imageAndAlbumIds == null) {
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", response);
        }
        // Add Comment
        try {
            JsonObject jsonRequest = gson.fromJson(request.getReader(), JsonObject.class);
            String action = jsonRequest.get("action").getAsString();
            if ("addComment".equals(action))
                handleAddComment(jsonRequest, response, username, imageAndAlbumIds);
            else
                response.sendRedirect(request.getContextPath() + "/spa#album?albumId=" + imageAndAlbumIds.get(1) + "&page=0");
        } catch (JsonSyntaxException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error. Please try again.", response);
            e.printStackTrace();
        }
    }

    /**
     * Retrieves and validates the image and album IDs from the request parameters.
     * @param request    the HTTP request object.
     * @param response   the HTTP response object.
     * @return           a list containing the image ID and album ID.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs.
     */
    private ArrayList<Integer> getImageAndAlbumIds (HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        // Get imageId and albumId
        String imageIdParam = request.getParameter("imageId");
        String albumIdParam = request.getParameter("albumId");
        int imageId = -1;
        int albumId = -1;
        ArrayList<Integer> imageAndAlbumIds = new ArrayList<>();
        imageAndAlbumIds.add(imageId);
        imageAndAlbumIds.add(albumId);
        if (albumIdParam == null || albumIdParam.isEmpty()) {
            sendErrorRedirect(HttpServletResponse.SC_BAD_REQUEST, "Invalid album id.", "#home", response);
            return null;
        }
        try {
            albumId = Integer.parseInt(albumIdParam);
        } catch (NumberFormatException e) {
            sendErrorRedirect(HttpServletResponse.SC_BAD_REQUEST, "Invalid album id.", "#home", response);
            return null;
        }
        AlbumDAO albumDAO = new AlbumDAO();
        boolean albumExists = albumDAO.doesAlbumExist(albumId);
        if (albumExists) {
            imageAndAlbumIds.set(1, albumId);
        } else {
            sendErrorRedirect(HttpServletResponse.SC_BAD_REQUEST, "Invalid album id.", "#home", response);
            return null;
        }
        if (imageIdParam == null || imageIdParam.isEmpty()) {
            sendErrorRedirect(HttpServletResponse.SC_BAD_REQUEST, "Invalid image id.", "#album?albumId=" + albumId + "&page=0", response);
            return null;
        }
        try {
            imageId = Integer.parseInt(imageIdParam);
        } catch (NumberFormatException e) {
            sendErrorRedirect(HttpServletResponse.SC_BAD_REQUEST, "Invalid image id.", "#album?albumId=" + albumId + "&page=0", response);
            return null;
        }
        ImageDAO imageDAO = new ImageDAO();
        boolean imageExists = imageDAO.doesImageExist(imageId);
        if (!imageExists) {
            sendErrorRedirect(HttpServletResponse.SC_BAD_REQUEST, "Invalid image id.", "#album?albumId=" + albumId + "&page=0", response);
            return null;
        }
        boolean imageBelongToAlbum = imageDAO.doesImageBelongToAlbum(imageId, albumId);
        if (imageBelongToAlbum) {
            imageAndAlbumIds.set(0, imageId);
        } else {
            sendErrorRedirect(HttpServletResponse.SC_BAD_REQUEST, "Invalid image id.", "#album?albumId=" + albumId + "&page=0", response);
            return null;
        }
        return imageAndAlbumIds;
    }

    /**
     * Handles the addition of a comment to an image.
     * @param response          the HTTP response object.
     * @param username          the username of the logged-in user.
     * @param imageAndAlbumIds  the list containing the image ID and album ID.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs.
     */
    private void handleAddComment(JsonObject jsonRequest, HttpServletResponse response, String username, ArrayList<Integer> imageAndAlbumIds) throws ServletException, IOException {
        String commentText = jsonRequest.get("commentText").getAsString();
        if (!StringUtil.isValidText(commentText)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid comment text.", response);
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
                sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", response);
            }
        } catch (SQLException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", response);
            e.printStackTrace();
        }
    }

    /**
     * Sends an error response with the specified status and message.
     * @param status   the HTTP status code.
     * @param message  the error message.
     * @param response the HTTP response object.
     * @throws IOException if an I/O error occurs during response writing.
     */
    private void sendErrorResponse(int status, String message, HttpServletResponse response) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", message);
        response.setStatus(status);
        response.getWriter().write(gson.toJson(jsonObject));
    }

    /**
     * Sends an error response with the specified status, message, and redirect URL.
     * @param status   the HTTP status code
     * @param message  the error message
     * @param redirect the redirect URL
     * @param response the HTTP response object
     * @throws IOException if an I/O error occurs during response writing
     */
    private void sendErrorRedirect(int status, String message, String redirect, HttpServletResponse response) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", message);
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
        jsonObject.addProperty("message", message);
        jsonObject.addProperty("redirect", redirect);
        response.setStatus(status);
        response.getWriter().write(gson.toJson(jsonObject));
    }

}