package org.whiteboard.client;

import javafx.application.Platform;
import org.whiteboard.client.controller.CanvasController;
import org.whiteboard.client.controller.ChatController;
import org.whiteboard.client.controller.MainController;
import org.whiteboard.client.controller.UsersController;
import org.whiteboard.common.action.Action;
import org.whiteboard.common.action.DrawAction;
import org.whiteboard.common.action.EraseAction;
import org.whiteboard.common.action.TextAction;
import org.whiteboard.common.rmi.IClientCallback;
import org.whiteboard.common.rmi.IWhiteboardServer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class WhiteboardClient implements IClientCallback {

    private static final String SERVICE_NAME = "WhiteboardServer";
    private final IWhiteboardServer whiteboardServer;
    private final String username;


    public static WhiteboardClient createClient(boolean isAdmin, String host, int port, String username) throws RemoteException {
        return new WhiteboardClient(isAdmin, host, port, username);
    }

    /**
     * Protected constructor contains join logic.
     *
     * @param host     the host name of the server
     * @param port     the port number of the server
     * @param username the username
     */
    protected WhiteboardClient(boolean isAdmin, String host, int port, String username) throws RemoteException {
        // Export the client object to make it available for remote calls
        UnicastRemoteObject.exportObject(this, 0);

        this.username = username;

        // Connect to the server with retry
        IWhiteboardServer whiteboardServer = connectWithRetry(5, 2000, host, port, SERVICE_NAME);

        if (whiteboardServer == null) {
            throw new RemoteException("Could not connect to service");
        }

        this.whiteboardServer = whiteboardServer;

        ConnectionManager.getInstance().setConnected(true);

        try {
            this.whiteboardServer.registerClient(isAdmin, username, this);
        } catch (RemoteException e) {
            throw new RemoteException("Failed to register callback ", e);
        }
    }

    /**
     * Getter for the remote service stub
     */
    public IWhiteboardServer getWhiteboardServer() {
        return whiteboardServer;
    }


    /**
     * Disconnect from the server and unregister the client.
     */
    public void disconnect() {
        if (ConnectionManager.getInstance().isConnected()) return;

        try {
            whiteboardServer.unregisterClient(username);
            ConnectionManager.getInstance().setConnected(false);
            Platform.runLater(() -> {
                ChatController ctrl = ConnectionManager.getInstance().getChatController();
                ctrl.receiveMessage("Warning! ", "Disconnected from server.");
            });
        } catch (RemoteException e) {
            System.err.println("Error: Failed to unregister client: " + e.getMessage());
        } finally {

            // Unexport the object
            try {
                UnicastRemoteObject.unexportObject(this, true);
            } catch (Exception e) {
                System.err.println("Error: Failed to unexport client: " + e.getMessage());
            }
        }
    }

    @Override
    public void onAction(Action action) throws RemoteException {
        switch (action) {
            case DrawAction draw -> Platform.runLater(() -> {
                CanvasController ctrl = ConnectionManager.getInstance().getCanvasController();
                if (ctrl != null) {
                    ctrl.renderRemoteDrawAction(draw);
                }
            });
            case EraseAction erase -> Platform.runLater(() -> {
                CanvasController ctrl = ConnectionManager.getInstance().getCanvasController();
                if (ctrl != null) {
                    ctrl.renderRemoteEraseAction(erase);
                }
            });
            case TextAction text -> Platform.runLater(() -> {
                CanvasController ctrl = ConnectionManager.getInstance().getCanvasController();
                if (ctrl != null) {
                    if (text.getTextType() == TextAction.TextType.ADD) {
                        ctrl.renderRemoteTextAction(text);
                    } else if (text.getTextType() == TextAction.TextType.REMOVE) {
                        ctrl.renderRemoteRemoveTextActions(text);
                    }
                }
            });
            default -> System.err.println("Error: Unknown action type: " + action.getClass().getName());
        }
    }

    @Override
    public void onSendMessage(String username, String message) throws RemoteException {
        Platform.runLater(() -> {
            ChatController ctrl = ConnectionManager.getInstance().getChatController();
            if (ctrl != null) {
                ctrl.receiveMessage(username, message);
            }
        });
    }

    @Override
    public void onInitialClientState(List<String> usernames, boolean isAdmin) throws RemoteException {
        Platform.runLater(() -> {
            UsersController utrl = ConnectionManager.getInstance().getUsersController();

            if (utrl != null) {
                utrl.initialUserList(usernames, isAdmin);
            }

            MainController mtrl = ConnectionManager.getInstance().getMainController();

            if (mtrl != null) {
                mtrl.initialClient(isAdmin);
            }
        });
    }

    @Override
    public void onAddUser(String username) throws RemoteException {
        Platform.runLater(() -> {
            UsersController utrl = ConnectionManager.getInstance().getUsersController();

            if (utrl != null) {
                utrl.addNewUser(username);
            }
        });
    }

    @Override
    public void onRemoveUser(String username) throws RemoteException {
        Platform.runLater(() -> {
            UsersController utrl = ConnectionManager.getInstance().getUsersController();

            if (utrl != null) {
                utrl.removeUser(username);
            }
        });
    }

    @Override
    public void onKicked(String message) throws RemoteException {

        disconnect();
        Platform.runLater(() -> {
            ChatController ctrl = ConnectionManager.getInstance().getChatController();
            if (ctrl != null) {
                ctrl.receiveMessage("Warning! ", message);
            }

            MainController mtrl = ConnectionManager.getInstance().getMainController();

            if (ctrl != null) {
                mtrl.disable();
            }
        });
    }

    @Override
    public void onServerShutdown(String reason) throws RemoteException {
        System.out.println("Received server shutdown notification: " + reason);
        // Trigger client-side cleanup, similar to disconnect but without calling the server
        cleanupLocalResources();
    }

    @Override
    public void onSyncWhiteboard(String canvasData) throws RemoteException {
        Platform.runLater(() -> {
            CanvasController ctrl = ConnectionManager.getInstance().getCanvasController();

            ctrl.importCanvas(canvasData);
        });
    }

    @Override
    public void onAskUserJoin(String username) throws RemoteException {
        Platform.runLater(() -> {
            // ask admin
            UsersController utrl = ConnectionManager.getInstance().getUsersController();
            if (utrl != null) {
                utrl.askUserJoin(username);
            }
        });
    }

    /**
     * Performs local resource cleanup without contacting the server.
     */
    private void cleanupLocalResources() {
        System.out.println("Cleaning up local client resources for: " + username);

        // Unexport the RMI object
        try {
            UnicastRemoteObject.unexportObject(this, true);
            System.out.println("Successfully unexported client callback object.");
        } catch (Exception e) {
            System.err.println("Error during client callback unexport: " + e.getMessage());
        }
    }


    /**
     * Connect to the RMI registry and look up the service with retry logic.
     *
     * @param maxRetries   maximum number of retries
     * @param retryDelayMs initial delay between retries in milliseconds
     * @param host         the host name of the server
     * @param port         the port number of the server
     * @param serviceName  the name of the service
     */
    private static IWhiteboardServer connectWithRetry(int maxRetries, long retryDelayMs, String host, int port, String serviceName) {
        int attempt = 1;
        while (attempt <= maxRetries) {
            try {
                // Try to get the registry and look up the service
                Registry registry = LocateRegistry.getRegistry(host, port);
                return (IWhiteboardServer) registry.lookup(serviceName);

            } catch (Exception ex) {
                System.err.format("Attempt %d failed: %s%n", attempt, ex.getMessage());
                if (attempt == maxRetries) {
                    break;
                }
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }

                retryDelayMs *= 2; // Exponential backoff
                attempt++;
            }
        }
        return null;
    }
}
