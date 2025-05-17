package org.whiteboard.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import org.whiteboard.client.ConnectionManager;

import java.util.List;

public class UsersController {

    @FXML
    private Button kickUserButton;

    @FXML
    private ListView<Node> userList;

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    @FXML
    public void initialize() {
        // inject the controller into the connection manager
        connectionManager.setUsersController(this);

        kickUserButton.setOnAction(e -> {
            Node selected = userList.getSelectionModel().getSelectedItem();
            if (selected instanceof HBox) {
                Label lbl = (Label) ((HBox) selected).getChildren().getFirst();
                kickUser(lbl.getText());
            }
        });

        // default cell factory to render nodes directly
        userList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Node item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(item);
                }
            }
        });
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

    public void initialUserList(List<String> usernames, boolean isAdmin) {
        ObservableList<Node> items = FXCollections.observableArrayList();
        for (String user : usernames) {
            items.add(createUserCell(user));
        }
        userList.setItems(items);

        if (isAdmin) {
            kickUserButton.setVisible(true);
        }
    }

    public void addNewUser(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }

        userList.getItems().add(createUserCell(username));
    }

    public void removeUser(String username) {
        userList.getItems().removeIf(node -> {
            if (node instanceof HBox) {
                Label lbl = (Label) ((HBox) node).getChildren().getFirst();
                return username.equals(lbl.getText());
            }
            return false;
        });
    }

    public void askUserJoin(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }
        HBox cell = createUserCellWithActions(username);
        userList.getItems().add(cell);
    }

    private HBox createUserCell(String username) {
        Label nameLabel = new Label(username);
        return new HBox(8, nameLabel);
    }

    private HBox createUserCellWithActions(String username) {
        Label nameLabel = new Label(username);
        Button acceptBtn = new Button("Accept");
        Button refuseBtn = new Button("Refuse");

        acceptBtn.setOnAction(e -> handleAccept(username));
        refuseBtn.setOnAction(e -> handleRefuse(username));

        return new HBox(8, nameLabel, acceptBtn, refuseBtn);
    }

    private void handleAccept(String username) {
        System.out.println("Accepted: " + username);
        connectionManager.acceptUserJoin(username)
                .exceptionally(ex -> {
                    System.err.println("Error: Fail to accept user join: " + ex.getMessage());
                    return null;
                });
        removeUser(username);
    }

    private void handleRefuse(String username) {
        System.out.println("Refused: " + username);
        connectionManager.refuseUserJoin(username)
                .exceptionally(ex -> {
                    System.err.println("Error: Fail to refuse user join: " + ex.getMessage());
                    return null;
                });
        removeUser(username);
    }
}
