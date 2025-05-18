package org.whiteboard.server;

import org.whiteboard.common.rmi.IClientCallback;
import org.whiteboard.server.service.FileService;
import org.whiteboard.server.service.UserService;
import org.whiteboard.server.service.WhiteboardService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    public static void main(String[] args) {
        int port = 3000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number provided: " + args[0] + ". Using default port " + port);
            }
        }

        try {
            System.out.println("Starting Whiteboard server on port " + port + "...");

            Map<String, IClientCallback> clients = new ConcurrentHashMap<>();

            UserService userService = new UserService(clients);
            WhiteboardService whiteboardService = new WhiteboardService(clients);
            FileService fileService = new FileService();

            // Create and start the server
            WhiteboardServer server = WhiteboardServer.CreateServer(
                    port,
                    whiteboardService,
                    fileService,
                    userService
            );

            System.out.println("Server started successfully on port " + port);

            // Add Shutdown Hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown hook triggered. Shutting down server...");
                server.shutdown();
                System.out.println("Server shutdown process finished.");
            }, "ServerShutdownHook"));

        } catch (Exception ex) {
            System.err.println("FATAL: Error during server startup: " + ex.getMessage());

            // Exit if server failed to start
            System.exit(1);
        }
    }
}