package it.polimi.tiw.controller;

import it.polimi.tiw.dao.AlbumDAO;
import it.polimi.tiw.model.Album;
import it.polimi.tiw.model.Image;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AlbumServlet extends HttpServlet {

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
        // WebContext
        ServletContext servletContext = getServletContext();
        WebContext webContext = new WebContext(request, response, servletContext, request.getLocale());
        // Get album ID from request
        int albumId = getAlbumId(request, response, webContext);
        if (albumId == -1) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }
        webContext.setVariable("albumId", albumId);
        // Show success messages
        showSuccessMessage(session, webContext);
        // Render page
        renderAlbumPage(request, response, webContext, albumId);
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
        // WebContext
        ServletContext servletContext = getServletContext();
        WebContext webContext = new WebContext(request, response, servletContext, request.getLocale());
        // Get album ID from request
        int albumId = getAlbumId(request, response, webContext);
        if (albumId == -1) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }
        webContext.setVariable("albumId", albumId);
        // Return To Home or Logout
        String action = request.getParameter("action");
        if ("returnToHome".equals(action))
            response.sendRedirect(request.getContextPath() + "/home");
        else if ("logout".equals(action))
            handleLogout(request, response);
        else
            response.sendRedirect(request.getContextPath() + "/album?albumId=" + albumId + "&page=0");
    }

    /**
     * Retrieves and validates the album ID from the request.
     * Ensures the album ID is present, correctly formatted, and exists in the database.
     * Displays an error page if validation fails or a database error occurs.
     *
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @param webContext the WebContext object for setting template variables.
     * @return the valid album ID, or -1 if the ID is invalid or the album does not exist.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private int getAlbumId(HttpServletRequest request, HttpServletResponse response, WebContext webContext) throws ServletException, IOException {
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
        try {
            AlbumDAO albumDAO = new AlbumDAO();
            boolean exists = albumDAO.doesAlbumExist(albumId);
            if (exists)
                return albumId;
            else
                return -1;
        } catch (SQLException e) {
            renderImagePageException(request, response, webContext);
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Renders the album page by loading album details and images.
     * Handles any database errors by setting error variables in the web context.
     *
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @param webContext the WebContext object for managing template variables.
     * @param albumId  the ID of the album to load and display.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private void renderAlbumPage(HttpServletRequest request, HttpServletResponse response, WebContext webContext, int albumId) throws ServletException, IOException {
        try {
            handleLoadAlbumData(webContext, albumId);
            handleLoadAlbumImages(request, webContext, albumId);
            templateEngine.process("album.html", webContext, response.getWriter());
        } catch (SQLException e) {
            renderImagePageException(request, response, webContext);
            e.printStackTrace();
        }
    }

    /**
     * Loads album details for the specified album ID and sets them in the web context.
     *
     * @param webContext the WebContext object for managing template variables.
     * @param albumId    the ID of the album to load.
     * @throws SQLException if a database access error occurs while retrieving the album data.
     */
    private void handleLoadAlbumData(WebContext webContext, int albumId) throws SQLException {
        AlbumDAO albumDAO = new AlbumDAO();
        Album album = albumDAO.getAlbumById(albumId);
        webContext.setVariable("album", album);
    }

    /**
     * Loads and paginates the images for the specified album.
     * Retrieves all images associated with the album ID, calculates the current page,
     * and sets the paginated images along with pagination details in the web context.
     *
     * @param request    the HTTP request object, used to retrieve the "page" parameter.
     * @param webContext the WebContext object for managing template variables.
     * @param albumId    the ID of the album whose images are to be loaded.
     * @throws SQLException if a database access error occurs while retrieving the images.
     */
    private void handleLoadAlbumImages(HttpServletRequest request, WebContext webContext, int albumId) throws SQLException {
        // Default information
        int page = 0;
        int pageSize = 5;
        // Get album info
        AlbumDAO albumDAO = new AlbumDAO();
        int totalImages = albumDAO.getImagesCountByAlbumId(albumId);
        // Calculate the maximum page index
        int maxPage = 0;
        if (totalImages > 0)
            maxPage = (int) Math.ceil((double) totalImages / pageSize) - 1;
        else
            maxPage = 0;
        // Manage current page with parameter (first page is 0)
        String pageParam = request.getParameter("page");
        if (pageParam != null && !pageParam.isEmpty()) {
            try {
                page = Integer.parseInt(pageParam);
            } catch (NumberFormatException e) {
                page = 0;
            }
            if (page < 0 || page > maxPage)
                page = 0;
        } else {
            page = 0;
        }
        // Calculate indexes
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalImages);
        // Load images to the page
        ArrayList<Image> images = albumDAO.getImagesByAlbumIdWithPagination(albumId, pageSize, startIndex);
        // WebContext
        webContext.setVariable("images", images);
        webContext.setVariable("currentPage", page);
        webContext.setVariable("hasPrevious", page > 0);
        webContext.setVariable("hasNext", endIndex < totalImages);
    }

    private void renderImagePageException(HttpServletRequest request, HttpServletResponse response, WebContext webContext) throws ServletException, IOException {
        webContext.setVariable("album", null);
        webContext.setVariable("images", null);
        webContext.setVariable("albumErrorMessage", "Database error. Please reload page.");
        templateEngine.process("image.html", webContext, response.getWriter());
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

    private void showSuccessMessage(HttpSession session, WebContext webContext) {
        if (session == null)
            return;
        String[] successMessages = {"deleteImageSuccessMessage"};
        for (String successMessage : successMessages) {
            Object message = session.getAttribute(successMessage);
            if (message != null) {
                webContext.setVariable(successMessage, message);
                session.removeAttribute(successMessage);
            }
        }
    }

}