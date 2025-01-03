package it.polimi.tiw.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import it.polimi.tiw.dao.AlbumDAO;
import it.polimi.tiw.dao.UserImageOrderDAO;
import it.polimi.tiw.model.Album;
import it.polimi.tiw.model.Comment;
import it.polimi.tiw.model.Image;
import it.polimi.tiw.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Servlet responsible for handling requests related to albums.
 * This class manages user session validation, retrieves album details and images,
 * and renders the appropriate Thymeleaf templates for displaying album pages.
 * It also handles actions such as logout and redirects users based on their actions or
 * errors encountered during processing.
 */
public class AlbumServlet extends HttpServlet {

    /**
     * Unique identifier for Serializable class to ensure compatibility
     * during the deserialization process. Changing this value can cause
     * deserialization issues if there are any modifications to the class structure.
     */
    private static final long serialVersionUID = 1L;

    private final Gson gson = new Gson();

    private static final int PAGE_SIZE = 5;

    /**
     * Handles GET requests to the album page.
     * Verifies user authentication and loads album details.
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        // Set JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // Get album ID from request
        int albumId = -1;
        try {
            albumId = getAlbumId(request);
            if (albumId == -1) {
                sendErrorRedirect(HttpServletResponse.SC_BAD_REQUEST, "Invalid album id.", "#home", response);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorRedirect(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", "#home", response);
        }
        // Render Album Page
        try {
            Map<String, Object> albumData = renderAlbumPage(request, albumId);
            if (albumData == null || albumData.isEmpty()) {
                sendErrorRedirect(HttpServletResponse.SC_BAD_REQUEST, "Invalid album id.", "#home", response);
                return;
            }
            // Page Size
            int pageSize = PAGE_SIZE;
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("pageSize", pageSize);
            jsonObject.add("album", gson.toJsonTree(albumData));
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(jsonObject.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please reload the page.", response);
        }
    }

    /**
     * Handles POST requests, primarily for logout functionality.
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
        // Get album ID from request
        int albumId = -1;
        try {
            albumId = getAlbumId(request);
            if (albumId == -1) {
                sendErrorRedirect(HttpServletResponse.SC_BAD_REQUEST, "Invalid album id.", "#home", response);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorRedirect(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", "#home", response);
        }
        // Return To Home or Logout
        try {
            JsonObject jsonRequest = gson.fromJson(request.getReader(), JsonObject.class);
            String action = jsonRequest.get("action").getAsString();
            if ("returnToHome".equals(action))
                sendSuccessResponse(HttpServletResponse.SC_OK, "Back to home.", "#home", response);
            else if ("logoutAlbum".equals(action))
                handleLogout(request, response);
            else if ("saveOrder".equals(action))
                handleSaveImagesOrder(jsonRequest, response, username, albumId);
            else
                response.sendRedirect(request.getContextPath() + "/spa#home");
        } catch (JsonSyntaxException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.", response);
            e.printStackTrace();
        }
    }

    /**
     * Retrieves and validates the album ID from the request.
     * Ensures the album ID is present, correctly formatted, and exists in the database.
     * Displays an error page if validation fails or a database error occurs.
     * @param request    the HTTP request object.
     * @return the valid album ID, or -1 if the ID is invalid or the album does not exist.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private int getAlbumId(HttpServletRequest request) throws SQLException, ServletException, IOException {
        // Get albumId parameter
        String albumIdParam = request.getParameter("albumId");
        int albumId = -1;
        if (albumIdParam == null || albumIdParam.isEmpty()) {
            return -1;
        }
        try {
            albumId = Integer.parseInt(albumIdParam);
        } catch (NumberFormatException e) {
            return -1;
        }
        // Check if the album exists
        AlbumDAO albumDAO = new AlbumDAO();
        boolean exists = albumDAO.doesAlbumExist(albumId);
        if (exists)
            return albumId;
        else
            return -1;
    }

    /**
     * Renders the album page by loading album details and images.
     * Handles any database errors by setting error variables in the web context.
     * @param request    the HTTP request object
     * @param albumId    the ID of the album to load and display.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private Map<String, Object> renderAlbumPage(HttpServletRequest request, int albumId) throws SQLException, ServletException, IOException {
        Map<String, Object> albumData = new HashMap<>();
        // Album
        Album album = handleLoadAlbumData(albumId);
        if (album == null)
            return Collections.emptyMap();
        albumData.put("albumId", album.getAlbumId());
        albumData.put("albumCreator", album.getAlbumCreator());
        albumData.put("albumTitle", album.getAlbumTitle());
        albumData.put("albumDate", album.getAlbumDate());
        // Images with comments
        List<Map<String, Object>> imagesList = handleLoadAlbumImages(albumId);
        albumData.put("images", imagesList);
        return albumData;
    }

    /**
     * Loads album details for the specified album ID and sets them in the web context.
     * @param albumId    the ID of the album to load.
     * @throws SQLException if a database access error occurs while retrieving the album data.
     */
    private Album handleLoadAlbumData(int albumId) throws SQLException {
        AlbumDAO albumDAO = new AlbumDAO();
        return albumDAO.getAlbumById(albumId);
    }

