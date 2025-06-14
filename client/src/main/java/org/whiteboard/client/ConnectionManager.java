package org.whiteboard.client;

import org.whiteboard.client.controller.CanvasController;
import org.whiteboard.client.controller.ChatController;
import org.whiteboard.client.controller.MainController;
import org.whiteboard.client.controller.UsersController;
import org.whiteboard.common.action.DrawAction;
import org.whiteboard.common.action.EraseAction;
import org.whiteboard.common.action.TextAction;
import org.whiteboard.common.rmi.IClientCallback;
import org.whiteboard.common.rmi.IWhiteboardServer;

import java.rmi.RemoteException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConnectionManager {
    // Eager singleton
    private static final ConnectionManager INSTANCE = new ConnectionManager();
    private boolean isAdmin;
    private String username;

    private volatile boolean connected;
    private IWhiteboardServer server;
    private IClientCallback callback;

    private MainController mainController;

    private CanvasController canvasController;

    private ChatController chatController;

    private UsersController usersController;

    // Executor for background network tasks
    // Using a cached thread pool suitable for potentially many short-lived tasks
    private final ExecutorService networkExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread t = Executors.defaultThreadFactory().newThread(runnable);
        t.setName("ConnectionManager-NetWorker-Executor" + t.threadId());
        t.setDaemon(true);
        return t;
    });


    private ConnectionManager() {
    }

    public static ConnectionManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initialize RMI connection details.
     * Should be called after successful connection setup
     *
     * @param service  remote service stub
     * @param callback local callback stub
     * @param username the client's username
     */
    public void init(IWhiteboardServer service, IClientCallback callback, String username, boolean isAdmin) {
        if (service == null) {
            System.err.println("Error: IWhiteboardServer service cannot be null in ConnectionManager.init()");
            throw new IllegalArgumentException("IWhiteboardServer service cannot be null");
        }
        this.server = service;
        this.callback = callback;
        this.username = username;
        this.isAdmin = isAdmin;
        System.out.println("ConnectionManager initialized for user: " + username);
    }

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    public MainController getMainController() {
        return this.mainController;
    }

    public void setUsersController(UsersController controller) {
        this.usersController = controller;
    }

    public UsersController getUsersController() {
        return usersController;
    }

    public void setChatController(ChatController controller) {
        this.chatController = controller;
    }

    public ChatController getChatController() {
        return chatController;
    }

    public void setCanvasController(CanvasController controller) {
        this.canvasController = controller;
    }

    public CanvasController getCanvasController() {
        return canvasController;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return !this.connected;
    }

    public String getUsername() {
        return username;
    }

    /**
     * Let RMI calls asynchronously and handle errors
     *
     * @param actionDescription Description for logging purposes
     * @param remoteCall        Lambda expression containing the actual RMI call
     */
    private CompletableFuture<Void> performRemoteCall(String actionDescription, RemoteCallExecutor remoteCall) {
        if (isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Client disconnected."));
        }
        if (server == null) {
            System.err.println("Error: Cannot " + actionDescription + ". Server connection not initialized.");
            // Return a future that has already failed
            return CompletableFuture.failedFuture(new IllegalStateException("Server connection not initialized."));
        }

        // Return a CompletableFuture that runs the remote call asynchronously
        return CompletableFuture.runAsync(() -> {
            try {
                remoteCall.execute();
//                System.out.println("Successfully sent: " + actionDescription);
            } catch (RemoteException ex) {
                System.err.println("RMI Error during [" + actionDescription + "]: " + ex.getMessage());

                // Warping the RemoteException in a RuntimeException
                throw new RuntimeException(ex);
            } catch (Exception ex) {
                System.err.println("Unexpected Error during [" + actionDescription + "]: " + ex.getMessage());
                throw new RuntimeException(ex);
            }
        }, networkExecutor);
    }

    @FunctionalInterface
    private interface RemoteCallExecutor {
        void execute() throws RemoteException;
    }

    public CompletableFuture<Void> acceptUserJoin(String username) {
        return performRemoteCall("accept user join", () -> server.acceptUserJoin(username));
    }

    public CompletableFuture<Void> refuseUserJoin(String username) {
        return performRemoteCall("refuse user join", () -> server.refuseUserJoin(username));
    }

    /**
     * Sends a DrawAction asynchronously.
     *
     * @param action The DrawAction to send.
     */
    public CompletableFuture<Void> drawAction(DrawAction action) {
        return performRemoteCall("draw action", () -> server.broadcastAction(username, action));
    }

    /**
     * Sends an EraseAction asynchronously.
     *
     * @param action The EraseAction to send.
     */
    public CompletableFuture<Void> eraseAction(EraseAction action) {
        return performRemoteCall("erase action", () -> server.broadcastAction(username, action));
    }

    /**
     * Sends a TextAction asynchronously.
     *
     * @param action The TextAction to send.
     */
    public CompletableFuture<Void> textAction(TextAction action) {
        System.out.println("Queueing text action for async send: " + action);
        return performRemoteCall("text action", () -> server.broadcastAction(username, action));
    }

    /**
     * Sends a chat message asynchronously.
     *
     * @param username The username sending the message.
     * @param message  The message content.
     */
    public CompletableFuture<Void> sendChatMessage(String username, String message) {
        return performRemoteCall("send chat message", () -> server.broadcastMessage(username, message));
    }

    /**
     * Sends a request to kick a user asynchronously.
     *
     * @param targetUsername The username to kick.
     */
    public CompletableFuture<Void> kickUser(String targetUsername) {
        if (this.username == null) {
            System.err.println("Error: Cannot kick user. Client username not initialized.");
            return CompletableFuture.failedFuture(new IllegalStateException("Client username not initialized."));
        }
        String currentUser = this.username;
        return performRemoteCall("kick user " + targetUsername, () -> server.kickUser(currentUser, targetUsername, "You are be kicked by Admin"));
    }

    public CompletableFuture<Void> closeCanvas() {
        return performRemoteCall("close canvas", () -> server.clearCanva(true));
    }

    public CompletableFuture<Void> newCanvas() {
        return performRemoteCall("new canvas", () -> server.clearCanva(false));
    }

    public CompletableFuture<Void> openCanvas(String canvasData) {
        return performRemoteCall("open canvas", () -> server.importCanvas(canvasData));
    }

    public CompletableFuture<String> saveCanvas() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return server.exportCanvas();
            } catch (RemoteException ex) {
                System.err.println("RMI Error during [save canvas]: " + ex.getMessage());
                throw new RuntimeException(ex);
            }
        }, networkExecutor);
    }

    /**
     * Shuts down the network executor service. Call this when the application exits.
     */
    public void shutdown() {
        System.out.println("Shutting down ConnectionManager network executor...");

        // Disable new tasks from being submitted
        networkExecutor.shutdown();
        try {
            server.unregisterClient(username);
            // Wait a while for existing tasks to terminate
            if (!networkExecutor.awaitTermination(5, TimeUnit.SECONDS)) {

                // Cancel currently executing tasks
                networkExecutor.shutdownNow();

                // Wait a while for tasks to respond to being canceled
                if (!networkExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Error: ConnectionManager network executor did not terminate cleanly.");
                }
            }
        } catch (InterruptedException ie) {
            // Cancel if current thread also interrupted
            networkExecutor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        } catch (RemoteException e) {
            System.err.println("Error: Failed to unregister client: " + e.getMessage());
        }
        System.out.println("ConnectionManager network executor shut down.");
    }
}