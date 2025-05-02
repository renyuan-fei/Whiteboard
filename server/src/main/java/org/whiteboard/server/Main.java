package org.whiteboard.server;

import org.whiteboard.server.service.FileService;
import org.whiteboard.server.service.UserService;
import org.whiteboard.server.service.WhiteboardService;

public class Main {
    public static void main(String[] args) {
        int PORT = args.length > 0 ? Integer.parseInt(args[0]) : 3000;

        try {
            WhiteboardServer.CreateServer(
                    PORT,
                    new WhiteboardService(),
                    new FileService(),
                    new UserService()
            );
            System.out.println("Server started on port " + PORT);
        } catch (Exception e) {
            System.err.println("Failed to create service");
        }
    }
}
