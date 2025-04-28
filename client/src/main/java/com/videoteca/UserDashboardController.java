package com.videoteca;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import java.io.IOException;

public class UserDashboardController {

    @FXML
    private StackPane contentPane;

    @FXML
    private void handleViewMovies() throws IOException {
        loadPage("viewMovies.fxml");
    }

    @FXML
    private void handleViewRentedMovies() throws IOException {
       loadPage("viewRentedMovies.fxml");
    }

    @FXML
    private void handleViewCart() throws IOException {
        loadPage("viewCart.fxml");
    }

    private void loadPage(String page) throws IOException {
        Node node = FXMLLoader.load(getClass().getResource(page));
        contentPane.getChildren().setAll(node);
    }
}
