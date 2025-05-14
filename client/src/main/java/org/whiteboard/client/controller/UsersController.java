package org.whiteboard.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import org.whiteboard.client.ConnectionManager;

import java.util.List;

public class UsersController {

    @FXML
    private Button kickUserButton;

    @FXML
    private ListView<String> userList;

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    @FXML
    public void initialize() {
        // inject the canvas controller into the connection manager
        ConnectionManager.getInstance().setUsersController(this);

        kickUserButton.setOnAction(e -> kickUser(userList.getSelectionModel().getSelectedItem()));
    }

    private void kickUser(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }

        System.out.println("Kicking user: " + username);

        connectionManager.kickUser(username)
                .exceptionally(ex -> {
                    System.err.println("Error: Fail to kick user: " + ex.getMessage());

                    return null;
                });
    }

    public void initialUserList(List<String> usernames) {
        if (connectionManager.isAdmin()) {
            kickUserButton.setVisible(true);
        }
        ObservableList<String> names = FXCollections.observableArrayList(usernames);
        userList.setItems(names);
    }

    public void addNewUser(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }

        // Get the items from the ListView and cast to the correct type
        ObservableList<String> items = userList.getItems();

        // Add the new username if it doesn't already exist
        if (!items.contains(username)) {
            items.add(username);
        }
    }

    public void removeUser(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }

        // Get the items from the ListView and cast to the correct type
        ObservableList<String> items = userList.getItems();

        // Remove the username if it exists
        items.remove(username);
    }
}