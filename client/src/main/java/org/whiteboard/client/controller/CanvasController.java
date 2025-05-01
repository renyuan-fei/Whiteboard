package org.whiteboard.client.controller;

import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.whiteboard.client.ConnectionManager;
import org.whiteboard.common.Point;
import org.whiteboard.common.action.DrawAction;
import org.whiteboard.common.action.EraseAction;
import org.whiteboard.common.action.TextAction;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class CanvasController {

    @FXML
    private Canvas canvas;

    @FXML
    private Canvas previewCanvas;

    @FXML
    private AnchorPane overlayPane;

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

    // Used for split single click and double click
//    private final PauseTransition clickTimer = new PauseTransition(Duration.millis(100));

    private enum ToolType {FREEHAND, Text, LINE, RECTANGLE, OVAL, TRIANGLE, ERASER}

    private ToolType currentTool = ToolType.FREEHAND;

    private static class TextElement {
        String text;
        double x, y;
        Rectangle2D bounds;
        double scale;

        Color color;
    }

    private final List<TextElement> texts = new ArrayList<>();

    private TextField editingField;

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
        choiceBox.getItems().addAll("Freehand", "Text", "Line", "Rectangle", "Oval", "Triangle", "Eraser");
        choiceBox.setValue("Freehand");
        choiceBox.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            switch (val) {
                case "Freehand" -> currentTool = ToolType.FREEHAND;
                case "Text" -> currentTool = ToolType.Text;
                case "Line" -> currentTool = ToolType.LINE;
                case "Rectangle" -> currentTool = ToolType.RECTANGLE;
                case "Oval" -> currentTool = ToolType.OVAL;
                case "Triangle" -> currentTool = ToolType.TRIANGLE;
                case "Eraser" -> currentTool = ToolType.ERASER;
            }
        });

        // update stroke color when picker changes
        colorPicker.setOnAction(e -> gc.setStroke(colorPicker.getValue()));

        // update line width when slider moves
        slider.valueProperty().addListener((obs, oldVal, newVal) ->
                gc.setLineWidth(newVal.doubleValue() / 5)
        );
        slider.setValue(5.0);


        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            Point curr = new Point(e.getX(), e.getY());

            if (currentTool == ToolType.FREEHAND) {
                gc.setFill(colorPicker.getValue());
                double size = slider.getValue();
                gc.fillRect(curr.getX() - size / 2, curr.getY() - size / 2, size, size);
                sendPoint(curr);
            } else if (currentTool == ToolType.ERASER) {
                // Set larger size for eraser
                double size = slider.getValue() * 5;

                // Clear the area around the current point
                gc.clearRect(curr.getX() - size / 2, curr.getY() - size / 2, size, size);

                sendErase(curr, size);
            } else if (currentTool == ToolType.Text) {

                // commit editing field if it exists
                if (editingField != null) {
                    commitEditingField();
                    return;
                }

                // otherwise, show the text editor
                if (e.getClickCount() == 1) {
                    showTextEditor("", e.getX(), e.getY());
                }
            }
        });

        // drawing handlers
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            startPoint = new Point(e.getX(), e.getY());
            lastPoint = startPoint;

            if (currentTool == ToolType.FREEHAND || currentTool == ToolType.ERASER) {
                gc.beginPath();
                gc.moveTo(startPoint.getX(), startPoint.getY());
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            Point curr = new Point(e.getX(), e.getY());
            gc.setStroke(colorPicker.getValue());
            gc.setLineWidth(slider.getValue());

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
            case Point -> {
                // Draw a point
                System.out.println("Drawing point");
                gc.fillRect(startPoint.getX(), startPoint.getY(), action.getStrokeWidth(), action.getStrokeWidth());
            }
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

    private void sendPoint(Point point) {
        DrawAction act = new DrawAction(
                0,
                connectionManager.getUsername(),
                null,
                DrawAction.ShapeType.Point,
                List.of(point),
                colorPicker.getValue().toString(),
                slider.getValue()
        );
        try {
            connectionManager.drawAction(act);
        } catch (RemoteException ignored) {
        }
    }

    // Send a Freehand segment to the server
    private void sendSegment(Point p1, Point p2, Color color, double width) {
        DrawAction act = new DrawAction(
                0, connectionManager.getUsername(), null,
                DrawAction.ShapeType.FREEHAND, List.of(p1, p2),
                color.toString(), width
        );
        try {
            connectionManager.drawAction(act);
        } catch (RemoteException ignored) {
        }
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
        gc.strokePolygon(
                new double[]{a.getX(), b.getX(), c.getX()},
                new double[]{a.getY(), b.getY(), c.getY()},
                3
        );
    }

    private void showTextEditor(String initText, double x, double y) {

        if (editingField != null) {
            hideTextEditor();
        }

        editingField = new TextField(initText);
        editingField.setLayoutX(x);
        editingField.setLayoutY(y);
        editingField.setPrefColumnCount(Math.max(10, initText.length()));
        editingField.setFont(Font.font(slider.getValue() * 4));
        editingField.setStyle("-fx-text-fill: " + toCssColor(colorPicker.getValue()) + ";");

        overlayPane.getChildren().add(editingField);
        overlayPane.setMouseTransparent(false);
        editingField.requestFocus();

        // commit when the user presses enter, click outside, or presses escape
        editingField.setOnAction(e -> commitEditingField());
        editingField.focusedProperty().addListener((obs, oldV, has) -> {
            if (!has) commitEditingField();
        });
        editingField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                commitEditingField();
            }
        });
    }

    private String toCssColor(Color c) {
        return String.format("rgba(%d,%d,%d,%.2f)",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255), c.getOpacity());
    }

    private void hideTextEditor() {
        if (editingField != null) {
            overlayPane.getChildren().remove(editingField);
            editingField = null;
            overlayPane.setMouseTransparent(true);
        }
    }

    private void commitEditingField() {
        if (editingField == null) return;

        String txt = editingField.getText();

        if (txt != null && !txt.isBlank()) {

            // hide and disable the editing field
            editingField.setEditable(false);
            editingField.setVisible(false);

            gc.setFill(colorPicker.getValue());

            // compute text height to align baseline properly
            double scale = slider.getValue();
            gc.setFont(new Font(scale * 4));
            double textHeight = computeTextHeight(txt, gc.getFont());

            double textPositionX = editingField.getLayoutX();
            double textPositionY = editingField.getLayoutY() + textHeight;

            gc.fillText(txt, textPositionX, textPositionY);

            TextElement te = new TextElement();
            te.text = txt;
            te.x = textPositionX;
            te.y = textPositionY;
            te.scale = scale;
            texts.add(te);

            // Send the text action to the server
            sendTextAction(txt, textPositionX, textPositionY, scale);
        }

        // Hide the text editor after committing
        hideTextEditor();
    }

    private void sendTextAction(String txt, double x, double y, double scale) {
        TextAction action = new TextAction(
                0,
                connectionManager.getUsername(),
                null,
                txt,
                new Point(x, y),
                scale,
                colorPicker.getValue().toString()
        );
        try {
            connectionManager.textAction(action);
        } catch (RemoteException ignored) {
        }
    }

    public void renderRemoteTextAction(TextAction textAction) {
        gc.setFont(new Font(textAction.getScale() * 4));
        gc.setFill(Color.web(textAction.getColor()));
        gc.fillText(textAction.getText(), textAction.getPosition().getX(), textAction.getPosition().getY());

        TextElement te = new TextElement();
        te.text = textAction.getText();
        te.x = textAction.getPosition().getX();
        te.y = textAction.getPosition().getY();
        te.scale = textAction.getScale();
        texts.add(te);
    }

    private double computeTextWidth(String txt, Font font) {
        Text helper = new Text(txt);
        helper.setFont(font);
        return helper.getLayoutBounds().getWidth();
    }

    private double computeTextHeight(String txt, Font font) {
        Text helper = new Text(txt);
        helper.setFont(font);
        return helper.getLayoutBounds().getHeight();
    }
}
