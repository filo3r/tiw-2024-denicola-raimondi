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
 * and handles actions such as logout and saving image orders.
 */
public class AlbumServlet extends HttpServlet {

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
     * The default size for paginated results.
     */
    private static final int PAGE_SIZE = 5;

    /**
     * Handles GET requests to the album page.
     * Verifies user authentication, loads album details, and sends the album data in JSON format.
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
        // Render Album Page
        try {
            Map<String, Object> albumData = renderAlbumPage(username, albumId);
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
     * Handles POST requests for actions like logout and saving image orders.
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
        // Return To Home or Logout or Save Order
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
     * @param request the HTTP request object.
     * @return the valid album ID, or -1 if the ID is invalid or the album does not exist.
     * @throws SQLException if a database access error occurs.
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
     * @param username the username of the current user.
     * @param albumId  the ID of the album to load.
     * @return a map containing album details and images.
     * @throws SQLException if a database access error occurs.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private Map<String, Object> renderAlbumPage(String username, int albumId) throws SQLException, ServletException, IOException {
        // Create a map to hold all album data that will be sent as a response
        Map<String, Object> albumData = new HashMap<>();
        // Load album details from the database
        Album album = handleLoadAlbumData(albumId);
        if (album == null)
            return Collections.emptyMap();
        // Add basic album details to the map
        albumData.put("albumId", album.getAlbumId());
        albumData.put("albumCreator", album.getAlbumCreator());
        albumData.put("albumTitle", album.getAlbumTitle());
        albumData.put("albumDate", album.getAlbumDate());
        // Load all images and their associated comments for the specified album
        List<Map<String, Object>> imagesList = handleLoadAlbumImages(username, albumId);
        // Add the images list to the album data map
        albumData.put("images", imagesList);
        return albumData;
    }

    /**
     * Loads album details for the specified album ID.
     * @param albumId the ID of the album to load.
     * @return the Album object or null if not found.
     * @throws SQLException if a database access error occurs.
     */
    private Album handleLoadAlbumData(int albumId) throws SQLException {
        AlbumDAO albumDAO = new AlbumDAO();
        return albumDAO.getAlbumById(albumId);
    }

    /**
     * Loads and optionally sorts the images for the specified album based on user preferences.
     * @param username the username of the current user.
     * @param albumId  the ID of the album whose images are to be loaded.
     * @return a list of maps representing the images and their comments.
     * @throws SQLException if a database access error occurs.
     */
    private List<Map<String, Object>> handleLoadAlbumImages(String username, int albumId) throws SQLException {
        AlbumDAO albumDAO = new AlbumDAO();
        UserImageOrderDAO userImageOrderDAO = new UserImageOrderDAO();
        // Retrieve all images and their associated comments for the specified album
        Map<Image, List<Comment>> imagesWithComments = albumDAO.getImagesWithCommentsByAlbumId(albumId);
        // Check if the user has a saved custom image order for the album
        if (userImageOrderDAO.userHasImagesOrderForAlbum(username, albumId)) {
            // Retrieve the user's custom order (list of image IDs)
            List<Integer> userOrder = userImageOrderDAO.getUserImagesOrderForAlbum(username, albumId);
            // Map image IDs to their corresponding image and comments entries for sorting
            Map<Integer, Map.Entry<Image, List<Comment>>> imageIdToEntry = mapImageIdToEntry(imagesWithComments);
            // Sort images based on the custom user order
            return orderImages(userOrder, imageIdToEntry);
        } else {
            // If no custom order exists, return images in their default order
            return defaultImageOrder(imagesWithComments);
        }
    }

    /**
     * Maps image IDs to their corresponding image and comment entries.
     * @param imagesWithComments a map of images and their comments.
     * @return a map from image IDs to their corresponding entries.
     */
    private Map<Integer, Map.Entry<Image, List<Comment>>> mapImageIdToEntry(Map<Image, List<Comment>> imagesWithComments) {
        // Initialize a map to store image IDs as keys and their corresponding entries (image and comments) as values
        Map<Integer, Map.Entry<Image, List<Comment>>> imageIdToEntry = new HashMap<>();
        // Iterate over each entry in the input map
        for (Map.Entry<Image, List<Comment>> entry : imagesWithComments.entrySet()) {
            // Use the image ID as the key
            // Map the image ID to the corresponding entry (image and its list of comments)
            imageIdToEntry.put(entry.getKey().getImageId(), entry);
        }
        return imageIdToEntry;
    }

