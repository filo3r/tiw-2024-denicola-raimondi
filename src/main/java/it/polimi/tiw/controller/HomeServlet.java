package it.polimi.tiw.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import it.polimi.tiw.dao.AlbumDAO;
import it.polimi.tiw.dao.CommentDAO;
import it.polimi.tiw.dao.ImageDAO;
import it.polimi.tiw.model.Album;
import it.polimi.tiw.model.Image;
import it.polimi.tiw.model.User;
import it.polimi.tiw.util.MimeDetector;
import it.polimi.tiw.util.StringUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HomeServlet handles requests for the home page of the application.
 * It manages album loading, album creation, and adding images to albums.
 */
public class HomeServlet extends HttpServlet {

    /**
     * Unique identifier for the Serializable class to ensure compatibility
     * during the deserialization process. Changing this value can cause
     * deserialization issues if there are any modifications to the class structure.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Gson instance for parsing and serializing JSON data.
     */
    private final Gson gson = new Gson();

    /**
     * Handles GET requests to the servlet.
     * Verifies user authentication and loads user albums for display on the home page.
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @throws ServletException if an error occurs during request processing.
     * @throws IOException      if an I/O error occurs during request processing.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        // Check the request type (HTML or JSON)
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            // Set JSON
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            // Get user
            User user = (User) session.getAttribute("user");
            String username = user.getUsername();
            // Render Home Page
            try {
                Map<String, Object> homeData = renderHomePage(request, user);
                String json = gson.toJson(homeData);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(json);
            } catch (SQLException e) {
                e.printStackTrace();
                sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please reload the page.", response);
            }
        } else {
            // Render the SPA page
            request.getRequestDispatcher("/WEB-INF/view/spa.html").forward(request, response);
        }
    }

    /**
     * Handles POST requests to the servlet.
     * Processes actions such as album creation, adding images, and logging out.
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @throws ServletException if an error occurs during request processing.
     * @throws IOException      if an I/O error occurs during request processing.
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
        // Create Album or Add Photo or Logout
        try {
            JsonObject jsonRequest = gson.fromJson(request.getReader(), JsonObject.class);
            String action = jsonRequest.get("action").getAsString();
            if ("createAlbum".equals(action))
                handleCreateAlbum(jsonRequest, request, response, username);
            else if ("addImage".equals(action))
                handleAddImage(jsonRequest, request, response, username);
            else if ("logoutHome".equals(action))
                handleLogout(request, response);
            else
                response.sendRedirect(request.getContextPath() + "/home");
        } catch (JsonSyntaxException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.", response);
            e.printStackTrace();
        }
    }

    /**
     * Renders the home page by loading user-specific albums and profile data.
     * @param request the HTTP request object.
     * @param user    the authenticated user.
     * @return a map containing data required to render the home page.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     * @throws SQLException     if a database access error occurs.
     */
    private Map<String, Object> renderHomePage(HttpServletRequest request, User user) throws ServletException, IOException, SQLException {
        Map<String, Object> homeData = new HashMap<>();
        // Albums
        Map<String, Object> albumsData = handleLoadAlbums(user.getUsername());
        homeData.putAll(albumsData);
        // Profile
        Map<String, Object> profileData = handleLoadProfile(user.getUsername());
        homeData.putAll(profileData);
        // User Info
        homeData.put("user", user);
        homeData.put("username", user.getUsername());
        return homeData;
    }

