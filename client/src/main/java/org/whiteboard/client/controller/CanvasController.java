package org.whiteboard.client.controller;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.whiteboard.client.ConnectionManager;
import org.whiteboard.common.Point;
import org.whiteboard.common.action.DrawAction;
import org.whiteboard.common.action.EraseAction;

import java.rmi.RemoteException;
import java.util.List;

public class CanvasController {

    @FXML
    private Canvas canvas;

    @FXML
    private Canvas previewCanvas;

    @FXML
    private ColorPicker colorPicker;

    @FXML
    private Slider slider;

    @FXML
    private ChoiceBox<String> choiceBox;

    // Graphics context for drawing
    private GraphicsContext gc;

    // Graphics context for drawing on the preview canvas
    private GraphicsContext pgc;

    // Used for freehand drawing and shape creation
    private Point lastPoint;

    // Used for Eraser
    private Point startPoint;

    private enum ToolType { FREEHAND, LINE, RECTANGLE, OVAL, TRIANGLE, ERASER }

    private ToolType currentTool = ToolType.FREEHAND;

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    @FXML
    public void initialize() {
        ConnectionManager.getInstance().setCanvasController(this);

        // setup canvas
        gc = canvas.getGraphicsContext2D();
        pgc = previewCanvas.getGraphicsContext2D();
        colorPicker.setValue(Color.BLACK);
        gc.setStroke(colorPicker.getValue());
        gc.setLineWidth(slider.getValue());

        // initialize choice box
        choiceBox.getItems().addAll("Freehand", "Line", "Rectangle", "Oval", "Triangle", "Eraser");
        choiceBox.setValue("Freehand");
        choiceBox.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            switch (val) {
                case "Freehand"   -> currentTool = ToolType.FREEHAND;
                case "Line"       -> currentTool = ToolType.LINE;
                case "Rectangle"  -> currentTool = ToolType.RECTANGLE;
                case "Oval"       -> currentTool = ToolType.OVAL;
                case "Triangle"   -> currentTool = ToolType.TRIANGLE;
                case "Eraser"     -> currentTool = ToolType.ERASER;
            }
        });

        // update stroke color when picker changes
        colorPicker.setOnAction(e -> gc.setStroke(colorPicker.getValue()));

        // update line width when slider moves
        slider.valueProperty().addListener((obs, oldVal, newVal) ->
            gc.setLineWidth(newVal.doubleValue() / 5)
        );
        slider.setValue(5.0);

        // drawing handlers
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            startPoint = new Point(e.getX(), e.getY());
            lastPoint  = startPoint;

            if (currentTool == ToolType.FREEHAND || currentTool == ToolType.ERASER) {
                gc.beginPath();
                gc.moveTo(startPoint.getX(), startPoint.getY());
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            Point curr = new Point(e.getX(), e.getY());

            switch (currentTool) {
                // Real-time drawing
                case FREEHAND -> {
                    gc.lineTo(curr.getX(), curr.getY());
                    gc.stroke();
                    sendSegment(lastPoint, curr, colorPicker.getValue(), slider.getValue());
                }

                // Real-time preview in local canvas
                case ERASER -> {
                    // Set larger size for eraser
                    double size = slider.getValue() * 5;

                    // Clear the area around the current point
                    gc.clearRect(curr.getX() - size / 2, curr.getY() - size / 2, size, size);

                    sendErase(curr, size);
                }
                case LINE -> {
                    pgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawLine(startPoint, curr, pgc);
                }
                case RECTANGLE -> {
                    pgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawRectangle(startPoint, curr, pgc);
                }
                case OVAL -> {
                    pgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawOval(startPoint, curr, pgc);
                }
                case TRIANGLE -> {
                    pgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawTriangle(startPoint, curr, pgc);
                }
            }
            lastPoint = curr;
        });


        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            Point end = new Point(e.getX(), e.getY());
            gc.setStroke(colorPicker.getValue());
            gc.setLineWidth(slider.getValue());

            switch (currentTool) {
                case LINE -> {
                    drawLine(startPoint, end, gc);
                    pgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    sendShape(List.of(startPoint, end), DrawAction.ShapeType.LINE);
                }
                case RECTANGLE -> {
                    drawRectangle(startPoint, end, gc);
                    pgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    sendShape(List.of(startPoint, end), DrawAction.ShapeType.RECTANGLE);
                }
                case OVAL -> {
                    drawOval(startPoint, end, gc);
                    pgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    sendShape(List.of(startPoint, end), DrawAction.ShapeType.OVAL);
                }
                case TRIANGLE -> {
                    drawTriangle(startPoint, end, gc);
                    pgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    sendShape(List.of(startPoint, end), DrawAction.ShapeType.TRIANGLE);
                }
                case FREEHAND, ERASER -> gc.closePath();
            }
        });
    }

    // Render a remote draw action
    public void renderRemoteDrawAction(DrawAction action) {
        gc.setStroke(Color.web(action.getColor()));
        gc.setLineWidth(action.getStrokeWidth());

        Point startPoint = action.getPoints().getFirst();
        Point end = action.getPoints().getLast();

        switch (action.getShapeType()) {
            case FREEHAND -> {
                List<Point> pts = action.getPoints();
                // At least 2 points are needed to draw a line
                if (pts.size() < 2) return;
                Point a = pts.get(0), b = pts.get(1);
                gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
            }
            case LINE -> {
                drawLine(startPoint, end, gc);
            }
            case RECTANGLE -> {
                drawRectangle(startPoint, end, gc);
            }
            case OVAL -> {
                drawOval(startPoint, end, gc);
            }
            case TRIANGLE -> {
                System.out.println(action);
                drawTriangle(startPoint, end, gc);
            }
        }
    }

    // Render a remote erase action
    public void renderRemoteEraseAction(EraseAction eraseAction) {
        double size = eraseAction.getEraserSize();

        // Erase point
        Point p = eraseAction.getErasePath().getFirst();
        gc.clearRect(p.getX() - size / 2, p.getY() - size / 2, size, size);
    }

    // Send a Freehand segment to the server
    private void sendSegment(Point p1, Point p2, Color color, double width) {
        DrawAction act = new DrawAction(
                0, connectionManager.getUsername(), null,
                DrawAction.ShapeType.FREEHAND, List.of(p1, p2),
                color.toString(), width
        );
        try { connectionManager.drawAction(act); } catch (RemoteException ignored) {}
    }

    private void sendErase(Point point, double size) {
        EraseAction er = new EraseAction(
                0,
                connectionManager.getUsername(),
                null,
                List.of(point),
                size
        );
        try {
            connectionManager.eraseAction(er);
        } catch (RemoteException ignored) {

        }
    }

    // Send a shape to the server
    private void sendShape(List<Point> pts, DrawAction.ShapeType type) {
        DrawAction act = new DrawAction(
                0,
                connectionManager.getUsername(),
                null,
                type,
                pts,
                colorPicker.getValue().toString(),
                slider.getValue()
        );
        try {
            connectionManager.drawAction(act);
        } catch (RemoteException ignored) {

        }
    }

    private void drawLine(Point start, Point end, GraphicsContext gc) {
        gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
    }

    private void drawRectangle(Point start, Point end, GraphicsContext gc) {
        double x = Math.min(start.getX(), end.getX());
        double y = Math.min(start.getY(), end.getY());
        double w = Math.abs(start.getX() - end.getX());
        double h = Math.abs(start.getY() - end.getY());
        gc.strokeRect(x, y, w, h);
    }

    private void drawOval(Point start, Point end, GraphicsContext gc) {
        double x = Math.min(start.getX(), end.getX());
        double y = Math.min(start.getY(), end.getY());
        double w = Math.abs(start.getX() - end.getX());
        double h = Math.abs(start.getY() - end.getY());
        gc.strokeOval(x, y, w, h);
    }

    private void drawTriangle(Point start, Point end, GraphicsContext gc) {
        double x1 = start.getX(), y1 = start.getY();
        double x2 = end.getX(), y2 = end.getY();
        Point a = new Point((x1 + x2) / 2, y1);
        Point b = new Point(x1, y2);
        Point c = new Point(x2, y2);
        System.out.println("Drawing triangle from " + a + " to " + b + " to " + c);
        gc.strokePolygon(
                new double[]{a.getX(), b.getX(), c.getX()},
                new double[]{a.getY(), b.getY(), c.getY()},
                3
        );
    }
}
