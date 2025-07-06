package com.videoteca;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    private void switchToLanding() throws IOException {
        App.setRoot("landing");
    }

    @FXML
    private TextField usernameField;
    @FXML
    private TextField passwordField;

    @FXML
     void handleLogin() throws IOException {
        String logName = usernameField.getText();
        String password = passwordField.getText();

        if(logName.isEmpty() || password.isEmpty()) {
            System.out.println("Username and password cannot be empty.");
            return;
        }

        //Connessione al server
        try(Socket socket = new Socket("videoteca-server", 8080);
            OutputStream output = socket.getOutputStream();
            InputStream input = socket.getInputStream()) {

                //Invia al server la scelta 2
                output.write("2\n".getBytes());
                output.flush();

                //Invia username e password al server
                output.write((logName + "\n").getBytes());
                output.flush();
                output.write((password + "\n").getBytes());
                output.flush();

                System.out.println("Dati di login inviati al server.");

                byte[] buffer = new byte[1024];
                int bytesRead = input.read(buffer);
                String response = new String(buffer, 0, bytesRead);
                System.out.println("Risposta del server: " + response);

                if(response.contains("Login riuscito!")){
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Login Successo");
                    alert.setHeaderText(null);
                    alert.setContentText("Login riuscito!");
                    alert.showAndWait();
                    Session.setUsername(logName);
                    App.setRoot("userDashboard");
                } else{
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Login Fallito");
                    alert.setHeaderText(null);
                    alert.setContentText("Login fallito, credenziali errate");
                    alert.showAndWait();
                    App.setRoot("landing");
                }

             } catch(IOException e) {
            e.printStackTrace();
            System.out.println("Errore durante la connessione al server.");
            return;
        }
    }
}
