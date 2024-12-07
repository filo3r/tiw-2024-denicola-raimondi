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

        // Get user
        User user = (User) session.getAttribute("user");

        // Get album ID from request
        String albumIdParam = request.getParameter("albumId");
        if (albumIdParam == null || albumIdParam.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }

        int albumId;
        try {
            albumId = Integer.parseInt(albumIdParam);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }

        // Creazione del WebContext e setting variabili qui
        ServletContext servletContext = getServletContext();
        WebContext webContext = new WebContext(request, response, servletContext, request.getLocale());

        // rendering album page, ora passiamo anche il webContext
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
        // Get user
        User user = (User) session.getAttribute("user");
        String username = user.getUsername();
        // WebContext
        ServletContext servletContext = getServletContext();
        WebContext webContext = new WebContext(request, response, servletContext, request.getLocale());
        webContext.setVariable("user", user);
        //logout
        String action = request.getParameter("action");
        if("logout".equals(action))
            handleLogout(request, response);
        else
            response.sendRedirect(request.getContextPath() + "/album");
    }


    private void renderAlbumPage(HttpServletRequest request, HttpServletResponse response, WebContext webContext, int albumId) throws ServletException, IOException {
        try {
            handleLoadAlbumData(webContext, albumId);
        } catch (SQLException e) {
            webContext.setVariable("albumErrorMessage", "Database error. Please reload page.");
            e.printStackTrace();
        }

        templateEngine.process("album.html", webContext, response.getWriter());
    }


    private void handleLoadAlbumData(WebContext webContext, int albumId) throws SQLException {
        AlbumDAO albumDAO = new AlbumDAO();
        Album album = albumDAO.getAlbumById(albumId);

        if (album == null) {
            webContext.setVariable("albumErrorMessage", "Album not found.");
            return;
        }

        //  Album's owner?
        // if (!albumDAO.isAlbumOwnedByUser(albumId, username)) {
        //     webContext.setVariable("albumErrorMessage", "You don't own this album.");
        //     return;
        // }

        webContext.setVariable("album", album);
        webContext.setVariable("albumTitle", album.getAlbumTitle());
        //todo: da inserire nella album page
        webContext.setVariable("albumDate", album.getAlbumDate());

    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null)
            session.invalidate();
        response.sendRedirect(request.getContextPath() + "/");
    }

}
