package it.polimi.tiw.controller;

import it.polimi.tiw.dao.AlbumDAO;
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

/**
 *
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

    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();
        this.templateEngine = ViewEngine.getTemplateEngine(servletContext);
    }

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
        // Load albums
        handleLoadAlbums(request, response, username);
    }

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
        // Create Album or Add Photo
        String action = request.getParameter("action");
        if ("createAlbum".equals(action))
            handleCreateAlbum(request, response, username);
        else if ("addImage".equals(action))
            handleAddImage(request, response, username);
        else
            response.sendRedirect(request.getContextPath() + "/home");
    }

    private void handleLoadAlbums(HttpServletRequest request, HttpServletResponse response, String username) throws ServletException, IOException {
        try {
            AlbumDAO albumDAO = new AlbumDAO();
            ArrayList<Album> myAlbums = albumDAO.getMyAlbums(username);
            ArrayList<Album> otherAlbums = albumDAO.getOtherAlbums(username);
            ServletContext servletContext = getServletContext();
            final WebContext webContext = new WebContext(request, response, servletContext, request.getLocale());
            webContext.setVariable("myAlbums", myAlbums);
            webContext.setVariable("otherAlbums", otherAlbums);
            // Success message when creating an album
            String createAlbumSuccessMessage = (String) request.getAttribute("createAlbumSuccessMessage");
            if (createAlbumSuccessMessage != null)
                webContext.setVariable("createAlbumSuccessMessage", createAlbumSuccessMessage);
            // Success message when adding an image
            String addImageSuccessMessage = (String) request.getAttribute("addImageSuccessMessage");
            if (addImageSuccessMessage != null)
                webContext.setVariable("addImageSuccessMessage", addImageSuccessMessage);
            templateEngine.process("home.html", webContext, response.getWriter());
        } catch (SQLException e) {
            showErrorPage(request, response, "Database error. Please try again.", "albumsErrorMessage");
            e.printStackTrace();
        }
    }

    private void handleCreateAlbum(HttpServletRequest request, HttpServletResponse response, String username) throws ServletException, IOException {
        String albumTitle = request.getParameter("albumTitle");
        if (StringUtil.isNullOrEmpty(albumTitle) || !StringUtil.isValidAlbumTitle(albumTitle)) {
            showErrorPage(request, response, "Invalid album title.", "createAlbumErrorMessage");
            return;
        }
        Album album = new Album(username, albumTitle);
        try {
            AlbumDAO albumDAO = new AlbumDAO();
            boolean success = albumDAO.createAlbum(album);
            if (success) {
                request.setAttribute("createAlbumSuccessMessage", "Album created successfully.");
                handleLoadAlbums(request, response, username);
            } else {
                showErrorPage(request, response, "Database error. Please try again.", "createAlbumErrorMessage");
            }
        } catch (SQLException e) {
            showErrorPage(request, response, "Database error. Please try again.", "createAlbumErrorMessage");
        }
    }

    private void handleAddImage(HttpServletRequest request, HttpServletResponse response, String username) throws ServletException, IOException {

    }

    private void showErrorPage(HttpServletRequest request, HttpServletResponse response, String errorMessage, String errorAttribute) throws ServletException, IOException {
        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        // Get user
        User user = (User) session.getAttribute("user");
        String username = user.getUsername();
        // Reload albums
        try {
            AlbumDAO albumDAO = new AlbumDAO();
            ArrayList<Album> myAlbums = albumDAO.getMyAlbums(username);
            ArrayList<Album> otherAlbums = albumDAO.getOtherAlbums(username);
            WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale());
            webContext.setVariable("myAlbums", myAlbums);
            webContext.setVariable("otherAlbums", otherAlbums);
            webContext.setVariable(errorAttribute, errorMessage);
            webContext.setVariable("activePanel", getActivePanelFromErrorAttribute(errorAttribute));
            templateEngine.process("home.html", webContext, response.getWriter());
        } catch (SQLException e) {
            response.sendRedirect(request.getContextPath() + "/home");
        }
    }

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