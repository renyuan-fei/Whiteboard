package org.whiteboard.common.action;

import org.whiteboard.common.event.IEvent;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all whiteboard actions.
 */
public abstract class Action implements Serializable, IEvent {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String actionId;
    private final String username;
    private final Instant timestamp;

    /**
     * @param username  the user who generated this action
     */
    public Action(String username) {
        this.actionId = UUID.randomUUID().toString();
        this.username = username;
        this.timestamp = Instant.now();
    }

    public String getActionId() {
        return actionId;
    }

    public String getUsername() {
        return username;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format(
                "Action[id=%s, user=%s, time=%s]",
                actionId,
                username,
                timestamp
        );
    }
}
