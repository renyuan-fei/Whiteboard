module org.whiteboard.common {
    exports org.whiteboard.common.rmi;
    exports org.whiteboard.common.action;
    exports org.whiteboard.common.event;
    exports org.whiteboard.common;
    requires com.google.gson;
    requires java.rmi;
    requires java.desktop;
    requires javafx.graphics;
}