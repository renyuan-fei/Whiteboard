package org.whiteboard.client.controller;

import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
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
import javafx.scene.text.TextBoundsType;
import org.whiteboard.client.ConnectionManager;
import org.whiteboard.common.Point;
import org.whiteboard.common.TextElement;
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
    private Canvas textCanvas;

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

    // Graphics context for drawing on the text canvas
    private GraphicsContext tgc;

    // Used for freehand drawing and shape creation
    private Point lastPoint;

    // Used for Eraser
    private Point startPoint;

    private enum ToolType {FREEHAND, Text, LINE, RECTANGLE, OVAL, TRIANGLE, ERASER}

    private ToolType currentTool = ToolType.FREEHAND;

    private final List<TextElement> textElements = new ArrayList<>();

    private TextField editingField;

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    @FXML
    public void initialize() {
        // inject the canvas controller into the connection manager
        ConnectionManager.getInstance().setCanvasController(this);

        // setup canvas
        gc = canvas.getGraphicsContext2D();
        tgc = textCanvas.getGraphicsContext2D();
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

        // Mouse

        // hide the cursor in init
        canvas.setCursor(Cursor.NONE);

        // hide cursor when mouse is over the canvas
        canvas.addEventHandler(MouseEvent.MOUSE_ENTERED, e ->
                canvas.setCursor(Cursor.NONE)
        );

        // show cursor when mouse exits the canvas
        canvas.addEventHandler(MouseEvent.MOUSE_EXITED, e ->
                canvas.setCursor(Cursor.DEFAULT)
        );

        // preview with custom cursor
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            pgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            double size = slider.getValue();

            if (currentTool == ToolType.FREEHAND
                    || currentTool == ToolType.LINE
                    || currentTool == ToolType.RECTANGLE
                    || currentTool == ToolType.OVAL
                    || currentTool == ToolType.TRIANGLE) {
                // preview color and size
                pgc.setFill(colorPicker.getValue());
                pgc.fillRect(
                        e.getX() - size / 2,
                        e.getY() - size / 2,
                        size,
                        size
                );
            } else if (currentTool == ToolType.ERASER) {
                pgc.setLineWidth(1);
                pgc.strokeRect(
                        e.getX() - size / 2,
                        e.getY() - size / 2,
                        size,
                        size
                );
            } else if (currentTool == ToolType.Text) {
                // calculate text height
                double fontSize = slider.getValue() * 4;
                Font font = Font.font(fontSize);
                Text helper = new Text("Ay");
                helper.setFont(font);
                Bounds lb = helper.getLayoutBounds();
                double ascent = lb.getMinY() / 2;

                // get the baseline
                double baselineX = e.getX();
                double baselineY = e.getY();

                // calculate the top and bottom of the position of y
                double topY = baselineY - ascent;
                double bottomY = baselineY + ascent;

                pgc.setStroke(colorPicker.getValue());
                pgc.setLineWidth(1);
                pgc.strokeLine(baselineX, topY, baselineX, bottomY);
            }

        });

        canvas.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            pgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        });


        // Canvas
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            Point curr = new Point(e.getX(), e.getY());

            if (currentTool == ToolType.FREEHAND) {

                gc.setFill(colorPicker.getValue());
                double size = slider.getValue();
                gc.fillRect(curr.getX() - size / 2, curr.getY() - size / 2, size, size);
                sendPoint(curr);

            } else if (currentTool == ToolType.ERASER) {
                // Set larger size for eraser
                double size = slider.getValue();

                // Clear the area around the current point
                gc.clearRect(
                        curr.getX() - size / 2,
                        curr.getY() - size / 2,
                        size,
                        size);

                TextElement text = hitText(curr.getX(), curr.getY());
                if (text != null) {
                    textElements.remove(text);
                    sendRemoveTextAction(text);
                    reDrawText();
                }

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
                    double size = slider.getValue();

                    gc.strokeLine(lastPoint.getX(), lastPoint.getY(), curr.getX(), curr.getY());

                    sendSegment(lastPoint, curr, colorPicker.getValue(), slider.getValue());

                }
                case ERASER -> {

                    // Set larger size for eraser
                    double size = slider.getValue();

                    // Real-time preview in local canvas
                    pgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
//                    pgc.setLineWidth(size);
                    pgc.strokeRect(
                            curr.getX() - size / 2,
                            curr.getY() - size / 2,
                            size,
                            size
                    );

                    // Clear the area around the current point
                    gc.clearRect(
                            curr.getX() - size / 2,
                            curr.getY() - size / 2,
                            size,
                            size);

                    TextElement text = hitText(curr.getX(), curr.getY());
                    if (text != null) {
                        textElements.remove(text);
                        sendRemoveTextAction(text);
                        reDrawText();
                    }

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
        double size = action.getStrokeWidth();

        switch (action.getShapeType()) {
            case POINT -> {
                // Draw a point
                gc.fillRect(startPoint.getX() - size / 2, startPoint.getY() - size / 2, action.getStrokeWidth(), action.getStrokeWidth());
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
                DrawAction.ShapeType.POINT,
                List.of(point),
                colorPicker.getValue().toString(),
                slider.getValue()
        );
        try {
            connectionManager.drawAction(act);
        } catch (RemoteException e) {
            System.out.println("Failed to send point action: " + e.getMessage());
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
        } catch (RemoteException e) {
            System.out.println("Failed to send draw action: " + e.getMessage());
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
        } catch (RemoteException e) {
            System.out.println("Failed to send erase action: " + e.getMessage());
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
        } catch (RemoteException e) {
            System.out.println("Failed to send shape action: " + e.getMessage());
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

        double fontHeight = computeTextHeight(slider.getValue());

        // agile the text field and cursor
        editingField.setLayoutX(x);
        editingField.setLayoutY(y - fontHeight / 2);


        editingField.setPrefColumnCount(Math.max(10, initText.length()));
        editingField.setFont(Font.font(slider.getValue() * 4));
        editingField.setPadding(new Insets(0));
        editingField.setStyle("-fx-text-fill: " + toCssColor(colorPicker.getValue()) + "; -fx-padding: 0;");

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

            tgc.setFill(colorPicker.getValue());

            // compute text height to align baseline properly
            double scale = slider.getValue();
            tgc.setFont(new Font(scale * 4));
            double textHeight = computeTextHeight(scale);

            double textPositionX = editingField.getLayoutX();
            double textPositionY = editingField.getLayoutY();
            System.out.println(textPositionX + ", " + textPositionY + " " + textHeight);
            System.out.println("Text position: " + textPositionX + ", " + textPositionY + textHeight * 0.8);

            // agile the text position
            tgc.fillText(txt, textPositionX, textPositionY + textHeight * 0.8);

            // add the text element to the list
            TextElement textElement = new TextElement(
                    txt,
                    textPositionX,
                    textPositionY,
                    new Rectangle2D(
                            textPositionX,
                            textPositionY,
                            computeTextWidth(txt, scale),
                            textHeight
                    ),
                    scale,
                    colorPicker.getValue()
            );
            textElements.add(textElement);

            // Send the text action to the server
            sendTextAction(textElement);
        }

        // Hide the text editor after committing
        hideTextEditor();
    }

    private void sendTextAction(TextElement textElement) {
        TextAction action = new TextAction(
                0,
                connectionManager.getUsername(),
                null,
                TextAction.TextType.ADD,
                textElement
        );
        try {
            System.out.println("Sending text action " + action);
            connectionManager.textAction(action);
        } catch (RemoteException e) {
            System.out.println("Failed to send text add action: " + e.getMessage());
        }
    }

    private void sendRemoveTextAction(TextElement textElement) {
        TextAction action = new TextAction(
                0,
                connectionManager.getUsername(),
                null,
                TextAction.TextType.REMOVE,
                textElement
        );
        try {
            connectionManager.textAction(action);
        } catch (RemoteException e) {
            System.out.println("Failed to send text remove action: " + e.getMessage());
        }
    }

    public void renderRemoteTextAction(TextAction textAction) {
        tgc.setFill(Color.web(textAction.getColor()));
        tgc.setFont(new Font(textAction.getScale() * 4));
        double textHeight = computeTextHeight(textAction.getScale());
        System.out.println(textAction.getPosition().getX() + ", " + textAction.getPosition().getY() + " " + textHeight);
        System.out.println("Rendering text action " + textAction + " at " + textAction.getPosition().getX() + ", " + textAction.getPosition().getY() + textHeight * 0.8);
        tgc.fillText(textAction.getText(), textAction.getPosition().getX(), textAction.getPosition().getY() + textHeight * 0.8);

        // add the text element to the list
        TextElement textElement = textAction.getTextElement();
        System.out.println("Adding text element " + textElement);
        textElements.add(textElement);
    }

    public void renderRemoteRemoveTextActions(TextAction textActions) {
        System.out.println("Removing text element " + textActions.getTextElement());
        textElements.remove(textActions.getTextElement());
        reDrawText();
    }

    private double computeTextWidth(String txt, Double scale) {
        Text helper = new Text(txt);
        helper.setFont(new Font(scale * 4));
        return helper.getLayoutBounds().getWidth();
    }

    private double computeTextHeight(Double scale) {
        Text helper = new Text("Ay");
        helper.setFont(new Font(scale * 4));
        helper.setBoundsType(TextBoundsType.LOGICAL);

        System.out.println("Text height: " + helper.fontProperty().get().getSize());
        System.out.println("Text height: " + helper.getLayoutBounds());
        return helper.getLayoutBounds().getHeight();
    }

    private TextElement hitText(double x, double y) {
        for (int i = textElements.size() - 1; i >= 0; i--) {
            TextElement t = textElements.get(i);
            if (t.bounds().contains(x, y)) {
                System.out.println("Hit text at " + t.x() + ", " + t.y());
                return t;
            }
        }
        return null;
    }

    private void reDrawText() {
        tgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        for (TextElement textElement : textElements) {
            tgc.setFont(new Font(textElement.scale() * 4));
            tgc.setFill(Color.web(textElement.color().toString()));
            double textHeight = computeTextHeight(textElement.scale());
            tgc.fillText(textElement.text(), textElement.x(), textElement.y() + textHeight * 0.8);
        }
    }


    private void drawBounds(TextElement te) {
        gc.setLineWidth(1);
        gc.setStroke(Color.RED);
        gc.strokeRect(te.bounds().getMinX(), te.bounds().getMinY(), te.bounds().getWidth(), te.bounds().getHeight());
    }

    // TODO : implement import canvas
    public void importCanvas() {

    }
}
