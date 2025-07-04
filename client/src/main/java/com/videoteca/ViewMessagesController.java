package com.videoteca;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button; // Import Button
import javafx.scene.control.TableCell; // Import TableCell
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback; // Import Callback

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ViewMessagesController {

    @FXML
    private TableView<Message> messagesTable;
    @FXML
    private TableColumn<Message, String> adminColumn;
    @FXML
    private TableColumn<Message, String> titleColumn;
    @FXML
    private TableColumn<Message, String> expireDateColumn;
    // NEW: TableColumn for the button
    @FXML
    private TableColumn<Message, Void> detailsButtonColumn; // Type is Void because the cell itself doesn't hold data

    private ObservableList<Message> messageList;

    @FXML
    public void initialize() throws Exception {
        // Initialize existing columns
        adminColumn.setCellValueFactory(new PropertyValueFactory<>("admin"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        expireDateColumn.setCellValueFactory(new PropertyValueFactory<>("expireDate"));

        // Initialize the NEW button column
        setupDetailsButtonColumn(); // Call the new setup method

        // Initialize the list BEFORE loading data
        messageList = FXCollections.observableArrayList();
        messagesTable.setItems(messageList);

        loadMessagesFromDatabase();

    }

    private void setupDetailsButtonColumn() {
        // Define a CellFactory for the detailsButtonColumn
        Callback<TableColumn<Message, Void>, TableCell<Message, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Message, Void> call(final TableColumn<Message, Void> param) {
                final TableCell<Message, Void> cell = new TableCell<>() {
                    private final Button btn = new Button("Dettagli"); // Text for the button

                    {
                        // Action when the button is clicked
                        btn.setOnAction(event -> {
                            Message message = getTableView().getItems().get(getIndex());
                            showMessageDetail(message); // Call your detail method
                        });
                        // Optional: Add styling
                        btn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null); // No button if row is empty
                        } else {
                            setGraphic(btn); // Show button
                        }
                    }
                };
                return cell;
            }
        };

        detailsButtonColumn.setCellFactory(cellFactory);
        // Optional: Set a preferred width or min/max width for the button column
        detailsButtonColumn.setPrefWidth(100);
        detailsButtonColumn.setResizable(false); // Make it not resizable
    }



  private List<Message> loadMessagesFromDatabase() throws Exception {
        List<Message> messages = new ArrayList<>();

        try (Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("15");

            int expectedRecords = 0;
            int currentRecord = 0;
            Message message = null;

            String line;
            while ((line = in.readLine()) != null && !line.equals("END")) {
                if (line.startsWith("TITLE:")) {
                    message = new Message();
                    message.setTitle(line.substring(6));
                } else if (line.startsWith("SENDER:")) {
                    if (message != null)
                        message.setSender(line.substring(7)); 
                } else if (line.startsWith("USER:")) {
                    if (message != null)
                        message.setUser(line.substring(5)); 
                } else if (line.startsWith("TEXT:")) {
                    if (message != null)
                        message.setContent(line.substring(5));
                } 

                if(message != null && message.getUser().equals(Session.username)){
                    messages.add(message);
                } 

            }

            System.out.println("Received " + currentRecord + " of " + expectedRecords + " records");
        }
        return messages;
    }



    private void showMessageDetail(Message message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/videoteca/messageDetail.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Dettaglio Messaggio");
            stage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(loader.load());
            MessageDetailController controller = loader.getController();
            controller.setMessage(message);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading messageDetail.fxml: " + e.getMessage());
        }
    }
}