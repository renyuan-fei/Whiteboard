package org.whiteboard.common;

import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

public record TextElement(
        String text,
        double x,
        double y,
        Rectangle2D bounds,
        double scale,
        Color color
) {
}
