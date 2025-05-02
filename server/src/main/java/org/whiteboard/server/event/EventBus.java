package org.whiteboard.server.event;

import org.whiteboard.common.event.IEvent;
import org.whiteboard.common.event.IEventListener;
import org.whiteboard.common.rmi.IClientCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple event bus to decouple event producers and consumers.
 * Handles event dispatching asynchronously.
 */
public class EventBus<E extends IEvent> {

    private final List<IEventListener<E>> eventListeners = new ArrayList<>();

    // Executor for dispatching events to listeners orderly
    private final ExecutorService dispatchExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, IClientCallback> clients;

    public EventBus(Map<String, IClientCallback> clients) {
        this.clients = clients;
    }

    /**
     * Registers a listener for Action events.
     *
     * @param listener The listener to register.
     */
    public void register(IEventListener<E> listener) {
        eventListeners.add(listener);
    }

    /**
     * Publishes an Action event to all registered listeners asynchronously.
     *
     * @param event The event to publish.
     */
    public void publish(E event) {
        dispatchExecutor.submit(() -> {
            for (IEventListener<E> listener : eventListeners) {
                try {
                    // Pass the current state of the clients map to the listener
                    listener.onEventReceived(event, clients);
                } catch (Exception e) {
                    System.err.println("Error: dispatching action event to listener: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Shuts down the dispatcher executor service. Call this on server shutdown.
     */
    public void shutdown() {
        dispatchExecutor.shutdown();
    }
}