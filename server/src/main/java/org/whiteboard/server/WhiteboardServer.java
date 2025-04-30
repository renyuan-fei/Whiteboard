package org.whiteboard.server;

import org.whiteboard.server.service.WhiteboardService;

public class WhiteboardServer {

    public static void main(String[] args) {
        int PORT = args.length > 0 ? Integer.parseInt(args[0]) : 3000;

        try {
            WhiteboardService.CreateService(PORT);
            System.out.println("Whiteboard server started on port " + PORT);
        } catch (Exception e) {
            System.err.println("Failed to create service");
        }
    }
}
