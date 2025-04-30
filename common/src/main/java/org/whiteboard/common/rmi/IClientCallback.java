package org.whiteboard.common.rmi;

import org.whiteboard.common.action.Action;
import org.whiteboard.common.action.DrawAction;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Client-side callback interface.
 */
public interface IClientCallback extends Remote {
    /**
     * Invoked when the server is shutting down.
     * @throws RemoteException on network error
     */
    IWhiteboardService getService() throws RemoteException;

    /**
     * Invoked when a new DrawAction arrives.
     * @param action the action to render
     * @throws RemoteException on network error
     */
    void onAction(Action action) throws RemoteException;

    /**
     * Invoked when a chat message arrives.
     * @param username sender name
     * @param message chat text
     * @throws RemoteException on network error
     */
    void onSendMessage(String username, String message) throws RemoteException;

    /**
     * Invoked when this client is kicked.
     * @throws RemoteException on network error
     */
    void onKicked() throws RemoteException;
}