    /**
     * Loads and paginates the images for the specified album.
     * Retrieves all images associated with the album ID, calculates the current page,
     * and sets the paginated images along with pagination details in the web context.
     * @param albumId    the ID of the album whose images are to be loaded.
     * @throws SQLException if a database access error occurs while retrieving the images.
     */
    private List<Map<String, Object>> handleLoadAlbumImages(int albumId) throws SQLException {
        // Get Image->List<Comment> map from DAO
        AlbumDAO albumDAO = new AlbumDAO();
        Map<Image, List<Comment>> imagesWithComments = albumDAO.getImagesWithCommentsByAlbumId(albumId);
        // Create a list of maps, where each map represents an image + its list of comments
        List<Map<String, Object>> imagesList = new ArrayList<>();
        for (Map.Entry<Image, List<Comment>> entry : imagesWithComments.entrySet()) {
            Image image = entry.getKey();
            List<Comment> comments = entry.getValue();
            // Single image
            Map<String, Object> imageMap = new HashMap<>();
            imageMap.put("imageId", image.getImageId());
            imageMap.put("imageUploader", image.getImageUploader());
            imageMap.put("imageTitle", image.getImageTitle());
            imageMap.put("imageDate", image.getImageDate());
            imageMap.put("imageText", image.getImageText());
            imageMap.put("imagePath", image.getImagePath());
            // Convert the list of comments into a “serializable” structure
            List<Map<String, Object>> commentsData = new ArrayList<>();
            for (Comment comment : comments) {
                Map<String, Object> commentMap = new HashMap<>();
                commentMap.put("commentId", comment.getCommentId());
                commentMap.put("imageId", comment.getImageId());
                commentMap.put("commentAuthor", comment.getCommentAuthor());
                commentMap.put("commentText", comment.getCommentText());
                commentsData.add(commentMap);
            }
            // Insert the list of comments into the image map
            imageMap.put("comments", commentsData);
            // Add map to global list
            imagesList.add(imageMap);
        }
        return imagesList;
    }

    /**
     * Handles the user logout process by invalidating the session and redirecting to the login page.
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null)
            session.invalidate();
        sendSuccessResponse(HttpServletResponse.SC_OK, "Logout successful.", request.getContextPath() + "/", response);
    }

    private void handleSaveImagesOrder(JsonObject jsonRequest, HttpServletResponse response, String username, int albumId) throws ServletException, IOException {
        // Get sorted image ids
        ArrayList<Integer> sortedImageIds;
        try {
            sortedImageIds = gson.fromJson(jsonRequest.getAsJsonArray("sortedImageIds"), new TypeToken<ArrayList<Integer>>() {}.getType());
        } catch (JsonSyntaxException e) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid sorted image list.", response);
            return;
        }
        // Check sortedImageIds
        if (sortedImageIds == null || sortedImageIds.isEmpty()) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid sorted image list.", response);
            return;
        }
        // Remove duplicates
        LinkedHashSet<Integer> uniqueIds = new LinkedHashSet<>(sortedImageIds);
        sortedImageIds = new ArrayList<>(uniqueIds);
        // Save on database
        try {
            UserImageOrderDAO userImageOrderDAO = new UserImageOrderDAO();
            // Check if there is already a custom sort
            if (userImageOrderDAO.userHasImagesOrderForAlbum(username, albumId)) {
                // Delete existing custom sorting
                if (!userImageOrderDAO.deleteUserImagesOrderForAlbum(username, albumId)) {
                    sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please reload the page.", response);
                    return;
                }
            }
            if (!userImageOrderDAO.saveUserImagesOrderForAlbum(username, albumId, sortedImageIds))
                sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please reload the page.", response);
            else
                sendSuccessResponse(HttpServletResponse.SC_OK, "Custom sorting successfully saved.", "#album?albumId=" + albumId + "&page=0", response);
        } catch (SQLException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please reload the page.", response);
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