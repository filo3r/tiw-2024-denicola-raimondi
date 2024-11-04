package it.polimi.tiw.controller;

import it.polimi.tiw.dao.UserDAO;
import it.polimi.tiw.model.User;
import it.polimi.tiw.util.PasswordEncrypt;
import it.polimi.tiw.util.StringUtil;
import it.polimi.tiw.util.ViewEngine;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * IndexServlet handles HTTP GET and POST requests for the application's main page.
 * It allows users to sign up or sign in and validates user credentials through a series of checks.
 * The servlet communicates with the UserDAO for database operations and uses a TemplateEngine
 * to render dynamic content on the index page.
 */
public class IndexServlet extends HttpServlet {

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
     * Initializes the servlet by setting up the template engine.
     * @throws ServletException if an error occurs during initialization
     */
    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();
        this.templateEngine = ViewEngine.getTemplateEngine(servletContext);
    }

    /**
     * Handles GET requests by displaying the index page with the specified panel.
     * @param request  the HttpServletRequest object that contains the client request
     * @param response the HttpServletResponse object that contains the response the servlet sends to the client
     * @throws ServletException if an error occurs during request handling
     * @throws IOException      if an I/O error occurs during request handling
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String activePanel = request.getParameter("panel");
        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale());
        if (activePanel != null) {
            webContext.setVariable("activePanel", activePanel);
        }
        templateEngine.process("index.html", webContext, response.getWriter());
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
        String action = request.getParameter("action");
        if ("signUp".equals(action))
            handleSignUp(request, response);
        else if ("signIn".equals(action))
            handleSignIn(request, response);
    }

    /**
     * Handles the user sign-up process by validating inputs and registering
     * the user in the database if validation is successful.
     * @param request  the HttpServletRequest object that contains the client request
     * @param response the HttpServletResponse object that contains the response the servlet sends to the client
     * @throws ServletException if an error occurs during request handling
     * @throws IOException      if an I/O error occurs during request handling
     */
    private void handleSignUp(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        String username = request.getParameter("username");
        String password1 = request.getParameter("password1");
        String password2 = request.getParameter("password2");
        try {
            if (!isSignUpValid(email, username, password1, password2, request, response))
                return;
            UserDAO userDAO = new UserDAO();
            User user = new User(username, email, PasswordEncrypt.hashPassword(password1));
            boolean success = userDAO.registerUser(user);
            if (success) {
                request.getSession().setAttribute("user", user);
                response.sendRedirect("./home");
            } else {
                showErrorPage(request, response, "Wrong credentials.", "signUpErrorMessage", "signUp");
                return;
            }
        } catch (SQLException e) {
            showErrorPage(request, response, "Database error. Please try again.", "signUpErrorMessage", "signUp");
            e.printStackTrace();
        }
    }

    /**
     * Handles the user sign-in process by validating inputs and logging
     * in the user if validation is successful.
     * @param request  the HttpServletRequest object that contains the client request
     * @param response the HttpServletResponse object that contains the response the servlet sends to the client
     * @throws ServletException if an error occurs during request handling
     * @throws IOException      if an I/O error occurs during request handling
     */
    private void handleSignIn(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        try {
            if (!isSignInValid(email, password, request, response))
                return;
            UserDAO userDAO = new UserDAO();
            User user = new User(null, email, password);
            boolean success = userDAO.loginUser(user);
            if (success) {
                user.setPassword(null);
                user.setUsername(userDAO.getUsernameByEmail(user.getEmail()));
                request.getSession().setAttribute("user", user);
                response.sendRedirect("./home");
            } else {
                showErrorPage(request, response, "Unknown error. Please try again.", "signInErrorMessage", "signIn");
                return;
            }
        } catch (SQLException e) {
            showErrorPage(request, response, "Database error. Please try again.", "signInErrorMessage", "signIn");
            e.printStackTrace();
        }
    }

    /**
     * Validates the inputs for user sign-up. Ensures that the email, username, and passwords
     * meet criteria and are not already in use.
     * @param email    the email address of the user
     * @param username the chosen username of the user
     * @param password1 the password entered by the user
     * @param password2 the repeated password entered by the user
     * @param request  the HttpServletRequest object that contains the client request
     * @param response the HttpServletResponse object that contains the response the servlet sends to the client
     * @return true if validation is successful, false otherwise
     * @throws ServletException if an error occurs during request handling
     * @throws IOException      if an I/O error occurs during request handling
     * @throws SQLException     if a database error occurs
     */
    private boolean isSignUpValid(String email, String username, String password1, String password2, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException {
        if (!StringUtil.isValidEmail(email)) {
            showErrorPage(request, response, "Invalid email.", "signUpErrorMessage", "signUp");
            return false;
        }
        if (!StringUtil.isValidUsername(username)) {
            showErrorPage(request, response, "Invalid username.", "signUpErrorMessage", "signUp");
            return false;
        }
        if (!StringUtil.isValidPassword(password1)) {
            showErrorPage(request, response, "Invalid password.", "signUpErrorMessage", "signUp");
            return false;
        }
        if (!password1.equals(password2)) {
            showErrorPage(request, response, "Passwords don't match.", "signUpErrorMessage", "signUp");
            return false;
        }
        UserDAO userDAO = new UserDAO();
        if (userDAO.isEmailTaken(email)) {
            showErrorPage(request, response, "Email already taken.", "signUpErrorMessage", "signUp");
            return false;
        }
        if (userDAO.isUsernameTaken(username)) {
            showErrorPage(request, response, "Username already taken.", "signUpErrorMessage", "signUp");
            return false;
        }
        return true;
    }

    /**
     * Validates the inputs for user sign-in. Checks that the email is registered
     * and that the password meets criteria.
     * @param email    the email address of the user
     * @param password the password entered by the user
     * @param request  the HttpServletRequest object that contains the client request
     * @param response the HttpServletResponse object that contains the response the servlet sends to the client
     * @return true if validation is successful, false otherwise
     * @throws ServletException if an error occurs during request handling
     * @throws IOException      if an I/O error occurs during request handling
     * @throws SQLException     if a database error occurs
     */
    private boolean isSignInValid(String email, String password, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException {
        if (!StringUtil.isValidEmail(email)) {
            showErrorPage(request, response, "Invalid email.", "signInErrorMessage", "signIn");
            return false;
        }
        if (!StringUtil.isValidPassword(password)) {
            showErrorPage(request, response, "Invalid password.", "signInErrorMessage", "signIn");
            return false;
        }
        UserDAO userDAO = new UserDAO();
        if (!userDAO.isEmailTaken(email)) {
            showErrorPage(request, response, "Email not registered.", "signInErrorMessage", "signIn");
            return false;
        }
        return true;
    }

    /**
     * Displays an error page with a specific error message and active panel.
     * @param request       the HttpServletRequest object that contains the client request
     * @param response      the HttpServletResponse object that contains the response the servlet sends to the client
     * @param errorMessage  the error message to display
     * @param errorAttribute the attribute name for the error message
     * @param panel         the panel to be shown as active
     * @throws ServletException if an error occurs during request handling
     * @throws IOException      if an I/O error occurs during request handling
     */
    private void showErrorPage(HttpServletRequest request, HttpServletResponse response, String errorMessage, String errorAttribute, String panel) throws ServletException, IOException {
        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale());
        webContext.setVariable(errorAttribute, errorMessage);
        webContext.setVariable("activePanel", panel);
        templateEngine.process("index.html", webContext, response.getWriter());
    }

}