package it.polimi.tiw.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import it.polimi.tiw.dao.AlbumDAO;
import it.polimi.tiw.dao.UserDAO;
import it.polimi.tiw.model.Album;
import it.polimi.tiw.model.User;
import it.polimi.tiw.util.PasswordEncrypt;
import it.polimi.tiw.util.StringUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * IndexServlet handles HTTP GET and POST requests for the application's main page.
 * It allows users to sign up or sign in and validates user credentials through a series of checks.
 * The servlet communicates with the UserDAO for database operations and uses
 * JSON to communicate responses and handle requests.
 */
public class IndexServlet extends HttpServlet {

    /**
     * Unique identifier for the Serializable class to ensure compatibility
     * during the deserialization process. Changing this value can cause
     * deserialization issues if there are any modifications to the class structure.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Gson instance for parsing and serializing JSON data.
     */
    private final Gson gson = new Gson();

    /**
     * Handles GET requests by displaying the index page or redirecting to the home page
     * if the user is already logged in.
     * @param request  the HttpServletRequest object that contains the client request
     * @param response the HttpServletResponse object that contains the response the servlet sends to the client
     * @throws ServletException if an error occurs during request handling
     * @throws IOException      if an I/O error occurs during request handling
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Check if the user is already logged in
        if (request.getSession(false) != null && request.getSession().getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/spa#home");
            return;
        }
        // Render the page
        request.getRequestDispatcher("/WEB-INF/view/index.html").forward(request, response);
    }

    /**
     * Handles POST requests by determining the action (sign-up or sign-in)
     * and calling the appropriate method.
     * @param request  the HttpServletRequest object that contains the client request
     * @param response the HttpServletResponse object that contains the response the servlet sends to the client
     * @throws ServletException if an error occurs during request handling
     * @throws IOException      if an I/O error occurs during request handling
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Check if the user is already logged in
        if (request.getSession(false) != null && request.getSession().getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/spa#home");
            return;
        }
        // Set JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // Sign Up or Sign In
        try {
            JsonObject jsonRequest = gson.fromJson(request.getReader(), JsonObject.class);
            String action = jsonRequest.get("action").getAsString();
            if ("signUp".equals(action))
                handleSignUp(jsonRequest, request, response);
            else if ("signIn".equals(action))
                handleSignIn(jsonRequest, request, response);
            else if ("checkUsernameAvailability".equals(action))
                handleCheckUsernameAvailability(jsonRequest, response);
            else
                response.sendRedirect(request.getContextPath() + "/");
        } catch (JsonSyntaxException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid JSON format. Please try again.", null, response);
            e.printStackTrace();
        }
    }

    /**
     * Handles the user sign-up process by validating inputs and registering
     * the user in the database if validation is successful.
     * @param jsonRequest the JSON object containing the sign-up request details
     * @param request     the HttpServletRequest object that contains the client request
     * @param response    the HttpServletResponse object that contains the response the servlet sends to the client
     * @throws ServletException if an error occurs during request handling
     * @throws IOException      if an I/O error occurs during request handling
     */
    private void handleSignUp(JsonObject jsonRequest, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = jsonRequest.get("email").getAsString();
        String username = jsonRequest.get("username").getAsString();
        String password1 = jsonRequest.get("password1").getAsString();
        String password2 = jsonRequest.get("password2").getAsString();
        try {
            if (!isSignUpValid(email, username, password1, password2, response))
                return;
            UserDAO userDAO = new UserDAO();
            User user = new User(username, email, PasswordEncrypt.hashPassword(password1));
            boolean successSignUp = userDAO.registerUser(user);
            boolean successCreateAlbum = false;
            if (successSignUp) {
                AlbumDAO albumDAO = new AlbumDAO();
                Album album = new Album(username, "@" + username);
                successCreateAlbum = albumDAO.createAlbum(album);
            }
            if (successSignUp && successCreateAlbum) {
                request.getSession().setAttribute("user", user);
                sendSuccessResponse(HttpServletResponse.SC_OK, null, request.getContextPath() + "/spa#home", response);
            } else if (successSignUp && !successCreateAlbum) {
                userDAO.deleteUser(username);
                sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", null, response);
                return;
            } else {
                sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", null, response);
                return;
            }
        } catch (SQLException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", null, response);
            e.printStackTrace();
        }
    }

    /**
     * Handles the user sign-in process by validating inputs and logging
     * in the user if validation is successful.
     * @param jsonRequest the JSON object containing the sign-in request details
     * @param request     the HttpServletRequest object that contains the client request
     * @param response    the HttpServletResponse object that contains the response the servlet sends to the client
     * @throws ServletException if an error occurs during request handling
     * @throws IOException      if an I/O error occurs during request handling
     */
    private void handleSignIn(JsonObject jsonRequest, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = jsonRequest.get("email").getAsString();
        String password = jsonRequest.get("password").getAsString();
        try {
            if (!isSignInValid(email, password, response))
                return;
            UserDAO userDAO = new UserDAO();
            User user = new User(null, email, password);
            boolean success = userDAO.loginUser(user);
            if (success) {
                user.setPassword(null);
                user.setUsername(userDAO.getUsernameByEmail(user.getEmail()));
                request.getSession().setAttribute("user", user);
                sendSuccessResponse(HttpServletResponse.SC_OK, null, request.getContextPath() + "/spa#home", response);
            } else {
                sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Wrong credentials. Please try again.", null, response);
                return;
            }
        } catch (SQLException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", null, response);
            e.printStackTrace();
        }
    }

