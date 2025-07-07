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

    @FXML
    private void handleMailboxClick() throws IOException {
        loadPage("viewMessages.fxml");
    }

    @FXML
    private String username;

    public void setUsername(String username) {
        this.username = username;
    }

    private void loadPage(String page) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(page));
        Node node = loader.load();

        if (page.equals("viewCart.fxml")) {
            ViewCartController controller = loader.getController();
            controller.setUsername(Session.getUsername());
            controller.loadCartItems();
        }

        if (page.equals("viewRentedMovies.fxml")) {
            ViewRentalsController rentcontr = loader.getController();
            rentcontr.loadRentItems();
        }

        contentPane.getChildren().setAll(node);
    }
}
