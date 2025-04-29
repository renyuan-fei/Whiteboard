package org.whiteboard.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.rmi.RemoteException;

public class WhiteboardApplication extends javafx.application.Application {
    private String USERNAME;
    private String HOST;
    private int PORT;
    
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(WhiteboardApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 830);
        stage.setResizable(false);
        stage.setTitle("White board");
        stage.setScene(scene);

        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, evt -> {
            BackgroundWorker.run(HOST, PORT, USERNAME);
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}