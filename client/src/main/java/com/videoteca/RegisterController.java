package com.videoteca;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML
    private void switchToLanding() throws IOException {
        App.setRoot("landing");
    }

    @FXML
    private TextField usernameField;
    @FXML
    private TextField passwordField;

    @FXML
    private void handleRegister() throws IOException {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if(username.isEmpty() || password.isEmpty()) {
            // Show an error message to the user
            System.out.println("Username and password cannot be empty.");
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Credenziali vuote");
            alert.setHeaderText(null);
            alert.setContentText("Username e Password non possono essere vuoti.");
            alert.showAndWait();
            return;
        }

        //Connessione al server
        try(Socket socket = new Socket("videoteca-server", 8080);
            OutputStream output = socket.getOutputStream();
            InputStream input = socket.getInputStream()) {

                //Invia al server la scelta 1
                output.write("1\n".getBytes());

                //Invia username e password al server
                output.write((username + "\n").getBytes());
                output.write((password + "\n").getBytes());

                System.out.println("Dati di registrazione inviati al server.");
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Registrazione Successo");
                alert.setHeaderText(null);
                alert.setContentText("Registrazione effettuata con successo!");
                alert.showAndWait();

             } catch(IOException e) {
            e.printStackTrace();
            System.out.println("Errore durante la connessione al server.");
            return;
        }


        App.setRoot("landing");
    }
}
