package org.whiteboard.common.rmi;

import org.whiteboard.common.action.Action;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Remote interface for Whiteboard services.
 */
public interface IWhiteboardServer extends Remote {
    /**
     * Broadcast a drawing action to all peers.
     *
     * @param action the drawing action
     * @throws RemoteException on network error
     */
    void broadcastAction(String username, Action action) throws RemoteException;

    /**
     * Join the whiteboard session.
     *
     * @param username unique user name
     * @param callback client callback stub
     * @throws RemoteException on network error
     */
    void registerClient(boolean isAdmin, String username, IClientCallback callback) throws RemoteException;

    /**
     * Leave the whiteboard session.
     *
     * @param username unique user name
     * @throws RemoteException on network error
     */
    void unregisterClient(String username) throws RemoteException;

    /**
     * Send a chat message.
     *
     * @param username sender name
     * @param message  chat text
     * @throws RemoteException on network error
     */
    void broadcastMessage(String username, String message)
            throws RemoteException;

    /**
     * Kick a peer out of the session.
     *
     * @param senderName admin name
     * @param targetUsername username to be kicked
     * @throws RemoteException on network error
     */
    void kickUser(String senderName, String targetUsername) throws RemoteException;

    /**
     * get all usernames from clients
     *
     * @return list of usernames
     * @throws RemoteException on network error
     */
    ArrayList<String> getUsers() throws RemoteException;
}
