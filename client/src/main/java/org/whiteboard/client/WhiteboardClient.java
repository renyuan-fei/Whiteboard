package org.whiteboard.client;

import javafx.application.Platform;
import org.whiteboard.client.controller.CanvasController;
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

public class WhiteboardClient implements IClientCallback {

    private static final String SERVICE_NAME = "WhiteboardServer";
    private final IWhiteboardServer whiteboardServer;
    private final String username;

    /**
     * Create a new client instance.
     *
     * @param host     the host name of the server
     * @param port     the port number of the server
     * @param username the username
     * @return a new client instance
     * @throws RemoteException on network error
     */
    public static WhiteboardClient createClient(String host, int port, String username) throws RemoteException {
        return new WhiteboardClient(false, host, port, username);
    }

    public static WhiteboardClient createClient(String host, int port, String username, boolean isAdmin) throws RemoteException {
        return new WhiteboardClient(isAdmin, host, port, username);
    }

    /**
     * Protected constructor contains join logic.
     *
     * @param host     the host name of the server
     * @param port     the port number of the server
     * @param username the username
     * @throws RemoteException on network error
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

        try {
            this.whiteboardServer.registerClient(isAdmin, username, this);
        } catch (RemoteException e) {
            throw new RemoteException("Failed to register callback", e);
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
        try {
            whiteboardServer.unregisterClient(username);
        } catch (RemoteException e) {
            System.err.println("Failed to unregister client: " + e.getMessage());
        } finally {

            // Unexport the object
            try {
                UnicastRemoteObject.unexportObject(this, true);
            } catch (Exception e) {
                System.err.println("Failed to unexport client: " + e.getMessage());
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
            case EraseAction erase ->
                    Platform.runLater(() -> {
                        CanvasController ctrl = ConnectionManager.getInstance().getCanvasController();
                        if (ctrl != null) {
                            ctrl.renderRemoteEraseAction(erase);
                        }
                    });
            case TextAction text -> {
                Platform.runLater(() -> {
                    CanvasController ctrl = ConnectionManager.getInstance().getCanvasController();
                    if (ctrl != null) {
                        if (text.getType() == TextAction.TextType.ADD) {
                            ctrl.renderRemoteTextAction(text);
                        } else if (text.getType() == TextAction.TextType.REMOVE) {
                            ctrl.renderRemoteRemoveTextActions(text);
                        }
                    }
                });
            }
            default -> System.err.println("Unknown action type: " + action.getClass().getName());
        }
    }

    @Override
    public void onSendMessage(String username, String message) throws RemoteException {
        System.out.println(username + ": " + message);
    }

    @Override
    public void onKicked() throws RemoteException {
        disconnect();
    }

    /**
     * Connect to the RMI registry and lookup the service with retry logic.
     *
     * @param maxRetries   maximum number of retries
     * @param retryDelayMs initial delay between retries in milliseconds
     * @param host         the host name of the server
     * @param port         the port number of the server
     * @param serviceName  the name of the service
     * @return a reference to the IWhiteboardService or null if it fails
     */
    private static IWhiteboardServer connectWithRetry(int maxRetries, long retryDelayMs, String host, int port, String serviceName) {
        int attempt = 1;
        while (attempt <= maxRetries) {
            try {
                // Try to get the registry and lookup the service
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
