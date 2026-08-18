package com.nahid.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Opens the Swagger UI in the default browser when the application is ready.
 */
@Component
public class SwaggerUiOpener implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${server.port}")
    private String serverPort;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String swaggerPath = "/swagger-ui/index.html";
        String url = "http://localhost:" + serverPort + swaggerPath;
        System.out.println("Swagger UI available at: " + url);

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("Browser opened automatically");
            } else {
                System.out.println("Auto-opening browser not supported on this platform");
            }
        } catch (IOException | URISyntaxException e) {
            System.err.println("Failed to open browser: " + e.getMessage());
        }
    }
}