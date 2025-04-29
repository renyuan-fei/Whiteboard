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
     * @param port the port number of the server
     * @return a new service instance
     */
    public static IWhiteboardService CreateService(int port) {
        try {
            Registry registry = LocateRegistry.createRegistry(port);

            WhiteboardService service = new WhiteboardService();

            registry.rebind("WhiteboardService", service);

            return service;

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
        // TODO According to the action type, call the corresponding method for each client
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
    public synchronized void kickUser(String targetUsername) throws RemoteException {
        // TODO can only be called by the admin
    }
}
