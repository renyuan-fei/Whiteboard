package org.whiteboard.server.event;

import org.whiteboard.common.action.Action;
import org.whiteboard.common.event.IEventListener;
import org.whiteboard.common.rmi.IClientCallback;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listens for Action events and broadcasts them asynchronously to clients.
 */
public class AsyncActionBroadcaster implements IEventListener<Action> {

    // Use a cached thread pool for potentially numerous short-lived broadcast tasks
    private final ExecutorService broadcastExecutor = Executors.newCachedThreadPool();

    @Override
    public void onEventReceived(Action action, Map<String, IClientCallback> clients) {
        System.out.println("Async Broadcaster received action from " + action.getUsername() + ". Broadcasting...");

        // Iterate over a snapshot of the client entries to avoid ConcurrentModificationException
        // if the map is modified elsewhere (though ConcurrentHashMap handles gets safely)
        for (Map.Entry<String, IClientCallback> entry : clients.entrySet()) {
            String clientName = entry.getKey();
            IClientCallback clientCallback = entry.getValue();

            if (!clientName.equals(action.getUsername())) {
                // Submit the RMI call to the broadcast executor pool
                broadcastExecutor.submit(() -> {
                    try {
                        System.out.println("Sending action via RMI to client: " + clientName);
                        clientCallback.onAction(action);
                    } catch (RemoteException e) {
                        System.err.println("Failed to send action to client " + clientName + ": " + e.getMessage());
                        // Optional: Implement logic to remove unresponsive clients
                        // clients.remove(clientName); // Be careful with removal logic
                    } catch (Exception e) {
                        System.err.println("Unexpected error broadcasting to " + clientName + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }

    }

    /**
     * Shuts down the broadcast executor service. Call this on server shutdown.
     */
    public void shutdown() {
        broadcastExecutor.shutdown();
    }
}