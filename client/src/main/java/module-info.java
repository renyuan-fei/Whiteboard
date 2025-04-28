module org.whiteboard.client {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens org.whiteboard.client to javafx.fxml;
    exports org.whiteboard.client;
}