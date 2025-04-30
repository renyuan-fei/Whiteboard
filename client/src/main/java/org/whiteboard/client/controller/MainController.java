package org.whiteboard.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;

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
    private Pane mainPane;
}