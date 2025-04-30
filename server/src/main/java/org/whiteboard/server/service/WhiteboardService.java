package org.whiteboard.server.service;

import org.whiteboard.common.action.Action;
import org.whiteboard.common.rmi.IClientCallback;
import org.whiteboard.common.rmi.IWhiteboardService;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WhiteboardService extends UnicastRemoteObject implements IWhiteboardService {
    private final Map<String, IClientCallback> clients = new ConcurrentHashMap<>();

    /**
     * Create a new service instance.
     *
     * @param port the port number of the server
     */
    public static void CreateService(int port) {
        try {
            Registry registry = LocateRegistry.createRegistry(port);

            WhiteboardService service = new WhiteboardService();

            registry.rebind("WhiteboardService", service);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    protected WhiteboardService() throws RemoteException {
        super();
    }

    @Override
    public synchronized void registerClient(String name, IClientCallback callback) throws RemoteException {
        // Check if the client is already registered
        if (clients.containsKey(name)) {
            System.out.println("Client " + name + " is already registered, unregistering...");
            unregisterClient(name);
            System.out.println("Client " + name + " unregistered");
        }
        clients.put(name, callback);
        System.out.println("Registered client: " + name);
        broadcastMessage(name, "has joined the whiteboard");
    }

    @Override
    public synchronized void unregisterClient(String name) throws RemoteException {
        clients.remove(name);
        broadcastMessage(name, "has left the whiteboard");
    }

    @Override
    public synchronized void broadcastAction(Action action) throws RemoteException {
        System.out.println("Broadcasting action from" + action.getUsername() + " to all clients");
        for (Map.Entry<String, IClientCallback> entry : clients.entrySet()) {
            String clientName = entry.getKey();
            IClientCallback client = entry.getValue();

            if (!clientName.equals(action.getUsername())) {
                System.out.println("Broadcasting action to client: " + clientName);
                client.onAction(action);
            }
        }
    }

    @Override
    public synchronized void broadcastMessage(String username, String message) throws RemoteException {
        // TODO Broadcast the message to all clients
        for (Map.Entry<String, IClientCallback> entry : clients.entrySet()) {
            String clientName = entry.getKey();
            IClientCallback client = entry.getValue();
            if (!clientName.equals(username)) {
                client.onSendMessage(username, message);
            }
        }
    }

    @Override
    public synchronized void kickUser(String username) throws RemoteException {
        // TODO Only admin can kick users
        IClientCallback targetClient = clients.get(username);
        if (targetClient != null) {

            // call disconnect on the target client
            // disconnect will call unregisterClient
            targetClient.onKicked();

            // Check if the client has been unregistered
            if (clients.containsKey(username)) {
                System.out.println("Kicked user: " + username + " Failed");
            }

            System.out.println("Kicked user: " + username + " Success");
        } else {
            System.out.println("User " + username + " not found");
        }
    }
}
