package org.whiteboard.client;

import javafx.concurrent.Task;
import org.whiteboard.common.rmi.IClientCallback;
import org.whiteboard.common.rmi.IWhiteboardServer;

public class BackgroundWorker {
    public static void run(String host, int port, String username, boolean isAdmin) {
        Task<Void> backgroundTask = new Task<>() {
            @Override
            protected Void call() throws Exception {

                System.out.println("Starting RMI client in background...");

                IClientCallback callback = WhiteboardClient.createClient(isAdmin, host, port, username);
                IWhiteboardServer whiteboardServer = callback.getWhiteboardServer();

                // Initialize the connection manager with the whiteboardServer and callback
                ConnectionManager.getInstance().init(whiteboardServer, callback, username, isAdmin);

                System.out.println("RMI client started.");
                return null;
            }
        };

        backgroundTask.setOnFailed(ex -> {
            System.err.println("Error: Failed to start RMI client: " + backgroundTask.getException());

            // TODO fix exit
            System.exit(0);
        });

        // run task on a separate daemon thread
        Thread t = new Thread(backgroundTask);
        t.setName("BackgroundWorker-" + t.threadId());
        t.setDaemon(true);
        t.start();
    }
}
