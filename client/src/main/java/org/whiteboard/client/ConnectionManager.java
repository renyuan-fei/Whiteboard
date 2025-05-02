package org.whiteboard.client;

import org.whiteboard.client.controller.CanvasController;
import org.whiteboard.common.action.DrawAction;
import org.whiteboard.common.action.EraseAction;
import org.whiteboard.common.action.TextAction;
import org.whiteboard.common.rmi.IClientCallback;
import org.whiteboard.common.rmi.IWhiteboardServer;

import java.rmi.RemoteException;

public class ConnectionManager {
    // Eager singleton
    private static final ConnectionManager INSTANCE = new ConnectionManager();

    private String username;
    private IWhiteboardServer server;
    private IClientCallback callback;

    private CanvasController canvasController;

    private ConnectionManager() {
    }

    public static ConnectionManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initialize RMI connection.
     *
     * @param service  remote service
     * @param callback local callback stub
     */
    public void init(IWhiteboardServer service, IClientCallback callback, String username) {
        this.server = service;
        this.callback = callback;
        this.username = username;
    }

    public void setCanvasController(CanvasController controller) {
        this.canvasController = controller;
    }

    public CanvasController getCanvasController() {
        return canvasController;
    }

    public IWhiteboardServer getServer() {
        return server;
    }

    public IClientCallback getCallback() {
        return callback;
    }

    public String getUsername() {
        return username;
    }

    public void drawAction(DrawAction action) throws RemoteException {
        server.broadcastAction(action);
    }

    public void eraseAction(EraseAction action) throws RemoteException {
        server.broadcastAction(action);
    }

    public void textAction(TextAction action) throws RemoteException {
        System.out.println("Text action received in Connection manager: " + action);
        server.broadcastAction(action);
    }

    public void sendChatMessage(String user, String msg) throws RemoteException {
        server.broadcastMessage(user, msg);
    }

    public void kickUser(String targetUsername) throws RemoteException {
        server.kickUser(username, targetUsername);
    }
}