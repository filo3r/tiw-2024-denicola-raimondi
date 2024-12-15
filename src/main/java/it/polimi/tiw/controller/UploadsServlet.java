package it.polimi.tiw.controller;

import it.polimi.tiw.dao.ImageDAO;
import it.polimi.tiw.util.ViewEngine;
import org.thymeleaf.TemplateEngine;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Properties;

/**
 * UploadsServlet handles the retrieval and streaming of image files
 * stored on the server. It ensures secure access and delivers the
 * appropriate image file based on the request parameters.
 */
public class UploadsServlet extends HttpServlet {

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
     * Path to the directory where image uploads are stored.
     */
    private Path uploadsPath;

    /**
     * Initializes the servlet, retrieves the TemplateEngine instance, and
     * determines the uploads directory path from configuration.
     * @throws ServletException if an error occurs during initialization or if the
     *                          uploads directory configuration is missing or invalid.
     */
    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();
        this.templateEngine = ViewEngine.getTemplateEngine(servletContext);
        String uploadsPathString = getUploadsPath();
        if (uploadsPathString == null || uploadsPathString.isEmpty()) {
            this.uploadsPath = null;
            System.err.println("Could not find uploads.properties file.");
            throw new ServletException("Could not find uploads.properties file.");
        } else {
            this.uploadsPath = Paths.get(getUploadsPath()).toAbsolutePath().normalize();
        }
    }

    /**
     * Handles HTTP GET requests to retrieve and stream an image file.
     * Ensures that the user is authenticated and the requested image ID is valid.
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @throws ServletException if an error occurs during request processing.
     * @throws IOException      if an I/O error occurs during request processing.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        // Get image ID
        int imageId = getImageId(request, response);
        if (imageId == -1)
            return;
        // Get image path
        String imagePathString = getImagePathString(response, imageId);
        if (imagePathString == null || imagePathString.isEmpty())
            return;
        // Stream image
        streamImage(response, imagePathString);
    }

    /**
     * Retrieves the image ID from the request parameters and validates its existence.
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @return the validated image ID, or -1 if the image ID is invalid or does not exist.
     * @throws ServletException if an error occurs during request processing.
     * @throws IOException      if an I/O error occurs during request processing.
     */
    private int getImageId(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String imageIdParam = request.getParameter("imageId");
        int imageId = -1;
        if (imageIdParam == null || imageIdParam.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing imageId parameter.");
            return -1;
        }
        try {
            imageId = Integer.parseInt(imageIdParam);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid imageId parameter.");
            return -1;
        }
        try {
            ImageDAO imageDAO = new ImageDAO();
            boolean imageExists = imageDAO.doesImageExist(imageId);
            if (imageExists) {
                return imageId;
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            return -1;
        }
    }

    /**
     * Retrieves the file path of the image corresponding to the given image ID.
     * @param response the HTTP response object.
     * @param imageId  the ID of the image.
     * @return the file path of the image as a string, or null if the path is invalid.
     * @throws ServletException if an error occurs during request processing.
     * @throws IOException      if an I/O error occurs during request processing.
     */
    private String getImagePathString(HttpServletResponse response, int imageId) throws ServletException, IOException {
        String imagePathString = null;
        try {
            ImageDAO imageDAO = new ImageDAO();
            imagePathString = imageDAO.getImagePathById(imageId);
            if (imagePathString == null || imagePathString.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image path not found.");
                return null;
            } else {
                return imagePathString;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            return null;
        }
    }

    /**
     * Streams the image file to the client.
     * @param response         the HTTP response object.
     * @param imagePathString  the file path of the image to be streamed.
     * @return true if the image is successfully streamed, false otherwise.
     * @throws ServletException if an error occurs during request processing.
     * @throws IOException      if an I/O error occurs during request processing.
     */
    private boolean streamImage(HttpServletResponse response, String imagePathString) throws ServletException, IOException {
        // Get image path
        Path imagePath = Paths.get(imagePathString);
        // Safety check on the path
        if (!imagePath.toAbsolutePath().normalize().startsWith(this.uploadsPath)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image path.");
            return false;
        }
        // Check that the file exists and is not a directory
        if (!Files.exists(imagePath) || Files.isDirectory(imagePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found on disk.");
            return false;
        }
        // Determines the content type of the file
        String contentType = Files.probeContentType(imagePath);
        if (contentType == null)
            contentType = "application/octet-stream"; // Generic content type
        // Set the response headers
        response.setContentType(contentType);
        response.setContentLengthLong(Files.size(imagePath));
        // Stream the file to the client
        try (OutputStream outputStream = response.getOutputStream()) {
            Files.copy(imagePath, outputStream);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            return false;
        }
    }

    /**
     * Retrieves the path of the uploads directory from a properties file.
     * @return the uploads directory path as a string, or null if an error occurs.
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

}