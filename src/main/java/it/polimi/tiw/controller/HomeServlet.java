package it.polimi.tiw.controller;

import it.polimi.tiw.dao.AlbumDAO;
import it.polimi.tiw.dao.CommentDAO;
import it.polimi.tiw.dao.ImageDAO;
import it.polimi.tiw.model.Album;
import it.polimi.tiw.model.Image;
import it.polimi.tiw.model.User;
import it.polimi.tiw.util.StringUtil;
import it.polimi.tiw.util.ViewEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HomeServlet handles requests for the home page of the application.
 * It manages album loading, album creation, and adding images to albums.
 */
public class HomeServlet extends HttpServlet {

    /**
     * Unique identifier for Serializable class to ensure compatibility
     * during the deserialization process. Changing this value can cause
     * deserialization issues if there are any modifications to the class structure.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Template engine for rendering HTML templates
     */
    private TemplateEngine templateEngine;

    /**
     * Initializes the servlet and retrieves the TemplateEngine instance
     * from the ServletContext.
     * @throws ServletException if an error occurs during initialization.
     */
    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();
        this.templateEngine = ViewEngine.getTemplateEngine(servletContext);
    }

    /**
     * Handles GET requests to the servlet.
     * Verifies user authentication and loads user albums for display on the home page.
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
        // Get user
        User user = (User) session.getAttribute("user");
        String username = user.getUsername();
        // WebContext
        ServletContext servletContext = getServletContext();
        WebContext webContext = new WebContext(request, response, servletContext, request.getLocale());
        webContext.setVariable("user", user);
        // Show success messages
        showSuccessMessage(session, webContext);
        // Render page
        renderHomePage(request, response, webContext, username);
    }

    /**
     * Handles POST requests to the servlet.
     * Verifies user authentication and processes actions such as album creation and adding images.
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
        // Get user
        User user = (User) session.getAttribute("user");
        String username = user.getUsername();
        // WebContext
        ServletContext servletContext = getServletContext();
        WebContext webContext = new WebContext(request, response, servletContext, request.getLocale());
        webContext.setVariable("user", user);
        // Create Album or Add Photo or Logout
        String action = request.getParameter("action");
        if ("createAlbum".equals(action))
            handleCreateAlbum(request, response, webContext, username);
        else if ("addImage".equals(action))
            handleAddImage(request, response, webContext, username);
        else if ("logout".equals(action))
            handleLogout(request, response);
        else
            response.sendRedirect(request.getContextPath() + "/home");
    }

    /**
     * Renders the home page by loading user albums and profile data.
     * @param request     the HTTP request object.
     * @param response    the HTTP response object.
     * @param webContext  the Thymeleaf WebContext for rendering templates.
     * @param username    the username of the logged-in user.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private void renderHomePage(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username) throws ServletException, IOException {
        try {
            handleLoadAlbums(webContext, username);
            handleLoadProfile(request, webContext, username);
        } catch (SQLException e) {
            webContext.setVariable("myAlbums", null);
            webContext.setVariable("otherAlbums", null);
            webContext.setVariable("userStats", Map.of(
                    "numAlbums", "Error",
                    "numImages", "Error",
                    "numComments", "Error"
            ));
            webContext.setVariable("myAlbumsErrorMessage", "Database error. Please reload page.");
            webContext.setVariable("otherAlbumsErrorMessage", "Database error. Please reload page.");
            webContext.setVariable("createAlbumErrorMessage", "Database error. Please reload page.");
            webContext.setVariable("addImageErrorMessage", "Database error. Please reload page.");
            webContext.setVariable("profileErrorMessage", "Database error. Please reload page.");
            e.printStackTrace();
        }
        templateEngine.process("home.html", webContext, response.getWriter());
    }

    /**
     * Loads the albums associated with the user for display.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @param username   the username of the logged-in user.
     * @throws SQLException    if an error occurs while accessing the database.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private void handleLoadAlbums(WebContext webContext, String username) throws SQLException, ServletException, IOException {
        AlbumDAO albumDAO = new AlbumDAO();
        ArrayList<Album> myAlbums = albumDAO.getMyAlbums(username);
        ArrayList<Album> otherAlbums = albumDAO.getOtherAlbums(username);
        webContext.setVariable("myAlbums", myAlbums);
        webContext.setVariable("otherAlbums", otherAlbums);
    }

    /**
     * Loads the profile statistics for the user.
     * @param request    the HTTP request object.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @param username   the username of the logged-in user.
     * @throws SQLException    if an error occurs while accessing the database.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private void handleLoadProfile(HttpServletRequest request, WebContext webContext, String username) throws SQLException, ServletException, IOException {
        AlbumDAO albumDAO = new AlbumDAO();
        ImageDAO imageDAO = new ImageDAO();
        CommentDAO commentDAO = new CommentDAO();
        int numAlbums = albumDAO.getAlbumsCountByUser(username);
        int numImages = imageDAO.getImagesCountByUser(username);
        int numComments = commentDAO.getCommentsCountByUser(username);
        User user = (User) request.getSession().getAttribute("user");
        webContext.setVariable("user", user);
        webContext.setVariable("userStats", Map.of(
                "numAlbums", numAlbums,
                "numImages", numImages,
                "numComments", numComments
        ));
    }

    /**
     * Handles album creation for the logged-in user.
     * @param request    the HTTP request object.
     * @param response   the HTTP response object.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @param username   the username of the logged-in user.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private void handleCreateAlbum(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username) throws ServletException, IOException {
        String albumTitle = request.getParameter("albumTitle");
        if (!StringUtil.isValidTitle(albumTitle)) {
            showErrorPage("createAlbum", "Invalid album title.", request, response, webContext, username);
            return;
        }
        Album album = new Album(username, albumTitle);
        try {
            AlbumDAO albumDAO = new AlbumDAO();
            boolean success = albumDAO.createAlbum(album);
            if (success) {
                HttpSession session = request.getSession();
                session.setAttribute("createAlbumSuccessMessage", "Album created successfully.");
                response.sendRedirect(request.getContextPath() + "/home");
            } else {
                showErrorPage("createAlbum", "Database error. Please reload page.", request, response, webContext, username);
            }
        } catch (SQLException e) {
            showErrorPage("createAlbum", "Database error. Please reload page.", request, response, webContext, username);
            e.printStackTrace();
        }
    }

    /**
     * Handles the process of adding a new image to the user's albums.
     * Validates input data, saves the image to disk, updates the database, and provides user feedback.
     * @param request    the HTTP request object.
     * @param response   the HTTP response object.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @param username   the username of the logged-in user.
     * @throws ServletException if a servlet-specific error occurs.
     * @throws IOException      if an I/O error occurs.
     */
    private void handleAddImage(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username) throws ServletException, IOException {
        ArrayList<String> imageStringParameters = getImageStringParameters(request, response, webContext, username);
        if (imageStringParameters == null || imageStringParameters.isEmpty())
            return;
        ArrayList<Integer> selectedAlbums = getSelectedAlbums(request, response, webContext, username);
        if (selectedAlbums == null || selectedAlbums.isEmpty())
            return;
        Part imageFile = getImageFile(request, response, webContext, username);
        if (imageFile == null)
            return;
        String imageExtension = getImageExtension(request, response, webContext, username, imageFile);
        if (imageExtension == null)
            return;
        int imageId = insertImageIntoDatabase(request, response, webContext, username, imageStringParameters, selectedAlbums, imageExtension);
        if (imageId == -1)
            return;
        boolean imageSaved = saveImageIntoDisk(request, response, webContext, username, imageFile, imageId, imageExtension);
        if (!imageSaved)
            return;
        HttpSession session = request.getSession();
        session.setAttribute("addImageSuccessMessage", "Image added successfully.");
        response.sendRedirect(request.getContextPath() + "/home");
    }

    /**
     * Retrieves and validates the image title and description from the request.
     * @param request    the HTTP request object.
     * @param response   the HTTP response object.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @param username   the username of the logged-in user.
     * @return           an ArrayList containing the image title and description if valid; otherwise, null.
     * @throws ServletException if a servlet-specific error occurs.
     * @throws IOException      if an I/O error occurs.
     */
    private ArrayList<String> getImageStringParameters(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username) throws ServletException, IOException {
        String imageTitle = request.getParameter("imageTitle");
        String imageText = request.getParameter("imageText");
        ArrayList<String> imageStringParameters = new ArrayList<>();
        if (!StringUtil.isValidTitle(imageTitle)) {
            showErrorPage("addImage", "Invalid image title.", request, response, webContext, username);
            return null;
        }
        if (!StringUtil.isValidText(imageText)) {
            showErrorPage("addImage", "Invalid image description.", request, response, webContext, username);
            return null;
        }
        imageStringParameters.add(imageTitle);
        imageStringParameters.add(imageText);
        return imageStringParameters;
    }

    /**
     * Retrieves and validates the selected album IDs from the request.
     * Ensures the albums belong to the user and adds the user's personal album if it doesn't exist.
     * @param request    the HTTP request object.
     * @param response   the HTTP response object.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @param username   the username of the logged-in user.
     * @return           an ArrayList of validated album IDs; otherwise, null.
     * @throws ServletException if a servlet-specific error occurs.
     * @throws IOException      if an I/O error occurs.
     */
    private ArrayList<Integer> getSelectedAlbums(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username) throws ServletException, IOException {
        String[] selectedAlbumsStr = request.getParameterValues("albumSelect");
        ArrayList<Integer> selectedAlbums = new ArrayList<>();
        // Parse album IDs from the request
        if (selectedAlbumsStr != null) {
            for (String albumIdStr : selectedAlbumsStr) {
                try {
                    int albumIdInt = Integer.parseInt(albumIdStr);
                    selectedAlbums.add(albumIdInt);
                } catch (NumberFormatException e) {
                    showErrorPage("addImage", "Invalid albums selected.", request, response, webContext, username);
                    return null;
                }
            }
        }
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
                    showErrorPage("addImage", "Invalid albums selected.", request, response, webContext, username);
                    return null;
                }
            }
        } catch (SQLException e) {
            showErrorPage("addImage", "Database error. Please reload page.", request, response, webContext, username);
            e.printStackTrace();
            return null;
        }
        // Remove duplicates and return the list
        return new ArrayList<>(selectedAlbums.stream().distinct().collect(Collectors.toList()));
    }

    /**
     * Retrieves and validates the uploaded image file from the request.
     * Checks for file size and allowed MIME types.
     * @param request    the HTTP request object.
     * @param response   the HTTP response object.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @param username   the username of the logged-in user.
     * @return           the uploaded image Part if valid; otherwise, null.
     * @throws ServletException if a servlet-specific error occurs.
     * @throws IOException      if an I/O error occurs.
     */
    private Part getImageFile(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username) throws ServletException, IOException {
        Part imageFile = request.getPart("imageFile");
        if (imageFile == null || imageFile.getSize() <= 0) {
            showErrorPage("addImage", "Image not uploaded or empty.", request, response, webContext, username);
            return null;
        }
        if (imageFile.getSize() > 1024 * 1024 * 100) {
            showErrorPage("addImage", "Image is too large. Maximum allowed size is 100 MB.", request, response, webContext, username);
            return null;
        }
        String mimeType = imageFile.getContentType();
        List<String> allowedMimeTypes = Arrays.asList("image/jpg", "image/jpeg", "image/png", "image/webp");
        if (mimeType == null || !allowedMimeTypes.contains(mimeType)) {
            showErrorPage("addImage", "Invalid image type. Only JPG, JPEG, PNG or WEBP images are allowed.", request, response, webContext, username);
            return null;
        }
        return imageFile;
    }

    /**
     * Retrieves and validates the image file extension from the uploaded file.
     * @param request    the HTTP request object.
     * @param response   the HTTP response object.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @param username   the username of the logged-in user.
     * @param imageFile  the uploaded image Part.
     * @return           the image file extension if valid; otherwise, null.
     * @throws ServletException if a servlet-specific error occurs.
     * @throws IOException      if an I/O error occurs.
     */
    private String getImageExtension(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username, Part imageFile) throws ServletException, IOException {
        String imageName = imageFile.getSubmittedFileName();
        if (imageName == null && !imageName.contains(".")) {
            showErrorPage("addImage", "Image not uploaded or empty.", request, response, webContext, username);
            return null;
        }
        String imageExtension = "." + imageName.substring(imageName.lastIndexOf(".") + 1).toLowerCase();
        if (!imageExtension.equals(".jpg") && !imageExtension.equals(".jpeg") && !imageExtension.equals(".png") && !imageExtension.equals(".webp")) {
            showErrorPage("addImage", "Invalid image type. Only JPG, JPEG, PNG or WEBP images are allowed.", request, response, webContext, username);
            return null;
        }
        return imageExtension;
    }

    /**
     * Inserts the image data into the database and associates it with selected albums.
     * @param request               the HTTP request object.
     * @param response              the HTTP response object.
     * @param webContext            the Thymeleaf WebContext for rendering templates.
     * @param username              the username of the logged-in user.
     * @param imageStringParameters the list containing image title and description.
     * @param selectedAlbums        the list of selected album IDs.
     * @param imageExtension        the file extension of the image.
     * @return                      the image ID if successful; otherwise, -1.
     * @throws ServletException if a servlet-specific error occurs.
     * @throws IOException      if an I/O error occurs.
     */
    private int insertImageIntoDatabase(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username, ArrayList<String> imageStringParameters, ArrayList<Integer> selectedAlbums, String imageExtension) throws ServletException, IOException {
        try {
            Image image = new Image(username, imageStringParameters.get(0), imageStringParameters.get(1));
            ImageDAO imageDAO = new ImageDAO();
            int imageId = imageDAO.addImage(image);
            if (imageId == -1) {
                showErrorPage("addImage", "Database error. Please reload page.", request, response, webContext, username);
                return -1;
            }
            String uploadsPathString = getUploadsPath();
            if (uploadsPathString == null) {
                showErrorPage("addImage", "Server error. Please reload page.", request, response, webContext, username);
                return -1;
            }
            Path uploadsPath = Paths.get(uploadsPathString);
            Path imagePath = uploadsPath.resolve(imageId + imageExtension);
            boolean updatedPath = imageDAO.updateImagePath(imageId, imagePath.toString());
            if (!updatedPath) {
                showErrorPage("addImage", "Database error. Please reload page.", request, response, webContext, username);
                return -1;
            }
            boolean imageIntoAlbums = imageDAO.addImageToAlbums(imageId, selectedAlbums);
            if (!imageIntoAlbums) {
                showErrorPage("addImage", "Database error. Please reload page.", request, response, webContext, username);
                return -1;
            }
            return imageId;
        } catch (SQLException e) {
            showErrorPage("addImage", "Database error. There may have been errors adding the image. Please reload page.", request, response, webContext, username);
            return -1;
        }
    }

    /**
     * Saves the uploaded image file to the server's disk storage.
     * @param request        the HTTP request object.
     * @param response       the HTTP response object.
     * @param webContext     the Thymeleaf WebContext for rendering templates.
     * @param username       the username of the logged-in user.
     * @param imageFile      the uploaded image Part.
     * @param imageId        the ID of the image in the database.
     * @param imageExtension the file extension of the image.
     * @return               true if the image is saved successfully; otherwise, false.
     * @throws ServletException if a servlet-specific error occurs.
     * @throws IOException      if an I/O error occurs.
     */
    private boolean saveImageIntoDisk(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username, Part imageFile, int imageId, String imageExtension) throws ServletException, IOException {
        // Destination directory to save images
        String uploadsPathString = getUploadsPath();
        if (uploadsPathString == null) {
            showErrorPage("addImage", "Server error. Please reload page.", request, response, webContext, username);
            return false;
        }
        // Create Path object from the loaded uploads path
        Path uploadsPath = Paths.get(uploadsPathString);
        // Create uploads directory (if it doesn't already exist)
        Files.createDirectories(uploadsPath); // thread-safe
        // Full path of the file to save
        Path imagePath = uploadsPath.resolve(imageId + imageExtension);
        // Save the file contents to the destination
        try (InputStream inputStream = imageFile.getInputStream()) {
            Files.copy(inputStream, imagePath, StandardCopyOption.REPLACE_EXISTING);
            // Verify that the file has been created
            if (!Files.exists(imagePath)) {
                showErrorPage("addImage", "Error saving image to server. Please reload page.", request, response, webContext, username);
                return false;
            }
        } catch (IOException save) {
            // Rollback: delete the partial file, if it exists
            try {
                Files.deleteIfExists(imagePath);
            } catch (IOException delete) {
                delete.printStackTrace();
            }
            showErrorPage("addImage", "Error saving image to server. Please reload page.", request, response, webContext, username);
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
     * @throws ServletException if a servlet-specific error occurs.
     * @throws IOException      if an I/O error occurs.
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null)
            session.invalidate();
        response.sendRedirect(request.getContextPath() + "/");
    }

    /**
     * Displays an error message on the specified active panel and renders the home page.
     * @param activePanel  the panel to activate in the UI.
     * @param errorMessage the error message to display.
     * @param request      the HTTP request object.
     * @param response     the HTTP response object.
     * @param webContext   the Thymeleaf WebContext for rendering templates.
     * @param username     the username of the logged-in user.
     * @throws ServletException if a servlet-specific error occurs.
     * @throws IOException      if an I/O error occurs.
     */
    private void showErrorPage(String activePanel, String errorMessage, HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username) throws ServletException, IOException {
        // Set error message
        switch (activePanel) {
            case "myAlbums":
                webContext.setVariable("myAlbumsErrorMessage", errorMessage);
                break;
            case "otherAlbums":
                webContext.setVariable("otherAlbumsErrorMessage", errorMessage);
                break;
            case "createAlbum":
                webContext.setVariable("createAlbumErrorMessage", errorMessage);
                break;
            case "addImage":
                webContext.setVariable("addImageErrorMessage", errorMessage);
                break;
            case "profile":
                webContext.setVariable("profileErrorMessage", errorMessage);
                break;
            default:
                webContext.setVariable("myAlbumsErrorMessage", errorMessage);
                break;
        }
        // Set active panel
        webContext.setVariable("activePanel", activePanel);
        // Page Rendering
        renderHomePage(request, response, webContext, username);
    }

    /**
     * Retrieves and displays any success messages stored in the session.
     * @param session    the HTTP session object.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     */
    private void showSuccessMessage(HttpSession session, WebContext webContext) {
        if (session == null)
            return;
        String[] successMessages = {"createAlbumSuccessMessage", "addImageSuccessMessage"};
        for (String successMessage : successMessages) {
            Object message = session.getAttribute(successMessage);
            if (message != null) {
                webContext.setVariable(successMessage, message);
                session.removeAttribute(successMessage);
            }
        }
    }

}