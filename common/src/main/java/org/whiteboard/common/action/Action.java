package org.whiteboard.common.action;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Base class for all whiteboard actions.
 */
public abstract class Action implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final long actionId;
    private final String username;
    private final Instant timestamp;

    /**
     * @param actionId  unique identifier for this action
     * @param username  the user who generated this action
     * @param timestamp time when the action was created
     */
    public Action(long actionId, String username, Instant timestamp) {
        this.actionId = actionId;
        this.username = username;
        this.timestamp = timestamp;
    }

    public long getActionId() {
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
                "Action[id=%d, user=%s, time=%s]",
                actionId,
                username,
                timestamp
        );
    }
}
