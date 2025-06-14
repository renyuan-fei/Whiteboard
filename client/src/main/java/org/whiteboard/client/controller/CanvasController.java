package org.whiteboard.client.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
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
import org.whiteboard.common.action.Action;
import org.whiteboard.common.action.DrawAction;
import org.whiteboard.common.action.EraseAction;
import org.whiteboard.common.action.TextAction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

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
        // inject the controller into the connection manager
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

        canvas.addEventHandler(MouseEvent.MOUSE_EXITED, e ->
                pgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight()));


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
                    showTextEditor(e.getX(), e.getY());
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
                    gc.strokeLine(lastPoint.getX(), lastPoint.getY(), curr.getX(), curr.getY());

                    sendSegment(lastPoint, curr, colorPicker.getValue(), slider.getValue());

                }
                case ERASER -> {

                    // Set larger size for eraser
                    double size = slider.getValue();

                    // Real-time preview in local canvas
                    pgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
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
            case FREEHAND -> {
                List<Point> pts = action.getPoints();
                // At least 2 points are needed to draw a line
                if (pts.size() < 2) return;
                Point a = pts.get(0), b = pts.get(1);
                gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
            }
            case POINT ->
                    gc.fillRect(startPoint.getX() - size / 2, startPoint.getY() - size / 2, action.getStrokeWidth(), action.getStrokeWidth());
            case LINE -> drawLine(startPoint, end, gc);
            case RECTANGLE -> drawRectangle(startPoint, end, gc);
            case OVAL -> drawOval(startPoint, end, gc);
            case TRIANGLE -> drawTriangle(startPoint, end, gc);
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
        DrawAction action = new DrawAction(
                connectionManager.getUsername(),
                DrawAction.ShapeType.POINT,
                List.of(point),
                colorPicker.getValue().toString(),
                slider.getValue()
        );
        connectionManager.drawAction(action)
                .exceptionally(ex -> {
                    System.err.println("Error: Async Failure sending draw action: " + ex.getMessage());

                    return null;
                });
    }

    // Send a Freehand segment to the server
    private void sendSegment(Point p1, Point p2, Color color, double width) {
        DrawAction action = new DrawAction(
                connectionManager.getUsername(),
                DrawAction.ShapeType.FREEHAND, List.of(p1, p2),
                color.toString(), width
        );
        connectionManager.drawAction(action)
                .exceptionally(ex -> {
                    System.err.println("Error: Async Failure sending draw action: " + ex.getMessage());

                    return null;
                });
    }

    private void sendErase(Point point, double size) {
        EraseAction action = new EraseAction(
                connectionManager.getUsername(),
                List.of(point),
                size
        );
        connectionManager.eraseAction(action)
                .exceptionally(ex -> {
                    System.err.println("Error: Async Failure sending erase action: " + ex.getMessage());

                    return null;
                });

    }

    // Send a shape to the server
    private void sendShape(List<Point> pts, DrawAction.ShapeType type) {
        DrawAction action = new DrawAction(
                connectionManager.getUsername(),
                type,
                pts,
                colorPicker.getValue().toString(),
                slider.getValue()
        );
        connectionManager.drawAction(action)
                .exceptionally(ex -> {
                    System.err.println("Error: Async Failure sending shape action: " + ex.getMessage());

                    return null;
                });
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

    private void showTextEditor(double x, double y) {

        if (editingField != null) {
            hideTextEditor();
        }

        editingField = new TextField("");

        double fontHeight = computeTextHeight(slider.getValue());

        // agile the text field and cursor
        editingField.setLayoutX(x);
        editingField.setLayoutY(y - fontHeight / 2);


        editingField.setPrefColumnCount(10);
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
                connectionManager.getUsername(),
                TextAction.TextType.ADD,
                textElement
        );
        connectionManager.textAction(action)
                .exceptionally(ex -> {
                    System.err.println("Error: Async Failure sending text add action: " + ex.getMessage());

                    return null;
                });
    }

    private void sendRemoveTextAction(TextElement textElement) {
        TextAction action = new TextAction(
                connectionManager.getUsername(),
                TextAction.TextType.REMOVE,
                textElement
        );
        connectionManager.textAction(action)
                .exceptionally(ex -> {
                    System.err.println("Error: Async Failure sending text remove action: " + ex.getMessage());

                    return null;
                });
    }

    public void renderRemoteTextAction(TextAction textAction) {
        tgc.setFill(Color.web(textAction.getColor()));
        tgc.setFont(new Font(textAction.getScale() * 4));
        double textHeight = computeTextHeight(textAction.getScale());
        tgc.fillText(textAction.getText(), textAction.getPosition().getX(), textAction.getPosition().getY() + textHeight * 0.8);

        // add the text element to the list
        TextElement textElement = textAction.getTextElement();
        textElements.add(textElement);
    }

    public void renderRemoteRemoveTextActions(TextAction textActions) {
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
        return helper.getLayoutBounds().getHeight();
    }

    private TextElement hitText(double x, double y) {
        // Iterate in reverse order to find the topmost text element
        for (int i = textElements.size() - 1; i >= 0; i--) {
            TextElement t = textElements.get(i);

            // Check if the point is within the bounds of the text element
            if (t.bounds().contains(x, y)) {
                return t;
            }
        }
        return null;
    }

    private void reDrawText() {
        // Clear the text canvas
        tgc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Redraw all text elements
        for (TextElement textElement : textElements) {
            tgc.setFont(new Font(textElement.scale() * 4));
            tgc.setFill(Color.web(textElement.color().toString()));
            double textHeight = computeTextHeight(textElement.scale());
            tgc.fillText(textElement.text(), textElement.x(), textElement.y() + textHeight * 0.8);
        }
    }

    // import canvas
    public void importCanvas(String canvasData) {
        try {
            // Rebuild canvas
            parseAndRebuildCanvas(canvasData);

        } catch (Exception e) {
            System.err.println("Error: Fail to import data" + e.getMessage());
        }
    }

    private void parseAndRebuildCanvas(String canvasData) {
        try {
            byte[] data = Base64.getDecoder().decode(canvasData);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {

                @SuppressWarnings("unchecked")
                List<Action> actions = (List<Action>) ois.readObject();

                // clean the current canvas
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                tgc.clearRect(0, 0, textCanvas.getWidth(), textCanvas.getHeight());
                textElements.clear();

                // use actions to rebuild canvas in local
                for (Action action : actions) {
                    if (action instanceof DrawAction) {
                        renderRemoteDrawAction((DrawAction) action);
                    } else if (action instanceof EraseAction) {
                        renderRemoteEraseAction((EraseAction) action);
                    } else if (action instanceof TextAction textAction) {
                        if (Objects.equals(textAction.getType(), "Action.Text")) {
                            renderRemoteTextAction(textAction);
                        } else {
                            renderRemoteRemoveTextActions(textAction);
                        }
                    }
                }

                System.out.println("Canvas rebuild success, rebuild: " + actions.size() + " actions");
            }
        } catch (Exception e) {
            System.err.println("Error: Canvas rebuild failed" + e.getMessage());
        }
    }


    public void exportCanvasAsImage(String filename, String downloadDir, String type) throws IOException {
        File outputFile = new File(downloadDir, filename);

        WritableImage image = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(new SnapshotParameters(), image);

        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        try {
            ImageIO.write(bufferedImage, type, outputFile);
            System.out.println("Saved to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error: Fail to save canvas as " + type);
        }
    }

    public void clearCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void blockCanvas() {
        canvas.setDisable(true);
        connectionManager.getMainController().setLabelText("Canvas is closed");
    }

    public void unblockCanvas() {
        canvas.setDisable(false);
        connectionManager.getMainController().hideLabelText();
    }
}
