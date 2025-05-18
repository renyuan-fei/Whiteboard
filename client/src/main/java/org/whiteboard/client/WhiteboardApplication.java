package org.whiteboard.client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class WhiteboardApplication extends javafx.application.Application {
    // Default values
    private String username = UUID.randomUUID().toString().substring(0, 8);
    private String host = "127.0.0.1";
    private int port = 3000;
    private boolean isAdmin = false;

    @Override
    public void init() throws Exception {
        super.init();

        Parameters params = getParameters();
        List<String> args = params.getUnnamed();

        // <Mode> <serverIPAddress> <serverPort> <username>
        if (args.size() == 4) {
            String mode = args.get(0);
            this.host = args.get(1);

            try {
                this.port = Integer.parseInt(args.get(2));
            } catch (NumberFormatException ex) {
                System.err.println("Error: Invalid port number provided: " + args.get(2) + ". Using default port " + this.port);
            }

            this.username = args.get(3);

            if ("CreateWhiteBoard".equalsIgnoreCase(mode)) {
                this.isAdmin = true;
                System.out.println("Mode: CreateWhiteBoard (Admin)");
            } else if ("JoinWhiteBoard".equalsIgnoreCase(mode)) {
                this.isAdmin = false;
                System.out.println("Mode: JoinWhiteBoard (User)");
            } else {
                System.err.println("Error: Invalid mode specified: " + mode + ". Defaulting to JoinWhiteBoard (User).");
                this.isAdmin = false;
            }

            System.out.println("Using command line arguments: host=" + this.host + ", port=" + this.port + ", username=" + this.username);

            // Didn't provide all arguments
        } else if (!args.isEmpty()) {
            System.err.println("Error: Usage: java -jar your-app.jar <Mode> <serverIPAddress> <serverPort> <username>");
            System.err.println("Error: Mode can be 'CreateWhiteBoard' or 'JoinWhiteBoard'.");
            System.err.println("Error: Incorrect number of arguments provided (" + args.size() + " instead of 4). Using default values.");
            printDefaultValues();

            // No command arguments provided
        } else {
            System.out.println("No command line arguments provided. Using default values.");
            printDefaultValues();
        }
    }

    private void printDefaultValues() {
        System.out.println("Defaults: Mode=JoinWhiteBoard (User), host=" + this.host + ", port=" + this.port + ", username=" + this.username);
    }


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(WhiteboardApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1700, 830);
        stage.setResizable(false);

        // Set the title based on the mode
        String titleSuffix = isAdmin ? " (Admin)" : " (User)";
        stage.setTitle("White board - User: " + this.username + titleSuffix);

        stage.setScene(scene);

        // App start
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, evt -> {
            String host = this.host;
            int port = this.port;
//            String username = UUID.randomUUID().toString().substring(0, 8); // Example
            System.out.println("Attempting connection to " + host + ":" + port + " as " + username);
            BackgroundWorker.run(host, port, username, isAdmin);
        });


        Platform.setImplicitExit(true);

        stage.show();
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Application stopping...");

        // Ensure cleanup runs even if setOnCloseRequest wasn't triggered or completed fully
        ConnectionManager.getInstance().shutdown();

        super.stop();
        Platform.setImplicitExit(true);

        //TODO fix exit
        System.exit(0);
    }


    public static void main(String[] args) {
        launch(args);
    }
}