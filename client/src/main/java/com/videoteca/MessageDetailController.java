package com.videoteca;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.text.Text;

public class MessageDetailController {

    @FXML
    private Label titleLabel;
    @FXML
    private Label senderLabel;
    @FXML
    private Text messageContentText;

    /*
     * Setta i dettagli del messaggio da visualizzare nel pop-up.
     * 
     * @param message Il messaggio da visualizzare.
     */
    public void setMessage(Message message) {
        if (message != null) {
            titleLabel.setText(message.getTitle());
            senderLabel.setText("Da: " + message.getAdmin() + " (Utente: " + message.getUser() + ")");
            messageContentText.setText(message.getContent());
        }
    }
}