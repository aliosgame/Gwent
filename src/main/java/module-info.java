module gwent {
    requires java.logging;
    requires java.xml;
    requires javafx.controls;
    requires javafx.media;

    exports gwent.launch;
    opens gwent.launch to javafx.graphics;
}
