package it.polimi.tiw.controller;

import it.polimi.tiw.dao.AlbumDAO;
import it.polimi.tiw.dao.ImageDAO;
import it.polimi.tiw.model.Album;
import it.polimi.tiw.model.Image;
import it.polimi.tiw.model.User;
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

public class AlbumServlet extends HttpServlet{

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
        // Logout
        String action = request.getParameter("action");
        if ("logout".equals(action))
            handleLogout(request, response);
        else
            response.sendRedirect(request.getContextPath() + "/album");
    }

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
            showErrorPage("Database error. Please reload page.", request, response, webContext, albumId);
        }
        return albumId;
    }


    private void renderAlbumPage(HttpServletRequest request, HttpServletResponse response, WebContext webContext, int albumId) throws ServletException, IOException {
        try {
            handleLoadAlbumData(webContext, albumId);
            handleLoadAlbumImages(webContext, albumId);
        } catch (SQLException e) {
            webContext.setVariable("album", null);
            webContext.setVariable("images", null);
            webContext.setVariable("albumErrorMessage", "Database error. Please reload page.");
            e.printStackTrace();
        }
        templateEngine.process("album.html", webContext, response.getWriter());
    }

    private void handleLoadAlbumData(WebContext webContext, int albumId) throws SQLException {
        AlbumDAO albumDAO = new AlbumDAO();
        Album album = albumDAO.getAlbumById(albumId);
        webContext.setVariable("album", album);
    }

    private void handleLoadAlbumImages(WebContext webContext, int albumId) throws SQLException {
        AlbumDAO albumDAO = new AlbumDAO();
        ArrayList<Image> images = albumDAO.getImagesByAlbumId(albumId);
        webContext.setVariable("images", images);
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null)
            session.invalidate();
        response.sendRedirect(request.getContextPath() + "/");
    }

    private void showErrorPage(String errorMessage, HttpServletRequest request, HttpServletResponse response, WebContext webContext, int albumId) throws ServletException, IOException {
        webContext.setVariable("albumErrorMessage", errorMessage);
        renderAlbumPage(request, response, webContext, albumId);
    }

}