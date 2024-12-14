package it.polimi.tiw.controller;

import it.polimi.tiw.dao.ImageDAO;
import it.polimi.tiw.util.ViewEngine;
import org.thymeleaf.TemplateEngine;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Properties;

public class UploadsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private TemplateEngine templateEngine;

    private Path uploadsPath;

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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int imageId = getImageId(request, response);
        if (imageId == -1)
            return;
        String imagePathString = getImagePathString(response, imageId);
        if (imagePathString == null || imagePathString.isEmpty())
            return;
        streamImage(response, imagePathString);
    }

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