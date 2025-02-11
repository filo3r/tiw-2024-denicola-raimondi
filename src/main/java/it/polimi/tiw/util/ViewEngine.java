package it.polimi.tiw.util;

import javax.servlet.ServletContext;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

/**
 * ViewEngine is a utility class that provides a singleton instance of the TemplateEngine
 * to render Thymeleaf templates in a Java Servlet-based application.
 * This class is thread-safe and ensures that the TemplateEngine is initialized only once.
 */
public class ViewEngine {

    /**
     * The singleton instance of the TemplateEngine.
     * It is marked as volatile to ensure visibility among threads.
     */
    private static volatile TemplateEngine templateEngine;

    /**
     * Returns the singleton instance of the TemplateEngine.
     * Uses double-checked locking to ensure that the TemplateEngine is initialized
     * only once in a thread-safe manner.
     * @param servletContext the ServletContext of the current web application
     * @return the singleton instance of TemplateEngine
     */
    public static TemplateEngine getTemplateEngine(ServletContext servletContext) {
        // First check without synchronization to improve performance
        if (templateEngine == null) {
            // Synchronize and second check to ensure that only one thread can initialize the TemplateEngine
            synchronized (ViewEngine.class) {
                if (templateEngine == null) {
                    initializeTemplateEngine(servletContext);
                }
            }
        }
        return templateEngine;
    }

    /**
     * Initializes the TemplateEngine with the required template resolver configuration.
     * @param servletContext the ServletContext of the current web application
     */
    private static void initializeTemplateEngine(ServletContext servletContext) {
        // Create a new template resolver to define the configuration for locating templates
        ServletContextTemplateResolver templateResolver = createTemplateResolver(servletContext);
        // Instantiate the TemplateEngine and set the configured template resolver
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
    }

    /**
     * Creates and configures a ClassLoaderTemplateResolver.
     * The template resolver is responsible for finding and reading HTML template files.
     * @return a fully configured ClassLoaderTemplateResolver instance
     */
    private static ServletContextTemplateResolver createTemplateResolver(ServletContext servletContext) {
        // Instantiate a new template resolver that loads resources using the class loader
        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
        // Set the mode to HTML
        templateResolver.setTemplateMode("HTML");
        // Set the prefix path where Thymeleaf will search for template files
        templateResolver.setPrefix("/WEB-INF/view/");
        // Set the suffix for template files; templates should end with ".html"
        templateResolver.setSuffix(".html");
        // Set character encoding to UTF-8 to support international characters
        templateResolver.setCharacterEncoding("UTF-8");
        // Deactivate caching
        templateResolver.setCacheable(false);
        return templateResolver;
    }

}