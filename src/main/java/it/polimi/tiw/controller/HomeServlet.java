package it.polimi.tiw.controller;

import it.polimi.tiw.dao.AlbumDAO;
import it.polimi.tiw.model.Album;
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
            response.sendRedirect("./");
            return;
        }
        // Get user
        User user = (User) session.getAttribute("user");
        String username = user.getUsername();
        // Load albums
        try {
            AlbumDAO albumDAO = new AlbumDAO();
            ArrayList<Album> myAlbums = albumDAO.getMyAlbums(username);
            ArrayList<Album> otherAlbums = albumDAO.getOtherAlbums(username);
            // Set variables for Thymeleaf
            ServletContext servletContext = getServletContext();
            final WebContext webContext = new WebContext(request, response, servletContext, request.getLocale());
            webContext.setVariable("myAlbums", myAlbums);
            webContext.setVariable("otherAlbums", otherAlbums);
            templateEngine.process("home.html", webContext, response.getWriter());
        } catch (SQLException e) {
            showErrorPage(request, response, "Database error. Please try again", "AlbumsErrorMessage", "myAlbums");
            showErrorPage(request, response, "Database error. Please try again", "AlbumsErrorMessage", "otherAlbums");
            e.printStackTrace();
        }
    }

    private void showErrorPage(HttpServletRequest request, HttpServletResponse response, String errorMessage, String errorAttribute, String panel) throws ServletException, IOException {
        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale());
        webContext.setVariable(errorAttribute, errorMessage);
        webContext.setVariable("activePanel", panel);
        templateEngine.process("home.html", webContext, response.getWriter());
    }

}
