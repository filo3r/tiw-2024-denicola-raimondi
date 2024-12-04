package it.polimi.tiw.controller;

import it.polimi.tiw.dao.AlbumDAO;
import it.polimi.tiw.dao.CommentDAO;
import it.polimi.tiw.dao.ImageDAO;
import it.polimi.tiw.model.Album;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

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
        // Render page
        renderHomePage(request, response, webContext, username, null, null);
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

    private void renderHomePage(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username, String errorMessage, String errorAttribute) throws ServletException, IOException {
        handleLoadAlbums(request, response, webContext, username, errorMessage, errorAttribute);
        handleProfile(request, response, webContext, username);
        templateEngine.process("home.html", webContext, response.getWriter());
    }

    /**
     * Loads the user's albums and renders the home page.
     * @param request        the HTTP request object.
     * @param response       the HTTP response object.
     * @param username       the username of the logged-in user.
     * @param errorMessage   optional error message to display.
     * @param errorAttribute optional error attribute name.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private void handleLoadAlbums(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username, String errorMessage, String errorAttribute) throws ServletException, IOException {
        try {
            AlbumDAO albumDAO = new AlbumDAO();
            ArrayList<Album> myAlbums = albumDAO.getMyAlbums(username);
            ArrayList<Album> otherAlbums = albumDAO.getOtherAlbums(username);
            webContext.setVariable("myAlbums", myAlbums);
            webContext.setVariable("otherAlbums", otherAlbums);
            // Set success messages if any
            String createAlbumSuccessMessage = (String) request.getAttribute("createAlbumSuccessMessage");
            if (createAlbumSuccessMessage != null)
                webContext.setVariable("createAlbumSuccessMessage", createAlbumSuccessMessage);
            String addImageSuccessMessage = (String) request.getAttribute("addImageSuccessMessage");
            if (addImageSuccessMessage != null)
                webContext.setVariable("addImageSuccessMessage", addImageSuccessMessage);
            // Set error messages if any
            if (errorMessage != null && errorAttribute != null) {
                webContext.setVariable(errorAttribute, errorMessage);
                webContext.setVariable("activePanel", getActivePanelFromErrorAttribute(errorAttribute));
            }
        } catch (SQLException e) {
            showErrorPage(request, response, webContext, "Database error. Please reload the page.", "albumsErrorMessage", false);
            e.printStackTrace();
        }
    }

    /**
     * Handles album creation for the logged-in user.
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @param username the username of the logged-in user.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private void handleCreateAlbum(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username) throws ServletException, IOException {
        String albumTitle = request.getParameter("albumTitle");
        if (StringUtil.isNullOrEmpty(albumTitle) || !StringUtil.isValidAlbumTitle(albumTitle)) {
            showErrorPage(request, response, webContext, "Invalid album title.", "createAlbumErrorMessage", true);
            return;
        }
        Album album = new Album(username, albumTitle);
        try {
            AlbumDAO albumDAO = new AlbumDAO();
            boolean success = albumDAO.createAlbum(album);
            if (success) {
                request.setAttribute("createAlbumSuccessMessage", "Album created successfully.");
                renderHomePage(request, response, webContext, username, null, null);
            } else {
                showErrorPage(request, response, webContext, "Database error. Please try again.", "createAlbumErrorMessage", true);
            }
        } catch (SQLException e) {
            showErrorPage(request, response, webContext, "Database error. Please try again.", "createAlbumErrorMessage", false);
        }
    }

    private void handleAddImage(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username) throws ServletException, IOException {

    }

    private void handleProfile(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username) throws ServletException, IOException {
        try {
            AlbumDAO albumDAO = new AlbumDAO();
            ImageDAO imageDAO = new ImageDAO();
            CommentDAO commentDAO = new CommentDAO();
            int numAlbums = albumDAO.getAlbumsCountByUser(username);
            int numImages = imageDAO.getImagesCountByUser(username);
            int numComments = commentDAO.getCommentsByUser(username);
            User user = (User) request.getSession().getAttribute("user");
            webContext.setVariable("user", user);
            webContext.setVariable("userStats", Map.of(
                    "numAlbums", numAlbums,
                    "numImages", numImages,
                    "numComments", numComments
            ));
        } catch (SQLException e) {
            showErrorPage(request, response, webContext, "Database error. Please reload the page.", "profileErrorMessage", false);
            e.printStackTrace();
        }
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null)
            session.invalidate();
        response.sendRedirect(request.getContextPath() + "/");
    }

    /**
     * Displays an error page with a specified message and optional album loading.
     * @param request        the HTTP request object.
     * @param response       the HTTP response object.
     * @param errorMessage   the error message to display.
     * @param errorAttribute the error attribute name.
     * @param renderHomePage     whether to load albums before rendering the page.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private void showErrorPage(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String errorMessage, String errorAttribute, boolean renderHomePage) throws ServletException, IOException {
        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        // Get user
        User user = (User) session.getAttribute("user");
        String username = user.getUsername();
        if (renderHomePage) {
            // Reload page
            renderHomePage(request, response, webContext, username, errorMessage, errorAttribute);
        } else {
            // Render the page without loading albums
            webContext.setVariable(errorAttribute, errorMessage);
            webContext.setVariable("activePanel", getActivePanelFromErrorAttribute(errorAttribute));
            templateEngine.process("home.html", webContext, response.getWriter());
        }
    }

    /**
     * Determines the active panel based on the provided error attribute.
     * @param errorAttribute the error attribute name.
     * @return the corresponding active panel.
     */
    private String getActivePanelFromErrorAttribute(String errorAttribute) {
        switch (errorAttribute) {
            case "createAlbumErrorMessage":
                return "createAlbum";
            case "addImageErrorMessage":
                return "addImage";
            case "albumsErrorMessage":
                return "myAlbums";
            default:
                return "myAlbums";
        }
    }

}