package org.whiteboard.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import org.whiteboard.client.ConnectionManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    public MenuItem fileSaveAsPNG;

    @FXML
    public MenuItem fileSaveAsJPG;

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

        fileNew.setOnAction(e -> newFile());
        fileClose.setOnAction(e -> closeFile());

        fileSaveAsPNG.setOnAction(e -> exportCanvasAsImage("png"));
        fileSaveAsPNG.setOnAction(e -> exportCanvasAsImage("jpg"));
    }

    public void initialClient(boolean isAdmin) {
        if (isAdmin) {
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

    public void setLabelText(String text) {
        label.setText(text);
        label.setVisible(true);
    }

    public void hideLabelText() {
        label.setText("");
        label.setVisible(false);
    }

    private void exportCanvasAsJSON() {

    }

    private void exportCanvasAsImage(String type) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "canvas_" + timestamp + "." + type;
        try {
            connectionManager.getCanvasController().exportCanvasAsImage(filename, type);
            connectionManager.getChatController().receiveMessage("System: ", "Canva has saved as png");
        } catch (IOException e) {
            connectionManager.getChatController().receiveMessage("System: ", "Fail to save canvas as image");
            System.err.println("Error: Fail to save canvas as image: " + e.getMessage());
        }
    }

    private void newFile() {
        try {
            connectionManager.newCanvas();
        } catch (Exception e) {
            connectionManager.getChatController().receiveMessage("System: ", "Fail to create a new canvas");
            System.err.println("Error: Fail to create new canvas: " + e.getMessage());
        }
    }

    private void closeFile() {
        try {
            connectionManager.closeCanvas();
        } catch (Exception e) {
            connectionManager.getChatController().receiveMessage("System: ", "Fail to close a new canvas");
            System.err.println("Error: Fail to close canvas: " + e.getMessage());
        }
    }
//
//    private void openFile() {}
//
//    private void saveFile() {}
//
}