    /**
     * Validates the inputs for user sign-up. Ensures that the email, username, and passwords
     * meet criteria and are not already in use.
     * @param email     the email address of the user
     * @param username  the chosen username of the user
     * @param password1 the password entered by the user
     * @param password2 the repeated password entered by the user
     * @param response  the HttpServletResponse object that contains the response the servlet sends to the client
     * @return true if validation is successful, false otherwise
     * @throws ServletException if an error occurs during request handling
     * @throws IOException      if an I/O error occurs during request handling
     * @throws SQLException     if a database error occurs
     */
    private boolean isSignUpValid(String email, String username, String password1, String password2, HttpServletResponse response) throws ServletException, IOException, SQLException {
        if (!StringUtil.isValidEmail(email)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid email.", null, response);
            return false;
        }
        if (!StringUtil.isValidUsername(username)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.", null, response);
            return false;
        }
        if (!StringUtil.isValidPassword(password1)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid password.", null, response);
            return false;
        }
        if (!password1.equals(password2)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid password.", null, response);
            return false;
        }
        UserDAO userDAO = new UserDAO();
        if (userDAO.isEmailTaken(email)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Email already taken.", null, response);
            return false;
        }
        if (userDAO.isUsernameTaken(username)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Username already taken.", null, response);
            return false;
        }
        return true;
    }

    /**
     * Validates the inputs for user sign-in. Checks that the email is registered
     * and that the password meets criteria.
     * @param email    the email address of the user
     * @param password the password entered by the user
     * @param response the HttpServletResponse object that contains the response the servlet sends to the client
     * @return true if validation is successful, false otherwise
     * @throws ServletException if an error occurs during request handling
     * @throws IOException      if an I/O error occurs during request handling
     * @throws SQLException     if a database error occurs
     */
    private boolean isSignInValid(String email, String password, HttpServletResponse response) throws ServletException, IOException, SQLException {
        if (!StringUtil.isValidEmail(email)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid email.", null, response);
            return false;
        }
        if (!StringUtil.isValidPassword(password)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Invalid password.", null, response);
            return false;
        }
        UserDAO userDAO = new UserDAO();
        if (!userDAO.isEmailTaken(email)) {
            sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Email not registered.", null, response);
            return false;
        }
        return true;
    }

    /**
     * Checks if the username provided in the request is available.
     * Validates the input and queries the database to verify if the username
     * is already taken.
     * Sends a JSON response with a boolean property `isUsernameTaken` indicating
     * the availability of the username.
     * @param jsonRequest the JSON object containing the username to be checked
     * @param response    the HttpServletResponse object to send the JSON response
     * @throws ServletException if an error occurs during request handling
     * @throws IOException      if an I/O error occurs during response handling
     */
    private void handleCheckUsernameAvailability(JsonObject jsonRequest, HttpServletResponse response) throws ServletException, IOException {
        String username = jsonRequest.get("username").getAsString();
        try {
            UserDAO userDAO = new UserDAO();
            if (username == null || username.isEmpty()) {
                sendErrorResponse(HttpServletResponse.SC_BAD_REQUEST, "Username is required.", null, response);
                return;
            }
            boolean isUsernameTaken = userDAO.isUsernameTaken(username);
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("isUsernameTaken", isUsernameTaken);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(jsonResponse));
        } catch (SQLException e) {
            sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.", null, response);
            e.printStackTrace();
        }
    }

    /**
     * Sends an error response with the specified status, message, and redirect URL.
     * @param status   the HTTP status code
     * @param message  the error message
     * @param redirect the redirect URL
     * @param response the HTTP response object
     * @throws IOException if an I/O error occurs during response writing
     */
    private void sendErrorResponse(int status, String message, String redirect, HttpServletResponse response) throws IOException {
        JsonObject jsonObject = new JsonObject();
        if (message != null)
            jsonObject.addProperty("message", message);
        if (redirect != null)
            jsonObject.addProperty("redirect", redirect);
        response.setStatus(status);
        response.getWriter().write(gson.toJson(jsonObject));
    }

    /**
     * Sends a success response with the specified status, message, and redirect URL.
     * @param status    the HTTP status code.
     * @param message   the success message.
     * @param redirect  the redirect URL.
     * @param response  the HTTP response object.
     * @throws IOException if an I/O error occurs during response writing.
     */
    private void sendSuccessResponse(int status, String message, String redirect, HttpServletResponse response) throws IOException {
        JsonObject jsonObject = new JsonObject();
        if (message != null)
            jsonObject.addProperty("message", message);
        if (redirect != null)
            jsonObject.addProperty("redirect", redirect);
        response.setStatus(status);
        response.getWriter().write(gson.toJson(jsonObject));
    }

}