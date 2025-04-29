package org.whiteboard.client;

import org.whiteboard.common.action.Action;
import org.whiteboard.common.action.DrawAction;
import org.whiteboard.common.rmi.IClientCallback;
import org.whiteboard.common.rmi.IWhiteboardService;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class WhiteboardClient extends UnicastRemoteObject implements IClientCallback {

    private static final String SERVICE_NAME  = "WhiteboardService";
    private final IWhiteboardService service;
    private final String username;

    /**
     * Create a new client instance.
     * @param host the host name of the server
     * @param port the port number of the server
     * @param username the username
     * @return a new client instance
     * @throws RemoteException on network error
     */
    public static WhiteboardClient createClient(String host, int port, String username) throws RemoteException {
        return new WhiteboardClient(host, port, username);
    }

    protected WhiteboardClient(String host, int port ,String username) throws RemoteException {
        super();

        this.username = username;

        try {
            service = connectWithRetry(5, 2000, host, port, SERVICE_NAME, username);
            if (service != null) {
                service.registerClient(username, this);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Getter for the remote service stub
     */
    public IWhiteboardService getService() {
        return service;
    }

    /**
     * Disconnect from the server and unregister the client.
     */
    public void disconnect() {
        try {
            service.unregisterClient(username);
        } catch (RemoteException ignored) {}
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (Exception ignored) {}
    }


    @Override
    public void onAction(Action action) throws RemoteException {

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
     * @param maxRetries maximum number of retries
     * @param retryDelayMs initial delay between retries in milliseconds
     * @param host the host name of the server
     * @param port the port number of the server
     * @param serviceName the name of the service
     * @param username the username
     * @return a reference to the IWhiteboardService or null if it fails
     */
    private static IWhiteboardService connectWithRetry(int maxRetries, long retryDelayMs, String host, int port, String serviceName, String username) {
        int attempt = 1;
        while (attempt <= maxRetries) {
            try {
                // Try to get the registry and lookup the service
                Registry registry = LocateRegistry.getRegistry(host, port);

                return (IWhiteboardService) registry.lookup(serviceName);

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

                attempt++;
            }
        }
        return null;
    }
}
