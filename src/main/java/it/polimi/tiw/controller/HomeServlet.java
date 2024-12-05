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
     * Loads the user's albums and renders the home page.
     * @param username       the username of the logged-in user.
     */
    private void handleLoadAlbums(WebContext webContext, String username) throws SQLException, ServletException, IOException {
        AlbumDAO albumDAO = new AlbumDAO();
        ArrayList<Album> myAlbums = albumDAO.getMyAlbums(username);
        ArrayList<Album> otherAlbums = albumDAO.getOtherAlbums(username);
        webContext.setVariable("myAlbums", myAlbums);
        webContext.setVariable("otherAlbums", otherAlbums);
    }

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
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @param username the username of the logged-in user.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    private void handleCreateAlbum(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username) throws ServletException, IOException {
        String albumTitle = request.getParameter("albumTitle");
        if (!StringUtil.isValidTitle(albumTitle)) {
            request.setAttribute("createAlbumErrorMessage", "Invalid album title.");
            renderHomePage(request, response, webContext, username);
            return;
        }
        Album album = new Album(username, albumTitle);
        try {
            AlbumDAO albumDAO = new AlbumDAO();
            boolean success = albumDAO.createAlbum(album);
            if (success) {
                request.setAttribute("createAlbumSuccessMessage", "Album created successfully.");
                response.sendRedirect(request.getContextPath() + "/home");
            } else {
                request.setAttribute("createAlbumErrorMessage", "Database error. Please reload page.");
                renderHomePage(request, response, webContext, username);
            }
        } catch (SQLException e) {
            request.setAttribute("createAlbumErrorMessage", "Database error. Please reload page.");
            renderHomePage(request, response, webContext, username);
            e.printStackTrace();
        }
    }


    private void handleAddImage(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username) throws ServletException, IOException {
        boolean successStringParameter = getImageStringParameter(request, response, webContext, username);
        if (!successStringParameter)
            return;

    }

    private boolean getImageStringParameter(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username) throws ServletException, IOException {
        String imageTitle = request.getParameter("imageTitle");
        String imageText = request.getParameter("imageText");
        if (!StringUtil.isValidTitle(imageTitle)) {
            request.setAttribute("addImageErrorMessage", "Invalid image title.");
            renderHomePage(request, response, webContext, username);
            return false;
        }
        if (!StringUtil.isValidText(imageText)) {
            request.setAttribute("addImageErrorMessage", "Invalid image description.");
            renderHomePage(request, response, webContext, username);
            return false;
        }
        return true;
    }


    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null)
            session.invalidate();
        response.sendRedirect(request.getContextPath() + "/");
    }

}