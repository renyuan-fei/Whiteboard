package org.whiteboard.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import org.whiteboard.client.ConnectionManager;

public class MainController {

    @FXML
    public MenuBar menuBar;

    @FXML
    public Menu menuFile;

    @FXML
    public MenuItem fileNew;
    @FXML
    public MenuItem fileOpen;

    @FXML
    public MenuItem fileSave;

    @FXML
    public MenuItem fileSaveAs;

    @FXML
    public MenuItem fileClose;

    @FXML
    public Label label;

    @FXML
    private Pane mainPane;

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    @FXML
    public void initialize() {
        // inject the controller into the connection manager
        ConnectionManager.getInstance().setMainController(this);
    }

    public void initialClient(boolean isAdmin) {
        if (isAdmin) {
            System.out.println("Admin");
            menuFile.setVisible(true);
            menuFile.setDisable(false);
        }

        mainPane.setDisable(false);

        label.setVisible(false);
        label.setDisable(true);
    }

    public void disable() {
        mainPane.setDisable(true);

        label.setVisible(true);
        label.setDisable(false);
        label.setText("You are not connected to the server.");
    }

//    private void exportCanvasAsJSON() {
//
//    }
//
//    private void exportCanvasAsImage() {
//
//    }
//
//    private void newFile() {}
//
//    private void openFile() {}
//
//    private void saveFile() {}
//
//    private void closeFile() {}

}