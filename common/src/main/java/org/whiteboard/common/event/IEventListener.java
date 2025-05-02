package org.whiteboard.common.event;

import org.whiteboard.common.rmi.IClientCallback;

import java.util.Map;

/**
 * Interface for listeners interested in Action events.
 */
@FunctionalInterface
public interface IEventListener<E extends IEvent> {
    /**
     * Handles the received event.
     *
     * @param event   The event to handle.
     * @param clients The map of currently connected clients to broadcast to.
     */
    void onEventReceived(E event, Map<String, IClientCallback> clients);
}