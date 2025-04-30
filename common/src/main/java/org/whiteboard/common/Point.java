package org.whiteboard.common;

import java.io.Serial;
import java.io.Serializable;

/**
 * Simple immutable 2D point for whiteboard coordinates.
 */
public final class Point implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final double x;
    private final double y;

    /**
     * @param x the X coordinate
     * @param y the Y coordinate
     */
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Gets the X coordinate.
     *
     * @return x
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the Y coordinate.
     *
     * @return y
     */
    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return String.format("Point[x=%.2f, y=%.2f]", x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Point other)) {
            return false;
        }
        return Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(x);
        bits = 31 * bits + Double.doubleToLongBits(y);
        return (int) (bits ^ (bits >>> 32));
    }
}
