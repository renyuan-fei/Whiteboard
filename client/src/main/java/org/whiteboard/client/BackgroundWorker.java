package org.whiteboard.client;

import javafx.concurrent.Task;

public class BackgroundWorker {
    public static void run(String HOST, int PORT, String USERNAME) {
        Task<Void> connectTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // English log
                System.out.println("Starting RMI client in background...");
                WhiteboardClient.createClient(HOST, PORT, USERNAME);
                System.out.println("RMI client started.");
                return null;
            }
        };

        connectTask.setOnFailed(e -> {
            System.err.println("Failed to start RMI client: " + connectTask.getException());
        });

        // run task on a separate daemon thread
        Thread t = new Thread(connectTask);
        t.setDaemon(true);
        t.start();
    }
}
