package org.whiteboard.common.action;

import org.whiteboard.common.Point;

import java.io.Serial;
import java.time.Instant;
import java.util.List;

/**
 * Represents an erase operation on the whiteboard.
 */
public final class EraseAction extends Action {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<Point> erasePath;
    private final double eraserSize;

    /**
     * @param actionId    unique action id
     * @param username    creator username
     * @param timestamp   creation time
     * @param erasePath   list of points that were erased
     * @param eraserSize  diameter of the eraser in pixels
     */
    public EraseAction(
            long actionId,
            String username,
            Instant timestamp,
            List<Point> erasePath,
            double eraserSize
    ) {
        super(actionId, username, timestamp);
        this.erasePath = List.copyOf(erasePath);
        this.eraserSize = eraserSize;
    }

    public List<Point> getErasePath() {
        return erasePath;
    }

    public double getEraserSize() {
        return eraserSize;
    }

    @Override
    public String toString() {
        return String.format(
                "EraseAction[pathSize=%d, eraserSize=%.1f]",
                erasePath.size(),
                eraserSize
        );
    }
}