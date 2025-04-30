package org.whiteboard.client;

import javafx.concurrent.Task;
import org.whiteboard.common.rmi.IClientCallback;
import org.whiteboard.common.rmi.IWhiteboardService;

public class BackgroundWorker {
    public static void run(String HOST, int PORT, String USERNAME) {
        Task<Void> backgroundTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // English log
                System.out.println("Starting RMI client in background...");

                IClientCallback callback = WhiteboardClient.createClient(HOST, PORT, USERNAME);
                IWhiteboardService service = callback.getService();

                // Initialize the connection manager with the service and callback
                ConnectionManager.getInstance().init(service, callback, USERNAME);

                System.out.println("RMI client started.");
                return null;
            }
        };

        backgroundTask.setOnFailed(e -> {
            System.err.println("Failed to start RMI client: " + backgroundTask.getException());
        });

        // run task on a separate daemon thread
        Thread t = new Thread(backgroundTask);
        t.setDaemon(true);
        t.start();
    }
}
