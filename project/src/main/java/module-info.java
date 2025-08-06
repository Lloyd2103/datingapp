module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    
    exports com.example.gui;
    opens com.example.gui to javafx.fxml;

}
