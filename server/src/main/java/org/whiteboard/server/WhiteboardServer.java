package org.whiteboard.server;

import org.whiteboard.common.action.Action;
import org.whiteboard.common.rmi.IClientCallback;
import org.whiteboard.common.rmi.IWhiteboardServer;
import org.whiteboard.server.service.FileService;
import org.whiteboard.server.service.UserService;
import org.whiteboard.server.service.WhiteboardService;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WhiteboardServer extends UnicastRemoteObject implements IWhiteboardServer {

    private final WhiteboardService whiteboardService;
    private final FileService fileService;
    private final UserService userService;
    private transient Registry registry;


    /**
     * Factory method to create and register a new server instance.
     *
     * @param port the port number of the server
     * @param whiteboardService the whiteboard service
     * @param fileService       the file service
     * @param userService       the user service
     * @throws RuntimeException if server creation fails.
     */
    public static WhiteboardServer CreateServer(
            int port,
            WhiteboardService whiteboardService,
            FileService fileService,
            UserService userService
    ) {
        try {
            Registry registry = LocateRegistry.createRegistry(port);

            WhiteboardServer server = new WhiteboardServer(
                    registry,
                    whiteboardService,
                    fileService,
                    userService
            );

            registry.rebind("WhiteboardServer", server);

            System.out.println("WhiteboardServer bound to registry on port " + port);
            return server;

        } catch (RemoteException ex) {
            System.err.println("FATAL: Failed to create or bind server: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    /**
     * Constructor for the server.
     *
     * @param registry          RMI registry instance
     * @param whiteboardService the whiteboard service
     * @param fileService       the file service
     * @param userService       the user service
     * @throws RemoteException if an error occurs during remote object creation
     */
    protected WhiteboardServer(
            Registry registry,
            WhiteboardService whiteboardService,
            FileService fileService,
            UserService userService
    ) throws RemoteException {
        this.registry = registry;
        this.fileService = fileService;
        this.whiteboardService = whiteboardService;
        this.userService = userService;
    }


    /**
     * Constructor for the server.
     *
     * @param whiteboardService the whiteboard service
     * @param fileService       the file service
     * @param userService       the user service
     * @throws RemoteException if an error occurs during remote object creation
     */
    protected WhiteboardServer(
            WhiteboardService whiteboardService,
            FileService fileService,
            UserService userService
    ) throws RemoteException {
        super();

        this.fileService = fileService;
        this.whiteboardService = whiteboardService;
        this.userService = userService;
    }

    @Override
    public void registerClient(boolean isAdmin, String username, IClientCallback callback) throws RemoteException {
        // User can only register as an admin if no admin is registered, yet
        // Admin can only be registered if no other admin is registered
        if (isAdmin && !userService.hasAdmin()) {
            userService.setAdmin(username);
            System.out.println("Admin '" + username + "' has create a new whiteboard");
            // TODO create canvas storage
        } else if (isAdmin && userService.hasAdmin() && !Objects.equals(userService.getAdmin(), username)) {
            throw new RemoteException("Only one admin can be registered at a time");
        } else if (!isAdmin && !userService.hasAdmin()) {
            throw new RemoteException("No admin registered yet, please register as admin first");
        }

        userService.registerClient(username, callback);

        // TODO file service get current canvas storage

        // TODO whiteboard service import canvas storage

    }

    @Override
    public void unregisterClient(String username) throws RemoteException {
        if (userService.hasAdmin() && userService.getAdmin().equals(username)) {
            userService.setAdmin("");
            System.out.println("Admin '" + username + "' has left whiteboard");
            // TODO clean canvas storage

            // TODO kick all user
        }
        userService.unregisterClient(username);
    }

    @Override
    public void broadcastAction(String username, Action action) throws RemoteException {
        // TODO file service store action in canvas storage

        whiteboardService.broadcastAction(username, action);
    }

    @Override
    public void broadcastMessage(String username, String message) throws RemoteException {
        userService.broadcastMessage(username, message);
    }

    @Override
    public void kickUser(String senderName, String targetUsername) throws RemoteException {
        String admin = userService.getAdmin();
        if (userService.hasAdmin() && Objects.equals(senderName, admin)) {
            if (Objects.equals(targetUsername, admin)) {
                throw new RemoteException("Admin cannot kick themselves.");
            }
            System.out.println("Admin '" + senderName + "' attempting to kick user '" + targetUsername + "'");
            broadcastMessage(admin, "Admin '" + senderName + "' attempting to kick user '" + targetUsername + "'");
            userService.kickUser(targetUsername); // Delegate kick logic to UserService
        } else {
            throw new RemoteException("User '" + senderName + "' does not have permission to kick users (not admin).");
        }

    }

    @Override
    public ArrayList<String> getUsers() throws RemoteException {
        return userService.getUsers();
    }


    // TODO : implement importCanvas

    // TODO : implement exportCanvas

    // TODO : implement newCanvas

    // TODO : implement saveCanvas


    /**
     * Notifies all connected clients that the server is shutting down.
     */
    private void notifyClientsOfShutdown() {
        System.out.println("Notifying clients of server shutdown...");

        // Create a copy of the client list to avoid ConcurrentModificationException
        // if a client disconnects during notification.
        List<Map.Entry<String, IClientCallback>> clientsToNotify = new ArrayList<>(userService.getClients().entrySet());

        // Notify each client
        for (Map.Entry<String, IClientCallback> entry : clientsToNotify) {
            String username = entry.getKey();
            IClientCallback client = entry.getValue();
            try {
                client.onServerShutdown("Server is shutting down gracefully.");
                System.out.println("Notified client: " + username);
            } catch (RemoteException ex) {
                // Handle the case where the client is no longer reachable
                System.err.println("Error: Could not notify client " + username + " of shutdown: " + ex.getMessage());
            }
        }
        System.out.println("Finished notifying clients.");
    }

    /**
     * Gracefully shuts down the server, services, and RMI components.
     */
    public void shutdown() {
        System.out.println("Initiating server shutdown sequence...");

        // Unbind from the registry to stop accepting new connections
        if (this.registry != null) {
            try {
                registry.unbind("WhiteboardServer");
                System.out.println("Server unbound from RMI registry");
            } catch (Exception e) {
                System.err.println("Error unbinding server from registry: " + e.getMessage());
            }
        } else {
            System.err.println("Warning: Registry reference is null, cannot unbind");
        }


        // Notify connected clients
        notifyClientsOfShutdown();

        // Shutdown internal services
        if (whiteboardService != null) {
            whiteboardService.shutdown();
        }
        if (fileService != null) {
            fileService.shutdown();
        }
        if (userService != null) {
            userService.shutdown();
        }

        // Unexport the main server RMI object
        try {
            UnicastRemoteObject.unexportObject(this, true); // true = force unexport
            System.out.println("WhiteboardServer RMI object unexported successfully");
        } catch (NoSuchObjectException e) {
            System.err.println("Error: unexporting server RMI object (already unexported?): " + e.getMessage());
        }

        System.out.println("Server shutdown sequence complete");
    }

}
