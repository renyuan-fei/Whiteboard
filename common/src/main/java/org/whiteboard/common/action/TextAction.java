package org.whiteboard.common.action;

import org.whiteboard.common.Point;

import java.io.Serial;
import java.time.Instant;

public class TextAction extends Action {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String text;

    private final Point position;

    private final Double scale;

    /**
     * @param actionId  unique identifier for this action
     * @param username  the user who generated this action
     * @param timestamp time when the action was created
     * @param text      text to be drawn
     * @param position  position of the text on the whiteboard
     * @param scale     scale of the text
     */
    public TextAction(long actionId, String username, Instant timestamp, String text, Point position, Double scale) {
        super(actionId, username, timestamp);
        this.text = text;
        this.position = position;
        this.scale = scale;
    }

    public String getText() {
        return text;
    }

    public Point getPosition() {
        return position;
    }

    public Double getScale() {
        return scale;
    }
}
