package com.jiraclone.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;

@Component
public class PortCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    private static final Logger log = LoggerFactory.getLogger(PortCustomizer.class);

    private static final int BASE_PORT = 8080;
    private static final int MAX_PORT  = 8090;

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        int port = findAvailablePort(BASE_PORT);
        if (port != BASE_PORT) {
            log.warn("Port {} is in use — starting on port {} instead", BASE_PORT, port);
        }
        factory.setPort(port);
    }

    private int findAvailablePort(int startPort) {
        for (int port = startPort; port <= MAX_PORT; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        throw new IllegalStateException(
            "No available port found in range " + startPort + "-" + MAX_PORT);
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
