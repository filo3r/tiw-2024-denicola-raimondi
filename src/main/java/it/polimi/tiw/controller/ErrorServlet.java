package it.polimi.tiw.controller;

import it.polimi.tiw.util.ViewEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * ErrorServlet handles error responses within the web application.
 * It retrieves error details such as HTTP status codes and exceptions
 * from the request attributes and renders a custom error page using Thymeleaf.
 */
public class ErrorServlet extends HttpServlet {

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
     * Handles GET requests to the servlet.
     * Retrieves error details such as HTTP status codes and exceptions,
     * logs server-side errors, and renders a custom error page.
     * @param request  the HTTP request object.
     * @param response the HTTP response object.
     * @throws ServletException if an error occurs during processing.
     * @throws IOException      if an I/O error occurs during processing.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Get HTTP status code
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        // Server side problem log
        Throwable exception = (Throwable) request.getAttribute("javax.servlet.error.exception");
        if (exception != null)
            exception.printStackTrace();
        // Render
        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale());
        webContext.setVariable("statusCode", statusCode != null ? statusCode : "N/A");
        webContext.setVariable("redirectTimeoutSeconds", 5);
        String homeUrl = request.getContextPath() + "/home";
        response.setHeader("Refresh", "5; URL=" + homeUrl);
        templateEngine.process("error.html", webContext, response.getWriter());
    }

}