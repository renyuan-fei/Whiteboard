package org.whiteboard.common.rmi;

import org.whiteboard.common.action.Action;
import org.whiteboard.common.action.DrawAction;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote interface for Whiteboard services.
 */
public interface IWhiteboardService extends Remote {
    /**
     * Join the whiteboard session.
     * @param username unique user name
     * @param callback client callback stub
     * @throws RemoteException on network error
     */
    void registerClient(String username, IClientCallback callback) throws RemoteException;

    /**
     * Leave the whiteboard session.
     * @param username unique user name
     * @throws RemoteException on network error
     */
    void unregisterClient(String username) throws RemoteException;

    /**
     * Broadcast a drawing action to all peers.
     * @param action the drawing action
     * @throws RemoteException on network error
     */
    void broadcastAction(Action action) throws RemoteException;

    /**
     * Send chat message.
     * @param username sender name
     * @param message chat text
     * @throws RemoteException on network error
     */
    void broadcastMessage(String username, String message)
            throws RemoteException;

    /**
     * Kick a peer out of the session.
     * @param username username to be kicked
     * @throws RemoteException on network error
     */
    void kickUser(String username) throws RemoteException;
}