    /**
     * Orders images based on a user-defined order, appending remaining images in their original order.
     * @param userOrder          a list of image IDs defining the user order.
     * @param imageIdToEntry     a map from image IDs to their corresponding image and comments.
     * @return a list of maps representing ordered images.
     */
    private List<Map<String, Object>> orderImages(List<Integer> userOrder, Map<Integer, Map.Entry<Image, List<Comment>>> imageIdToEntry) {
        // Initialize a list to hold the images in the desired order
        List<Map<String, Object>> orderedImagesList = new ArrayList<>();
        // Add images based on the user's custom order
        for (int imageId : userOrder) {
            // Check if the image ID exists in the map
            if (imageIdToEntry.containsKey(imageId)) {
                // Retrieve the image and its comments
                Map.Entry<Image, List<Comment>> entry = imageIdToEntry.get(imageId);
                Image image = entry.getKey();
                List<Comment> comments = entry.getValue();
                // Convert the image and its comments into a serializable map and add to the ordered list
                orderedImagesList.add(createImageMap(image, comments));
                // Remove the image ID from the map to avoid duplicates
                imageIdToEntry.remove(imageId);
            }
        }
        // Append any remaining images (not included in the user's custom order) in their original order
        for (Map.Entry<Image, List<Comment>> entry : imageIdToEntry.values()) {
            Image image = entry.getKey();
            List<Comment> comments = entry.getValue();
            orderedImagesList.add(createImageMap(image, comments));
        }
        return orderedImagesList;
    }

    /**
     * Creates a default order for images.
     * @param imagesWithComments a map of images and their comments.
     * @return a list of maps representing images in their original order.
     */
    private List<Map<String, Object>> defaultImageOrder(Map<Image, List<Comment>> imagesWithComments) {
        // Initialize a list to hold the images in their default order
        List<Map<String, Object>> imagesList = new ArrayList<>();
        // Iterate over each entry in the map of images and their comments
        for (Map.Entry<Image, List<Comment>> entry : imagesWithComments.entrySet()) {
            // Extract the image and its associated comments
            Image image = entry.getKey();
            List<Comment> comments = entry.getValue();
            // Convert the image and its comments into a serializable map format and add to the list
            imagesList.add(createImageMap(image, comments));
        }
        return imagesList;
    }

    /**
     * Converts an image and its comments into a serializable map.
     * @param image    the image object.
     * @param comments the list of comments associated with the image.
     * @return a map representing the image and its comments.
     */
    private Map<String, Object> createImageMap(Image image, List<Comment> comments) {
        // Initialize a map to hold the serialized data for the image and its comments
        Map<String, Object> imageMap = new HashMap<>();
        // Add basic image details to the map
        imageMap.put("imageId", image.getImageId());
        imageMap.put("imageUploader", image.getImageUploader());
        imageMap.put("imageTitle", image.getImageTitle());
        imageMap.put("imageDate", image.getImageDate());
        imageMap.put("imageText", image.getImageText());
        imageMap.put("imagePath", image.getImagePath());
        // Convert the list of comments into a serializable structure and add it to the map
        imageMap.put("comments", handleLoadImageComments(comments));
        return imageMap;
    }

    /**
     * Converts a list of comments into a serializable structure.
     * @param comments the list of comments to be converted.
     * @return a list of maps representing the comments.
     */
    private List<Map<String, Object>> handleLoadImageComments(List<Comment> comments) {
        // Initialize a list to hold the serialized data for each comment
        List<Map<String, Object>> commentsData = new ArrayList<>();
        // Iterate over the list of comments
        for (Comment comment : comments) {
            // Create a map to store serialized data for the current comment
            Map<String, Object> commentMap = new HashMap<>();
            // Add comment details to the map
            commentMap.put("commentId", comment.getCommentId());
            commentMap.put("imageId", comment.getImageId());
            commentMap.put("commentAuthor", comment.getCommentAuthor());
            commentMap.put("commentText", comment.getCommentText());
            // Add the serialized comment map to the list
            commentsData.add(commentMap);
        }
        return commentsData;
    }

    /**
     * Saves a custom order of images for the specified album.
     * @param jsonRequest the JSON request containing the sorted image IDs.
     * @param response    the HTTP response object.
     * @param username    the username of the current user.
     * @param albumId     the ID of the album.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException if an I/O error occurs during processing.
     */
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