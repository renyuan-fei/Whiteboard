package org.whiteboard.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import org.whiteboard.client.ConnectionManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

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

//    @FXML
//    public MenuItem fileSaveAsJPG;

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

        fileSave.setOnAction(e -> saveFile());
        fileOpen.setOnAction(e -> openFile());
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

    private void newFile() {
        try {
            connectionManager.newCanvas()
                    .exceptionally(ex -> {
                        System.err.println("Error: Fail to create a new canvas: " + ex.getMessage());

                        return null;
                    });
        } catch (Exception e) {
            connectionManager.getChatController().receiveMessage("System: ", "Fail to create a new canvas");
            System.err.println("Error: Fail to create new canvas: " + e.getMessage());
        }
    }

    private void closeFile() {
        try {
            connectionManager.closeCanvas()
                    .exceptionally(ex -> {
                        System.err.println("Error: Fail to close the canvas: " + ex.getMessage());

                        return null;
                    });
        } catch (Exception e) {
            connectionManager.getChatController().receiveMessage("System: ", "Fail to close a new canvas");
            System.err.println("Error: Fail to close canvas: " + e.getMessage());
        }
    }

    private String getDownloadDirectory() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return userHome + "\\Downloads";
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
            return userHome + "/Downloads";
        } else {
            return userHome;
        }
    }

    private void exportCanvasAsImage(String type) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "canvas_" + timestamp + "." + type;
        try {
            String downloadDir = getDownloadDirectory();
            connectionManager.getCanvasController().exportCanvasAsImage(filename, downloadDir, type);
            connectionManager.getChatController().receiveMessage("System: ", "Canva has saved as " + type + " in " + downloadDir);
        } catch (IOException e) {
            connectionManager.getChatController().receiveMessage("System: ", "Fail to save canvas as image");
            System.err.println("Error: Fail to save canvas as image: " + e.getMessage());
        }
    }

    private void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Canvas File");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Canvas files (*.txt)", "*.txt")
        );


        File selectedFile = chooser.showOpenDialog(menuBar.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        try {
            String content = Files.readString(selectedFile.toPath(), StandardCharsets.UTF_8);

            if (!content.startsWith("CanvasData:")) {
                connectionManager.getChatController()
                        .receiveMessage("System: ", "Invalid canvas file selected.");
                return;
            }

            // Strip header
            String canvasData = content.substring("CanvasData:".length()).stripLeading();

            connectionManager.openCanvas(canvasData).exceptionally(ex -> {
                Platform.runLater(() -> {
                    connectionManager.getChatController().receiveMessage("System: ", "Failed to import canvas, please check canvas data format is correct?");
                });
                System.err.println("Error: Fail to import canvas: " + ex.getMessage());
                return null;
            });

        } catch (IOException ex) {
            connectionManager.getChatController().receiveMessage("System: ", "Unable to read selected file.");
            System.err.println("Error: Fail to import canvas: " + ex.getMessage());
        }
    }


    private void saveFile() {
        CompletableFuture<String> exportFuture = connectionManager.saveCanvas();
        exportFuture.thenAccept(canvasData -> Platform.runLater(() -> {
            if (canvasData == null) {
                connectionManager.getChatController()
                        .receiveMessage("System: ", "No canvas data received from server.");
                return;
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = "canvas_data_" + timestamp + ".txt";
            Path downloadDir = Paths.get(getDownloadDirectory());
            Path filePath = downloadDir.resolve(filename);

            try {
                // Write header + data
                String content = "CanvasData:\n" + canvasData;
                Files.writeString(
                        filePath,
                        content,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );

                connectionManager.getChatController()
                        .receiveMessage("System: ", "Canvas exported to " + filePath);
            } catch (IOException ex) {
                connectionManager.getChatController()
                        .receiveMessage("System: ", "Failed to write canvas file.");
                System.err.println("Error: Fail to write canvas file: " + ex.getMessage());
            }
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                connectionManager.getChatController()
                        .receiveMessage("System: ", "Failed to export canvas: " + ex.getMessage());
            });
            System.err.println("Error: Fail to export canvas: " + ex.getMessage());
            return null;
        });
    }


}