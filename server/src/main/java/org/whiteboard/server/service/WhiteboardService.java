package org.whiteboard.server.service;

import org.whiteboard.common.action.Action;
import org.whiteboard.common.rmi.IClientCallback;

import java.rmi.RemoteException;
import java.util.Map;

public class WhiteboardService extends Service {

    public WhiteboardService() {
        super();
    }

    /**
     * Broadcast an action to all clients except the one who sent it.
     *
     * @param action (drawing action, Erase action, Text action)
     * @throws RemoteException error
     */
    public synchronized void broadcastAction(Action action) throws RemoteException {
        System.out.println("Broadcasting action from" + action.getUsername() + " to all clients");
        for (Map.Entry<String, IClientCallback> entry : getClients().entrySet()) {
            String clientName = entry.getKey();
            IClientCallback client = entry.getValue();

            if (!clientName.equals(action.getUsername())) {
                System.out.println("Broadcasting action to client: " + clientName);
                client.onAction(action);
            }
        }
    }

    /**
     * Import canvas data to a user.
     *
     * @param username   unique user name
     * @param canvasData newest canvas data
     * @throws RemoteException error
     */
    public synchronized void importCanvas(String username, String canvasData) throws RemoteException {
    }
}
