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

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
    public void initialize() {
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

        // REMOVE THE OLD DOUBLE-CLICK LISTENER if you prefer only the button
        // If you want both double-click and button, keep this block.
        // messagesTable.setOnMouseClicked(event -> {
        //     if (event.getClickCount() == 2) {
        //         Message selectedMessage = messagesTable.getSelectionModel().getSelectedItem();
        //         if (selectedMessage != null) {
        //             showMessageDetail(selectedMessage);
        //         }
        //     }
        // });
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


    private void loadMessagesFromDatabase() {
    String url = "jdbc:sqlite:C:\\Users\\PlayXtreme\\Desktop\\Uni\\PROGETTI\\LSO\\lso-videoteca\\server\\videoteca.db"; //da rendere universale

    String sql = "SELECT me.id, me.username, me.sender, mo.title, me.message, me.movieId, re.returndate " +
                     "FROM messages me " +
                     "JOIN movies mo ON me.movieid = mo.id " +
                     "JOIN rentals re ON re.movieId = me.movieId AND re.username = me.username";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            messageList.clear();

            while (rs.next()) {
                messageList.add(new Message(
                    rs.getString("sender"),
                    rs.getString("username"),
                    rs.getString("title"),
                    rs.getString("returndate"),
                    rs.getInt("movieId"),
                    rs.getString("message")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database error: " + e.getMessage());
            // Handle error (e.g., show alert to user)
        }
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