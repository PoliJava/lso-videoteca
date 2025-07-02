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
     * Sets the message details to be displayed in the pop-up.
     * @param message The Message object containing the details.
     */
    public void setMessage(Message message) {
        if (message != null) {
            titleLabel.setText(message.getTitle());
            // Assuming 'admin' field in Message class represents the sender
            senderLabel.setText("From: " + message.getAdmin() + " (User: " + message.getUser() + ")");
            messageContentText.setText(message.getContent());
        }
    }
}