    /**
     * Loads albums for the specified user.
     * @param username the username of the authenticated user.
     * @return a map containing the user's albums and additional album-related data.
     * @throws SQLException     if a database access error occurs.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private Map<String, Object> handleLoadAlbums(String username) throws SQLException, ServletException, IOException {
        Map<String, Object> albumsData = new HashMap<>();
        AlbumDAO albumDAO = new AlbumDAO();
        ArrayList<Album> myAlbums = albumDAO.getMyAlbums(username);
        ArrayList<Album> otherAlbums = albumDAO.getOtherAlbums(username);
        int userAlbumId = albumDAO.getUserPersonalAlbumId(username);
        albumsData.put("myAlbums", myAlbums);
        albumsData.put("otherAlbums", otherAlbums);
        albumsData.put("userAlbumId", userAlbumId == -1 ? 0 : userAlbumId);
        return albumsData;
    }

    /**
     * Loads profile data, including statistics about albums, images, and comments.
     * @param username the username of the authenticated user.
     * @return a map containing the user's profile statistics.
     * @throws SQLException     if a database access error occurs.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private Map<String, Object> handleLoadProfile(String username) throws SQLException, ServletException, IOException {
        Map<String, Object> profileData = new HashMap<>();
        AlbumDAO albumDAO = new AlbumDAO();
        ImageDAO imageDAO = new ImageDAO();
        CommentDAO commentDAO = new CommentDAO();
        int numAlbums = albumDAO.getAlbumsCountByUser(username);
        int numImages = imageDAO.getImagesCountByUser(username);
        int numComments = commentDAO.getCommentsCountByUser(username);
        Map<String, Integer> userStats = new HashMap<>();
        userStats.put("numAlbums", numAlbums);
        userStats.put("numImages", numImages);
        userStats.put("numComments", numComments);
        profileData.put("userStats", userStats);
        return profileData;
    }

    /**
     * Handles the creation of a new album for the user.
     * @param jsonRequest the JSON request containing the album details.
     * @param request     the HTTP request object.
     * @param response    the HTTP response object.
     * @param username    the username of the authenticated user.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private void handleCreateAlbum(JsonObject jsonRequest, HttpServletRequest request, HttpServletResponse response, String username) throws ServletException, IOException {
        String albumTitle = jsonRequest.get("albumTitle").getAsString();
        if (!StringUtil.isValidTitle(albumTitle)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid album title.", response);
            return;
        }
        Album album = new Album(username, albumTitle);
        try {
            AlbumDAO albumDAO = new AlbumDAO();
            boolean success = albumDAO.createAlbum(album);
            if (success) {
                sendSuccessResponse(HttpServletResponse.SC_OK, "Album created successfully.", request.getContextPath() + "/home", response);
            } else {
                sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please reload the page.", response);
            }
        } catch (SQLException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please reload the page.", response);
            e.printStackTrace();
        }
    }

    /**
     * Handles the process of adding a new image to the user's albums.
     * @param jsonRequest the JSON request containing the image details.
     * @param request     the HTTP request object.
     * @param response    the HTTP response object.
     * @param username    the username of the authenticated user.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private void handleAddImage(JsonObject jsonRequest, HttpServletRequest request, HttpServletResponse response, String username) throws ServletException, IOException {
        ArrayList<String> imageStringParameters = getImageStringParameters(jsonRequest, response);
        if (imageStringParameters == null || imageStringParameters.isEmpty())
            return;
        ArrayList<Integer> selectedAlbums = getSelectedAlbums(jsonRequest, response, username);
        if (selectedAlbums == null || selectedAlbums.isEmpty())
            return;
        byte[] imageFile = getImageFile(jsonRequest, response);
        if (imageFile == null)
            return;
        String imageExtension = getImageExtension(imageFile, response);
        if (imageExtension == null)
            return;
        int imageId = insertImageIntoDatabase(response, username, imageStringParameters, selectedAlbums, imageExtension);
        if (imageId == -1)
            return;
        boolean imageSaved = saveImageIntoDisk(response, imageFile, imageId, imageExtension);
        if (!imageSaved)
            return;
        sendSuccessResponse(HttpServletResponse.SC_OK, "Image added successfully.", request.getContextPath() + "/home", response);
    }

    /**
     * Validates and retrieves the image title and description from the request.
     * @param jsonRequest the JSON request containing the image details.
     * @param response    the HTTP response object.
     * @return an ArrayList containing the image title and description if valid; otherwise, null.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private ArrayList<String> getImageStringParameters(JsonObject jsonRequest, HttpServletResponse response) throws ServletException, IOException {
        String imageTitle = jsonRequest.get("imageTitle").getAsString();
        String imageText = jsonRequest.get("imageText").getAsString();
        if (!StringUtil.isValidTitle(imageTitle)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid image title.", response);
            return null;
        }
        if (!StringUtil.isValidText(imageText)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid image description.", response);
            return null;
        }
        ArrayList<String> imageStringParameters = new ArrayList<>();
        imageStringParameters.add(imageTitle);
        imageStringParameters.add(imageText);
        return imageStringParameters;
    }

    /**
     * Retrieves and validates the selected album IDs from the request.
     * Ensures the albums belong to the user and includes the user's personal album if necessary.
     * @param jsonRequest the JSON request containing the album details.
     * @param response    the HTTP response object.
     * @param username    the username of the authenticated user.
     * @return an ArrayList of validated album IDs; otherwise, null.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private ArrayList<Integer> getSelectedAlbums(JsonObject jsonRequest, HttpServletResponse response, String username) throws ServletException, IOException {
        // Retrieve the array of selected albums
        if (!jsonRequest.has("albumSelect")) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid albums selected.", response);
            return null;
        }
        ArrayList<Integer> selectedAlbums = new ArrayList<>();
        for (var album : jsonRequest.getAsJsonArray("albumSelect")) {
            try {
                selectedAlbums.add(album.getAsInt());
            } catch (NumberFormatException e) {
                sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid albums selected.", response);
                return null;
            }
        }
        // Add personal album and check if albums belong to the user
        try {
            AlbumDAO albumDAO = new AlbumDAO();
            // Add @username album
            int userAlbumId = albumDAO.getUserPersonalAlbumId(username);
            // Create personal album if it doesn't exist
            if (userAlbumId == -1) {
                Album userAlbum = new Album(username, "@" + username);
                albumDAO.createAlbum(userAlbum);
                userAlbumId = albumDAO.getUserPersonalAlbumId(username);
            }
            selectedAlbums.add(userAlbumId);
            // Validate selected albums
            ArrayList<Integer> userAlbumsIds = albumDAO.getMyAlbumIds(username);
            for (Integer albumId : selectedAlbums) {
                if (!userAlbumsIds.contains(albumId)) {
                    sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid albums selected.", response);
                    return null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please reload the page.", response);
            return null;
        }
        // Remove duplicates and return the list
        return new ArrayList<>(selectedAlbums.stream().distinct().collect(Collectors.toList()));
    }

    /**
     * Decodes and validates the uploaded image file from the request.
     * @param jsonRequest the JSON request containing the image file.
     * @param response    the HTTP response object.
     * @return the image file as a byte array if valid; otherwise, null.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private byte[] getImageFile(JsonObject jsonRequest, HttpServletResponse response) throws ServletException, IOException {
        // Read base64 string
        if (!jsonRequest.has("imageFile")) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid image file.", response);
            return null;
        }
        String base64String = jsonRequest.get("imageFile").getAsString();
        if (base64String == null || base64String.isEmpty()) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid image file.", response);
            return null;
        }
        // Decode base64 string into byte array
        byte[] imageFile = null;
        try {
            imageFile = Base64.getDecoder().decode(base64String);
        } catch (IllegalArgumentException e) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid image file.", response);
            return null;
        }
        // Check imageFile
        if (imageFile == null || imageFile.length == 0) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid image file.", response);
            return null;
        }
        // Maximum size control (100 MB)
        if (imageFile.length > 1024L * 1024L * 100L) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid image file.", response);
            return null;
        }
        return imageFile;
    }

    /**
     * Validates the MIME type of the uploaded image and retrieves the file extension.
     * @param imageFile  the uploaded image as a byte array.
     * @param response   the HTTP response object.
     * @return the image file extension if valid; otherwise, null.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private String getImageExtension(byte[] imageFile, HttpServletResponse response) throws ServletException, IOException {
        // Detect MIME type
        String imageMimeType = null;
        imageMimeType = MimeDetector.detectMimeType(imageFile);
        if (imageMimeType == null) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid image file.", response);
            return null;
        }
        // Make sure it is one of the allowed MIMEs
        List<String> allowedMimeTypes = Arrays.asList("image/jpg", "image/jpeg", "image/png", "image/webp");
        if (!allowedMimeTypes.contains(imageMimeType)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid image file.", response);
            return null;
        }
        return MimeDetector.getExtension(imageMimeType);
    }

    /**
     * Inserts the image data into the database and associates it with selected albums.
     * @param response              the HTTP response object.
     * @param username              the username of the authenticated user.
     * @param imageStringParameters the list containing image title and description.
     * @param selectedAlbums        the list of selected album IDs.
     * @param imageExtension        the file extension of the image.
     * @return the image ID if successful; otherwise, -1.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private int insertImageIntoDatabase(HttpServletResponse response, String username, ArrayList<String> imageStringParameters, ArrayList<Integer> selectedAlbums, String imageExtension) throws ServletException, IOException {
        try {
            Image image = new Image(username, imageStringParameters.get(0), imageStringParameters.get(1));
            ImageDAO imageDAO = new ImageDAO();
            int imageId = imageDAO.addImage(image);
            if (imageId == -1) {
                sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Database error. Please reload the page.", response);
                return -1;
            }
            String uploadsPathString = getUploadsPath();
            if (uploadsPathString == null) {
                sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error. Please reload page.", response);
                return -1;
            }
            Path uploadsPath = Paths.get(uploadsPathString);
            Path imagePath = uploadsPath.resolve(imageId + imageExtension);
            boolean updatedPath = imageDAO.updateImagePath(imageId, imagePath.toString());
            if (!updatedPath) {
                sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please reload page.", response);
                return -1;
            }
            boolean imageIntoAlbums = imageDAO.addImageToAlbums(imageId, selectedAlbums);
            if (!imageIntoAlbums) {
                sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please reload page.", response);
                return -1;
            }
            return imageId;
        } catch (SQLException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. There may have been errors adding the image. Please reload page.", response);
            return -1;
        }
    }

    /**
     * Saves the uploaded image file to the server's disk storage.
     * @param response      the HTTP response object.
     * @param imageFile     the uploaded image as a byte array.
     * @param imageId       the ID of the image in the database.
     * @param imageExtension the file extension of the image.
     * @return true if the image is saved successfully; otherwise, false.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private boolean saveImageIntoDisk(HttpServletResponse response, byte[] imageFile, int imageId, String imageExtension) throws ServletException, IOException {
        // Destination directory to save images
        String uploadsPathString = getUploadsPath();
        if (uploadsPathString == null) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error. Please reload page.", response);
            return false;
        }
        // Create Path object from the loaded uploads path
        Path uploadsPath = Paths.get(uploadsPathString);
        // Create uploads directory (if it doesn't already exist)
        Files.createDirectories(uploadsPath); // thread-safe
        // Full path of the file to save
        Path imagePath = uploadsPath.resolve(imageId + imageExtension);
        // Save the file contents to the destination
        try {
            Files.write(imagePath, imageFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            // Verify that the file has been created
            if (!Files.exists(imagePath)) {
                sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error saving image to server. Please reload page.", response);
                return false;
            }
        } catch (IOException save) {
            // Rollback: delete the partial file, if it exists
            try {
                Files.deleteIfExists(imagePath);
            } catch (IOException delete) {
                delete.printStackTrace();
            }
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error saving image to server. Please reload page.", response);
            save.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Retrieves the uploads directory path from the configuration properties.
     * @return the uploads directory path as a String; otherwise, null if an error occurs.
     */
    private String getUploadsPath() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("/properties/uploads.properties")) {
            if (input == null) {
                System.err.println("Could not find uploads.properties file.");
                return null;
            }
            Properties properties = new Properties();
            properties.load(input);
            String uploadsPath = properties.getProperty("uploads.path");
            if (uploadsPath == null || uploadsPath.isEmpty()) {
                System.err.println("Error in uploads.properties file.");
                return null;
            }
            return uploadsPath;
        } catch (IOException e) {
            System.err.println("Error reading uploads.properties file: " + e.getMessage());
            return null;
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