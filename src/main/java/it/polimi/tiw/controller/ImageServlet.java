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
import java.sql.SQLException;
import java.util.ArrayList;

public class ImageServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

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
        // WebContext
        ServletContext servletContext = getServletContext();
        WebContext webContext = new WebContext(request, response, servletContext, request.getLocale());
        // Get image ID and album ID from request
        ArrayList<Integer> imageAndAlbumIds = getImageAndAlbumIds(request, response, webContext);
        if (imageAndAlbumIds == null || imageAndAlbumIds.isEmpty())
            return;
        // Get user
        User user = (User) session.getAttribute("user");
        String username = user.getUsername();
        webContext.setVariable("user", user);
        // Show success messages
        showSuccessMessage(session, webContext);
        // Render page
        renderImagePage(request, response, webContext, username, imageAndAlbumIds.get(0));
    }

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
        if (imageAndAlbumIds == null || imageAndAlbumIds.isEmpty())
            return;
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
        else if ("logout".equals(action))
            handleLogout(request, response);
        else
            response.sendRedirect(request.getContextPath() + "/image?albumId=" + imageAndAlbumIds.get(1) + "&imageId=" + imageAndAlbumIds.get(0));
    }

    private ArrayList<Integer> getImageAndAlbumIds (HttpServletRequest request, HttpServletResponse response, WebContext webContext) throws ServletException, IOException {
        String imageIdParam = request.getParameter("imageId");
        String albumIdParam = request.getParameter("albumId");
        int imageId = -1;
        int albumId = -1;
        ArrayList<Integer> imageAndAlbumIds = new ArrayList<>();
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
                imageAndAlbumIds.add(1, albumId);
            } else {
                response.sendRedirect(request.getContextPath() + "/home");
                return null;
            }
            if (imageIdParam == null || imageIdParam.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/album?albumId=" + albumId);
                return null;
            }
            try {
                imageId = Integer.parseInt(imageIdParam);
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/album?albumId=" + albumId);
                return null;
            }
            ImageDAO imageDAO = new ImageDAO();
            boolean imageExists = imageDAO.doesImageExist(imageId);
            if (!imageExists) {
                response.sendRedirect(request.getContextPath() + "/album?albumId=" + albumId);
                return null;
            }
            boolean imageBelongToAlbum = imageDAO.doesImageBelongToAlbum(imageId, albumId);
            if (imageBelongToAlbum) {
                imageAndAlbumIds.add(0, imageId);
            } else {
                response.sendRedirect(request.getContextPath() + "/album?albumId=" + albumId);
                return null;
            }
        } catch (SQLException e) {
            renderImagePageException(request, response, webContext);
            e.printStackTrace();
            return null;
        }
        return imageAndAlbumIds;
    }

    private void renderImagePage(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username, int imageId) throws ServletException, IOException {
        try {
            handleLoadImageData(webContext, imageId);
            handleLoadComments(webContext, imageId);
            handleLoadDeleteOption(webContext, username, imageId);
        } catch (SQLException e) {
            renderImagePageException(request, response, webContext);
            e.printStackTrace();
        }
    }

    private void handleLoadImageData(WebContext webContext, int imageId) throws SQLException, ServletException, IOException {
        ImageDAO imageDAO = new ImageDAO();
        Image image = imageDAO.getImageById(imageId);
        webContext.setVariable("image", image);
    }

    private void handleLoadComments(WebContext webContext, int imageId) throws SQLException, ServletException, IOException {
        CommentDAO commentDAO = new CommentDAO();
        ArrayList<Comment> comments = commentDAO.getCommentsByImageId(imageId);
        webContext.setVariable("comments", comments);
    }

    private void handleLoadDeleteOption(WebContext webContext, String username, int imageId) throws SQLException, ServletException, IOException {
        ImageDAO imageDAO = new ImageDAO();
        boolean imageBelongToUser = imageDAO.doesImageBelongToUser(imageId, username);
        webContext.setVariable("imageBelongToUser", imageBelongToUser);
    }

    private void renderImagePageException(HttpServletRequest request, HttpServletResponse response, WebContext webContext) throws ServletException, IOException {
        webContext.setVariable("image", null);
        webContext.setVariable("comments", null);
        webContext.setVariable("imageBelongToUser", false);
        webContext.setVariable("imageErrorMessage", "Database error. Please reload page.");
        templateEngine.process("image.html", webContext, response.getWriter());
    }

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
                session.setAttribute("addCommentSuccessMessage", comment);
                response.sendRedirect(request.getContextPath() + "/image?albumId=" + imageAndAlbumIds.get(1) + "&imageId=" + imageAndAlbumIds.get(0));
            } else {
                showErrorPage("Database error. Please reload page.", request, response, webContext, username, imageAndAlbumIds.get(0));
            }
        } catch (SQLException e) {
            showErrorPage("Database error. Please reload page.", request, response, webContext, username, imageAndAlbumIds.get(0));
            e.printStackTrace();
        }
    }

    private void handleDeleteImage(HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username, ArrayList<Integer> imageAndAlbumIds) throws ServletException, IOException {
        try {
            ImageDAO imageDAO = new ImageDAO();
            boolean imageExists = imageDAO.doesImageExist(imageAndAlbumIds.get(0));
            if (!imageExists)
                return;
            boolean imageBelongToUser = imageDAO.doesImageBelongToUser(imageAndAlbumIds.get(0), username);
            if (!imageBelongToUser)
                return;
            boolean success = imageDAO.deleteImageById(imageAndAlbumIds.get(0));
            if (success) {
                HttpSession session = request.getSession();
                session.setAttribute("deleteImageSuccessMessage", "Image deleted successfully.");
                response.sendRedirect(request.getContextPath() + "/album?albumId=" + imageAndAlbumIds.get(1));
            } else {
                showErrorPage("Database error. Please reload page.", request, response, webContext, username, imageAndAlbumIds.get(0));
            }
        } catch (SQLException e) {
            showErrorPage("Database error. Please reload page.", request, response, webContext, username, imageAndAlbumIds.get(0));
            e.printStackTrace();
        }
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null)
            session.invalidate();
        response.sendRedirect(request.getContextPath() + "/");
    }

    private void showErrorPage(String errorMessage, HttpServletRequest request, HttpServletResponse response, WebContext webContext, String username, int imageId) throws ServletException, IOException {
        webContext.setVariable("imageErrorMessage", errorMessage);
        renderImagePage(request, response, webContext, username, imageId);
    }

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