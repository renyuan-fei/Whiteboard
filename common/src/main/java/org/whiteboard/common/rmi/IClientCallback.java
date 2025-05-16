package org.whiteboard.common.rmi;

import org.whiteboard.common.action.Action;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Client-side callback interface.
 */
public interface IClientCallback extends Remote {
    /**
     * Invoked when the server is shutting down.
     *
     * @throws RemoteException on network error
     */
    IWhiteboardServer getWhiteboardServer() throws RemoteException;

    /**
     * Invoked when a new DrawAction arrives.
     *
     * @param action the action to render
     * @throws RemoteException on network error
     */
    void onAction(Action action) throws RemoteException;

    /**
     * Invoked when a chat message arrives.
     *
     * @param username sender name
     * @param message  chat text
     * @throws RemoteException on network error
     */
    void onSendMessage(String username, String message) throws RemoteException;

    /**
     * @param username new attend username
     * @throws RemoteException on network error
     */
    void onInitialUserList(List<String> username) throws RemoteException;

    /**
     * @param username new attend username
     * @throws RemoteException on network error
     */
    void onAddUser(String username) throws RemoteException;

    /**
     * @param username leave username
     * @throws RemoteException on network error
     */
    void onRemoveUser(String username) throws RemoteException;

    /**
     * Invoked when this client is kicked.
     *
     * @throws RemoteException on network error
     */
    void onKicked(String message) throws RemoteException;

    /**
     * Invoked when the server is shutting down.
     *
     * @param reason shutdown reason
     * @throws RemoteException on network error
     */
    void onServerShutdown(String reason) throws RemoteException;

    /**
     * sync current white board client
     *
     * @param canvasData Serialized actions
     * @throws RemoteException on network error
     */
    void onSyncWhiteboard(String canvasData) throws RemoteException;
}