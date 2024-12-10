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

public class ErrorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private TemplateEngine templateEngine;

    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();
        this.templateEngine = ViewEngine.getTemplateEngine(servletContext);
    }

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