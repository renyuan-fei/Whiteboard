package org.whiteboard.common.action;

import org.whiteboard.common.Point;

import java.io.Serial;
import java.util.List;

/**
 * Represents a drawing operation on the whiteboard.
 */
public final class DrawAction extends Action {
    @Serial
    private static final long serialVersionUID = 1L;


    public enum ShapeType {
        POINT,
        LINE,
        RECTANGLE,
        OVAL,
        TRIANGLE,
        FREEHAND,
        ERASER
    }

    private final ShapeType shapeType;
    private final List<Point> points;
    private final String color;
    private final double strokeWidth;

    /**
     * @param username    creator username
     * @param shapeType   type of shape drawn
     * @param points      list of points defining the shape
     * @param color       stroke color in hex (e.g. "#FF0000")
     * @param strokeWidth width of the stroke in pixels
     */
    public DrawAction(
            String username,
            ShapeType shapeType,
            List<Point> points,
            String color,
            double strokeWidth
    ) {
        super(username);
        this.shapeType = shapeType;
        this.points = List.copyOf(points);
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public List<Point> getPoints() {
        return points;
    }

    public String getColor() {
        return color;
    }

    public double getStrokeWidth() {
        return strokeWidth;
    }

    @Override
    public String toString() {
        return String.format(
                "DrawAction[%s, points=%s, color=%s, width=%.1f]",
                shapeType,
                points,
                color,
                strokeWidth
        );
    }

    @Override
    public String getType() {
        return "Action.Draw";
    }
}
