package org.whiteboard.common.action;

import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import org.whiteboard.common.Point;
import org.whiteboard.common.TextElement;

import java.io.Serial;
import java.time.Instant;

public class TextAction extends Action {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String text;

    private final Point position;

    private final Double scale;

    private final String color;

    private final TextType type;

    private final double minX;

    private final double minY;

    private final double width;

    private final double height;

    public enum TextType {
        ADD,
        REMOVE,
        UPDATE
    }

    /**
     * @param actionId  unique identifier for this action
     * @param username  the user who generated this action
     * @param timestamp time when the action was created
     * @param text      text to be drawn
     * @param position  position of the text on the whiteboard
     * @param scale     scale of the text
     */
    public TextAction(long actionId, String username, Instant timestamp, String text, Point position, Double scale, String color, TextType type, double minX, double minY, double width, double height) {
        super(actionId, username, timestamp);
        this.text = text;
        this.position = position;
        this.scale = scale;
        this.color = color;
        this.type = type;
        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
    }

    public TextAction(long actionId, String username, Instant timestamp, TextType type, TextElement textElement) {
        super(actionId, username, timestamp);
        this.text = textElement.text();
        this.position = new Point(textElement.x(), textElement.y());
        this.scale = textElement.scale();
        this.color = textElement.color().toString();
        this.type = type;
        this.minX = textElement.bounds().getMinX();
        this.minY = textElement.bounds().getMinY();
        this.width = textElement.bounds().getWidth();
        this.height = textElement.bounds().getHeight();
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

    public String getColor() {
        return color;
    }

    public TextElement getTextElement() {
        return new TextElement(
                text,
                position.getX(),
                position.getY(),
                new Rectangle2D(
                        minX,
                        minY,
                        width,
                        height
                ),
                scale,
                Color.web(color));
    }

    public TextType getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format(
                "TextAction[%s, text=%s, position=%s, scale=%.1f, color=%s]",
                type,
                text,
                position,
                scale,
                color
        );
    }
}
