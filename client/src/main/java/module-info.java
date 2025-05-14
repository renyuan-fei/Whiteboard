module org.whiteboard.client {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires org.whiteboard.common;
    requires java.rmi;
    requires java.desktop;
    requires java.compiler;
    requires java.management;

    opens org.whiteboard.client to javafx.fxml;
    exports org.whiteboard.client;
    exports org.whiteboard.client.controller;
    opens org.whiteboard.client.controller to javafx.fxml;
}