package it.polimi.tiw.controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * The SpaServlet class handles requests to render the Single Page Application (SPA) view.
 * It ensures that only authenticated users can access the SPA by checking the session.
 */
public class SpaServlet extends HttpServlet {

    /**
     * Unique identifier for the Serializable class to ensure compatibility
     * during the deserialization process. Changing this value can cause
     * deserialization issues if there are any modifications to the class structure.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Handles the HTTP GET request.
     * This method checks if the user is logged in by verifying the session.
     * If the user is not authenticated, they are redirected to the login page.
     * Otherwise, the SPA view is rendered.
     * @param request  the HttpServletRequest object that contains the request the client made to the servlet
     * @param response the HttpServletResponse object that contains the response the servlet returns to the client
     * @throws ServletException if the request could not be handled
     * @throws IOException      if an input or output error occurs while the servlet is handling the GET request
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        // Render the page
        request.getRequestDispatcher("/WEB-INF/view/spa.html").forward(request, response);
    }

}