package org.whiteboard.server.service;

import org.whiteboard.common.action.Action;
import org.whiteboard.common.rmi.IClientCallback;
import org.whiteboard.server.event.AsyncActionBroadcaster;
import org.whiteboard.server.event.EventBus;

import java.rmi.RemoteException;
import java.util.Map;

public class WhiteboardService extends Service {

    final EventBus<Action> eventBus;
    final AsyncActionBroadcaster actionBroadcaster;


    public WhiteboardService(Map<String, IClientCallback> clients) {
        super();
        setClients(clients);
        eventBus = new EventBus<>(clients);
        actionBroadcaster = new AsyncActionBroadcaster();
        eventBus.register(actionBroadcaster);
    }

    /**
     * Broadcast an action to all clients except the one who sent it.
     *
     * @param action (drawing action, Erase action, Text action)
     */
    public void broadcastAction(String username, Action action) throws RemoteException {
        assertRegistered(username);
        eventBus.publish(action);
    }

    /**
     * Import canvas data to a user.
     *
     * @param username   unique user name
     * @param canvasData newest canvas data
     * @throws RemoteException error
     */
    public synchronized void importCanvas(String username, String canvasData) throws RemoteException {
        assertRegistered(username);
    }

    /**
     * Shutdown the event bus and the action broadcaster.
     */
    public void shutdown() {
        eventBus.shutdown();
        actionBroadcaster.shutdown();
    }
}
