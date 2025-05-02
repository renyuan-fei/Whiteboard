package org.whiteboard.server;

import org.whiteboard.common.action.Action;
import org.whiteboard.common.rmi.IClientCallback;
import org.whiteboard.common.rmi.IWhiteboardServer;
import org.whiteboard.server.service.FileService;
import org.whiteboard.server.service.UserService;
import org.whiteboard.server.service.WhiteboardService;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class WhiteboardServer extends UnicastRemoteObject implements IWhiteboardServer {

    private final WhiteboardService whiteboardService;
    private final FileService fileService;
    private final UserService userService;

    private final Map<String, IClientCallback> clients = new ConcurrentHashMap<>();

    /**
     * Create a new server instance.
     *
     * @param port the port number of the server
     */
    public static void CreateServer(
            int port,
            WhiteboardService whiteboardService,
            FileService fileService,
            UserService userService
    ) {
        try {
            Registry registry = LocateRegistry.createRegistry(port);

            WhiteboardServer server = new WhiteboardServer(
                    whiteboardService,
                    fileService,
                    userService
            );

            registry.rebind("WhiteboardServer", server);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    protected WhiteboardServer(
            WhiteboardService whiteboardService,
            FileService fileService,
            UserService userService
    ) throws RemoteException {
        super();

        // Inject clients into services
        fileService.setClients(clients);
        whiteboardService.setClients(clients);
        userService.setClients(clients);

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
        if (userService.hasAdmin() && Objects.equals(userService.getAdmin(), username)) {
            userService.setAdmin("");
            // TODO clean canvas storage
        }
        userService.unregisterClient(username);
    }

    @Override
    public void broadcastAction(Action action) throws RemoteException {
        // TODO file service store action in canvas storage

        whiteboardService.broadcastAction(action);
    }

    @Override
    public void broadcastMessage(String username, String message) throws RemoteException {
        userService.broadcastMessage(username, message);
    }

    @Override
    public void kickUser(String senderName, String username) throws RemoteException {
        if (!Objects.equals(senderName, userService.getAdmin())) {
            userService.kickUser(username);
        } else if (Objects.equals(username, userService.getAdmin())) {
            throw new RemoteException("You cannot kick yourself");
        } else {
            throw new RemoteException("You are not the admin");
        }
    }

    // TODO : implement importCanvas

    // TODO : implement exportCanvas

    // TODO : implement newCanvas

    // TODO : implement saveCanvas
}
