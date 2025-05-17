package org.whiteboard.server.service;

import org.whiteboard.common.rmi.IClientCallback;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserService extends Service {

    private String Admin = "";

    private final Map<String, IClientCallback> waitingClients;

    public UserService(Map<String, IClientCallback> clients) {
        super();
        waitingClients = new ConcurrentHashMap<>();
        setClients(clients);
    }

    public synchronized boolean hasAdmin() {
        return !Admin.isEmpty();
    }

    public synchronized void setAdmin(String username) {
        this.Admin = username;
    }

    public synchronized String getAdmin() {
        return Admin;
    }

    public void waitingForJoin(String username, IClientCallback callback) throws RemoteException {
        synchronized (this) {
            // add user to a waiting list
            waitingClients.put(username, callback);

            // ask admin within the synchronized block
            IClientCallback admin = getClients().get(Admin);
            if (admin != null) {
                admin.onAskUserJoin(username);
            } else {
                System.err.println("Error: Admin client not found when a user is waiting to join.");
            }
        }
    }

    public void userRefuse(String username) throws RemoteException {
        // init after admin refuse
        IClientCallback userCallback = waitingClients.get(username);
        synchronized (this) {
            this.waitingClients.remove(username);
        }
        if (userCallback != null) {
            userCallback.onKicked("Admin refuse your request");
        } else {
            System.err.println("Error: User callback not found for username: " + username + " when trying to join.");
        }
    }

    public IClientCallback userJoin(String username) throws RemoteException {
        // init after admin approve
        IClientCallback userCallback = waitingClients.get(username);
        synchronized (this) {
            this.waitingClients.remove(username);
        }
        if (userCallback != null) {
            registerClient(username, userCallback, false);
        } else {
            System.err.println("Error: User callback not found for username: " + username + " when trying to join.");
        }

        return userCallback;
    }

    public synchronized void registerClient(String username, IClientCallback callback, boolean isAdmin) throws RemoteException {
        // Check if the client is already registered
        if (getClients().containsKey(username)) {
            System.out.println("Client " + username + " is already registered, unregistering...");
            throw new RemoteException("Client " + username + " is already registered");
//            unregisterClient(username);
//            System.out.println("Client " + username + " unregistered");
        }

        callback.onInitialClientState(getUsers(), isAdmin);

        getClients().put(username, callback);
        System.out.println("Registered client: " + username);

        broadcastMessage(username, "has joined the whiteboard");

        for (IClientCallback client : getClients().values()) {
            client.onAddUser(username);
        }
    }

    public synchronized void unregisterClient(String username) throws RemoteException {
        assertRegistered(username);

        for (IClientCallback client : getClients().values()) {
            client.onRemoveUser(username);
        }
        broadcastMessage(username, "has left the whiteboard");

        getClients().remove(username);
        System.out.println("Unregistered client: " + username);
    }

    public synchronized ArrayList<String> getUsers() {
        return new ArrayList<>(getClients().keySet());
    }

    public synchronized void broadcastMessage(String username, String message) throws RemoteException {
        assertRegistered(username);
        for (Map.Entry<String, IClientCallback> entry : getClients().entrySet()) {
            String clientName = entry.getKey();
            IClientCallback client = entry.getValue();
            if (!clientName.equals(username)) {
                client.onSendMessage(username, message);
            }
        }
    }

    public void kickUser(String username, String message) throws RemoteException {
        // Grab the stub while holding the lock only briefly
        IClientCallback targetClient;
        synchronized (this) {
            targetClient = getClients().get(username);
        }

        if (targetClient == null) {
            System.out.println("User " + username + " not found");
            return;
        }

        // Perform the remote callback outside the synchronized block to avoid dead‑lock
        targetClient.onKicked(message);

        // Check whether the user was removed after the callback
        synchronized (this) {
            if (getClients().containsKey(username)) {
                System.out.println("Kicked user: " + username + " Failed");
            } else {
                System.out.println("Kicked user: " + username + " Success");
            }
        }
    }

    public void shutdown() {
        System.out.println("Shutting down FileService...");
        System.out.println("FileService shut down.");
    }
}