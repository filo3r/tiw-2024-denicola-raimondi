package it.polimi.tiw.controller;

import it.polimi.tiw.dao.AlbumDAO;
import it.polimi.tiw.dao.CommentDAO;
import it.polimi.tiw.dao.ImageDAO;
import it.polimi.tiw.model.Comment;
import it.polimi.tiw.model.Image;
import it.polimi.tiw.model.User;
import it.polimi.tiw.util.StringUtil;
import it.polimi.tiw.util.ViewEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
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
     * Template engine for rendering HTML templates.
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
     * Handles HTTP GET requests to display the image page.
     * Checks user authentication and retrieves image and album IDs for rendering the image page.
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
        // WebContext
        ServletContext servletContext = getServletContext();
        WebContext webContext = new WebContext(request, response, servletContext, request.getLocale());
        // Get image ID and album ID from request
        ArrayList<Integer> imageAndAlbumIds = getImageAndAlbumIds(request, response, webContext);
        if (imageAndAlbumIds == null || imageAndAlbumIds.isEmpty() || imageAndAlbumIds.contains(-1))
            return;
        // Set image ID and album ID
        webContext.setVariable("imageId", imageAndAlbumIds.get(0));
        webContext.setVariable("albumId", imageAndAlbumIds.get(1));
        // Get user
        User user = (User) session.getAttribute("user");
        String username = user.getUsername();
        webContext.setVariable("user", user);
        // Show success messages
        showSuccessMessage(session, webContext);
        // Render page
        renderImagePage(request, response, webContext, username, imageAndAlbumIds.get(0));
    }

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
        // WebContext
        ServletContext servletContext = getServletContext();
        WebContext webContext = new WebContext(request, response, servletContext, request.getLocale());
        // Get image ID and album ID from request
        ArrayList<Integer> imageAndAlbumIds = getImageAndAlbumIds(request, response, webContext);
        if (imageAndAlbumIds == null || imageAndAlbumIds.isEmpty() || imageAndAlbumIds.contains(-1))
            return;
        // Set image ID and album ID
        webContext.setVariable("imageId", imageAndAlbumIds.get(0));
        webContext.setVariable("albumId", imageAndAlbumIds.get(1));
        // Get user
        User user = (User) session.getAttribute("user");
        String username = user.getUsername();
        webContext.setVariable("user", user);
        // Add Comment or Delete Image or Logout
        String action = request.getParameter("action");
        if ("addComment".equals(action))
            handleAddComment(request, response, webContext, username, imageAndAlbumIds);
        else if ("deleteImage".equals(action))
            handleDeleteImage(request, response, webContext, username, imageAndAlbumIds);
        else if ("returnToHome".equals(action))
            response.sendRedirect(request.getContextPath() + "/home");
        else if ("returnToAlbum".equals(action))
            response.sendRedirect(request.getContextPath() + "/album?albumId=" + imageAndAlbumIds.get(1) + "&page=0");
        else if ("logout".equals(action))
            handleLogout(request, response);
        else
            response.sendRedirect(request.getContextPath() + "/image?albumId=" + imageAndAlbumIds.get(1) + "&imageId=" + imageAndAlbumIds.get(0));
    }

    /**
     * Retrieves and validates the image and album IDs from the request parameters.
     * @param request    the HTTP request object.
     * @param response   the HTTP response object.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @return           a list containing the image ID and album ID.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs.
     */
    private ArrayList<Integer> getImageAndAlbumIds (HttpServletRequest request, HttpServletResponse response, WebContext webContext) throws ServletException, IOException {
        String imageIdParam = request.getParameter("imageId");
        String albumIdParam = request.getParameter("albumId");
        int imageId = -1;
        int albumId = -1;
        ArrayList<Integer> imageAndAlbumIds = new ArrayList<>();
        imageAndAlbumIds.add(imageId);
        imageAndAlbumIds.add(albumId);
        if (albumIdParam == null || albumIdParam.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/home");
            return null;
        }
        try {
            albumId = Integer.parseInt(albumIdParam);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/home");
            return null;
        }
        try {
            AlbumDAO albumDAO = new AlbumDAO();
            boolean albumExists = albumDAO.doesAlbumExist(albumId);
            if (albumExists) {
                imageAndAlbumIds.set(1, albumId);
            } else {
                response.sendRedirect(request.getContextPath() + "/home");
                return null;
            }
            if (imageIdParam == null || imageIdParam.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/album?albumId=" + albumId + "&page=0");
                return null;
            }
            try {
                imageId = Integer.parseInt(imageIdParam);
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/album?albumId=" + albumId + "&page=0");
                return null;
            }
            ImageDAO imageDAO = new ImageDAO();
            boolean imageExists = imageDAO.doesImageExist(imageId);
            if (!imageExists) {
                response.sendRedirect(request.getContextPath() + "/album?albumId=" + albumId + "&page=0");
                return null;
            }
            boolean imageBelongToAlbum = imageDAO.doesImageBelongToAlbum(imageId, albumId);
            if (imageBelongToAlbum) {
                imageAndAlbumIds.set(0, imageId);
            } else {
                response.sendRedirect(request.getContextPath() + "/album?albumId=" + albumId + "&page=0");
                return null;
            }
        } catch (SQLException e) {
            renderImagePageException(request, response, webContext);
            e.printStackTrace();
            return null;
        }
        return imageAndAlbumIds;
    }

    /**
     * Renders the image page by loading image data, comments, and delete options.
     * @param request   the HTTP request object.
     * @param response  the HTTP response object.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @param username  the username of the logged-in user.
     * @param imageId   the ID of the image to be displayed.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs.
     */
    private void renderImagePage(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username, int imageId) throws ServletException, IOException {
        try {
            handleLoadImageData(webContext, imageId);
            handleLoadComments(webContext, imageId);
            handleLoadDeleteOption(webContext, username, imageId);
            templateEngine.process("image.html", webContext, response.getWriter());
        } catch (SQLException e) {
            renderImagePageException(request, response, webContext);
            e.printStackTrace();
        }
    }

    /**
     * Loads the image data for the specified image ID.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @param imageId    the ID of the image.
     * @throws SQLException     if an error occurs while accessing the database.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs.
     */
    private void handleLoadImageData(WebContext webContext, int imageId) throws SQLException, ServletException, IOException {
        ImageDAO imageDAO = new ImageDAO();
        Image image = imageDAO.getImageById(imageId);
        webContext.setVariable("image", image);
    }

    /**
     * Loads comments associated with the specified image ID.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @param imageId    the ID of the image.
     * @throws SQLException     if an error occurs while accessing the database.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs.
     */
    private void handleLoadComments(WebContext webContext, int imageId) throws SQLException, ServletException, IOException {
        CommentDAO commentDAO = new CommentDAO();
        ArrayList<Comment> comments = commentDAO.getCommentsByImageId(imageId);
        webContext.setVariable("comments", comments);
    }

    /**
     * Determines whether the image belongs to the logged-in user and sets the delete option.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @param username   the username of the logged-in user.
     * @param imageId    the ID of the image.
     * @throws SQLException     if an error occurs while accessing the database.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs.
     */
    private void handleLoadDeleteOption(WebContext webContext, String username, int imageId) throws SQLException, ServletException, IOException {
        ImageDAO imageDAO = new ImageDAO();
        boolean imageBelongToUser = imageDAO.doesImageBelongToUser(imageId, username);
        webContext.setVariable("imageBelongToUser", imageBelongToUser);
    }

    /**
     * Renders the image page with an error message when an exception occurs.
     * @param request   the HTTP request object.
     * @param response  the HTTP response object.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs.
     */
    private void renderImagePageException(HttpServletRequest request, HttpServletResponse response, WebContext webContext) throws ServletException, IOException {
        webContext.setVariable("image", null);
        webContext.setVariable("comments", null);
        webContext.setVariable("imageBelongToUser", false);
        webContext.setVariable("imageErrorMessage", "Database error. Please reload page.");
        templateEngine.process("image.html", webContext, response.getWriter());
    }

    /**
     * Handles the addition of a comment to an image.
     * @param request           the HTTP request object.
     * @param response          the HTTP response object.
     * @param webContext        the Thymeleaf WebContext for rendering templates.
     * @param username          the username of the logged-in user.
     * @param imageAndAlbumIds  the list containing the image ID and album ID.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs.
     */
    private void handleAddComment(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username, ArrayList<Integer> imageAndAlbumIds) throws ServletException, IOException {
        String commentText = request.getParameter("commentText");
        if (!StringUtil.isValidText(commentText)) {
            showErrorPage("Invalid comment text.", request, response, webContext, username, imageAndAlbumIds.get(0));
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
                HttpSession session = request.getSession();
                session.setAttribute("addCommentSuccessMessage", "Comment added successfully.");
                response.sendRedirect(request.getContextPath() + "/image?albumId=" + imageAndAlbumIds.get(1) + "&imageId=" + imageAndAlbumIds.get(0));
            } else {
                showErrorPage("Database error. Please reload page.", request, response, webContext, username, imageAndAlbumIds.get(0));
            }
        } catch (SQLException e) {
            showErrorPage("Database error. Please reload page.", request, response, webContext, username, imageAndAlbumIds.get(0));
            e.printStackTrace();
        }
    }

    /**
     * Handles the deletion of an image by the logged-in user.
     * @param request           the HTTP request object.
     * @param response          the HTTP response object.
     * @param webContext        the Thymeleaf WebContext for rendering templates.
     * @param username          the username of the logged-in user.
     * @param imageAndAlbumIds  the list containing the image ID and album ID.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs.
     */
    private void handleDeleteImage(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username, ArrayList<Integer> imageAndAlbumIds) throws ServletException, IOException {
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
                HttpSession session = request.getSession();
                session.setAttribute("deleteImageSuccessMessage", "Image deleted successfully.");
                response.sendRedirect(request.getContextPath() + "/album?albumId=" + imageAndAlbumIds.get(1) + "&page=0");
            } else {
                showErrorPage("Database error. Please reload page.", request, response, webContext, username, imageAndAlbumIds.get(0));
            }
        } catch (SQLException e) {
            showErrorPage("Database error. Please reload page.", request, response, webContext, username, imageAndAlbumIds.get(0));
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
     * Handles user logout by invalidating the session and redirecting to the login page.
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs.
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null)
            session.invalidate();
        response.sendRedirect(request.getContextPath() + "/");
    }

    /**
     * Displays an error message on the image page.
     * @param errorMessage the error message to display.
     * @param request      the HTTP request object.
     * @param response     the HTTP response object.
     * @param webContext   the Thymeleaf WebContext for rendering templates.
     * @param username     the username of the logged-in user.
     * @param imageId      the ID of the image to be displayed on the error page.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs.
     */
    private void showErrorPage(String errorMessage, HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username, int imageId) throws ServletException, IOException {
        webContext.setVariable("imageErrorMessage", errorMessage);
        renderImagePage(request, response, webContext, username, imageId);
    }

    /**
     * Retrieves and displays any success messages stored in the session.
     * @param session    the HTTP session object.
     * @param webContext the Thymeleaf WebContext for rendering templates.
     */
    private void showSuccessMessage(HttpSession session, WebContext webContext) {
        if (session == null)
            return;
        String[] successMessages = {"addCommentSuccessMessage"};
        for (String successMessage : successMessages) {
            Object message = session.getAttribute(successMessage);
            if (message != null) {
                webContext.setVariable(successMessage, message);
                session.removeAttribute(successMessage);
            }
        }
    }

}