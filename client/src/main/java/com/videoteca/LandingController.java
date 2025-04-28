package com.videoteca;

import java.io.IOException;

import javafx.fxml.FXML;

public class LandingController {

    @FXML
    private void switchToRegister() throws IOException {
        System.out.println("Switching to register page...");
        App.setRoot("register");
    }

    @FXML
    private void switchToLogin() throws IOException {
        App.setRoot("login");
    }
}
