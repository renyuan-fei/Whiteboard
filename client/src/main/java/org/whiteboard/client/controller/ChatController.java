package org.whiteboard.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.whiteboard.client.ConnectionManager;

public class ChatController {
    @FXML
    private TextFlow textFlow;

    @FXML
    private TextField inputField;

    @FXML
    private Button sendButton;

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    @FXML
    public void initialize() {
        // inject the controller into the connection manager
        ConnectionManager.getInstance().setChatController(this);

        // Set up action for the send button
        sendButton.setOnAction(e -> sendMessage());
    }

    private void sendMessage() {
        String message = inputField.getText();

        // Only send it if a message is not empty
        if (!message.isEmpty()) {
            Platform.runLater(() -> {
                textFlow.getChildren().
                        add(new Text(connectionManager.getUsername() + ": " + message + "\n"));
            });

            connectionManager.sendChatMessage(connectionManager.getUsername(), message)
                    .exceptionally(ex -> {
                        System.err.println("Error: Fail to send message: " + ex.getMessage());

                        return null;
                    });

            // Clear the input field after sending
            inputField.setText("");

            // Set focus back to the input field
            inputField.requestFocus();
        }

    }

    public void receiveMessage(String username, String message) {
        Text messageText = new Text(username + ": " + message + "\n");
        textFlow.getChildren().add(messageText);
    }
}