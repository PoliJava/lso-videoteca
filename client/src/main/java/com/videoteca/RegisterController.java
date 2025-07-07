package com.videoteca;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username e Password non possono essere vuoti.");
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Credenziali vuote");
            alert.setHeaderText(null);
            alert.setContentText("Username e Password non possono essere vuoti.");
            alert.showAndWait();
            return;
        }

        // Connessione al server
        try (Socket socket = new Socket("videoteca-server", 8080);
                OutputStream output = socket.getOutputStream();
                InputStream input = socket.getInputStream()) {

            output.write("1\n".getBytes());

            output.write((username + "\n").getBytes());
            output.write((password + "\n").getBytes());

            output.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String response = reader.readLine();
            System.out.println("Server response: " + response);

            if (response == null) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Errore di connessione");
                alert.setHeaderText(null);
                alert.setContentText("Nessuna risposta dal server");
                alert.showAndWait();
                return;
            }

            if ("SUCCESS".equals(response)) {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Registrazione Successo");
                alert.setHeaderText(null);
                alert.setContentText("Registrazione effettuata con successo!");
                alert.showAndWait();
                App.setRoot("landing");
            } else if ("ERROR:USERNAME_EXISTS".equals(response)) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Registrazione Fallita");
                alert.setHeaderText(null);
                alert.setContentText("Username già esistente. Scegli un altro username.");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Registrazione Fallita");
                alert.setHeaderText(null);
                alert.setContentText("Errore durante la registrazione: " + response);
                alert.showAndWait();
            }

        } catch (IOException e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Errore di Connessione");
            alert.setHeaderText(null);
            alert.setContentText("Impossibile connettersi al server: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }
}
