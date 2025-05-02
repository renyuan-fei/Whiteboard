package org.whiteboard.server.service;

import org.whiteboard.common.rmi.IClientCallback;

import java.rmi.RemoteException;
import java.util.Map;

public class UserService extends Service {

    private String Admin = "";

    public UserService(Map<String, IClientCallback> clients) {
        super();
        setClients(clients);
    }

    public synchronized boolean hasAdmin() {
        return !Admin.isEmpty();
    }

    public synchronized void setAdmin(String name) {
        this.Admin = name;
    }

    public synchronized String getAdmin() {
        return Admin;
    }

    public synchronized void registerClient(String name, IClientCallback callback) throws RemoteException {
        // Check if the client is already registered
        if (getClients().containsKey(name)) {
            System.out.println("Client " + name + " is already registered, unregistering...");
            unregisterClient(name);
            System.out.println("Client " + name + " unregistered");
        }
        getClients().put(name, callback);
        System.out.println("Registered client: " + name);
        broadcastMessage(name, "has joined the whiteboard");
    }

    public synchronized void unregisterClient(String name) throws RemoteException {
        getClients().remove(name);
        broadcastMessage(name, "has left the whiteboard");
    }

    public synchronized void broadcastMessage(String username, String message) throws RemoteException {
        for (Map.Entry<String, IClientCallback> entry : getClients().entrySet()) {
            String clientName = entry.getKey();
            IClientCallback client = entry.getValue();
            if (!clientName.equals(username)) {
                client.onSendMessage(username, message);
            }
        }
    }

    public synchronized void kickUser(String username) throws RemoteException {
        IClientCallback targetClient = getClients().get(username);
        if (targetClient != null) {

            // call disconnect on the target client
            // disconnect will call unregisterClient
            targetClient.onKicked();

            // Check if the client has been unregistered
            if (getClients().containsKey(username)) {
                System.out.println("Kicked user: " + username + " Failed");
            }

            System.out.println("Kicked user: " + username + " Success");
        } else {
            System.out.println("User " + username + " not found");
        }
    }

    public void shutdown() {
        System.out.println("Shutting down FileService...");
        System.out.println("FileService shut down.");
    }
}
