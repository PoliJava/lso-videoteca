module com.videoteca {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.base;

    opens com.videoteca to javafx.fxml;
    exports com.videoteca;
}
