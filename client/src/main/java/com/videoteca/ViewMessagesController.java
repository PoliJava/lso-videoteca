package com.videoteca;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

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

    private ObservableList<Message> messageList; // Make private

    @FXML
    public void initialize() {
        // Initialize columns
        adminColumn.setCellValueFactory(new PropertyValueFactory<>("admin"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        expireDateColumn.setCellValueFactory(new PropertyValueFactory<>("expireDate"));
    
        // Initialize the list BEFORE loading data
        messageList = FXCollections.observableArrayList();
        messagesTable.setItems(messageList); // Set items immediately
        
        loadMessagesFromDatabase();
    }

    private void loadMessagesFromDatabase() {
        String url = "jdbc:sqlite:C:/Users/PlayXtreme/Desktop/Uni/PROGETTI/LSO/lso-videoteca-1/lso-videoteca/server/videoteca.db";
        String sql = "SELECT me.id, me.username, me.sender, mo.title, me.message, me.movieId, re.returndate "
                   + "FROM messages me "
                   + "JOIN movies mo ON me.movieid = mo.id "
                   + "JOIN rentals re ON re.movieId = me.movieId AND re.username = me.username";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            // Clear existing items
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
            // Handle error (e.g., show alert to user)
        }
    }


    /*private void showMessageDetail(Message message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/messageDetail.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Dettaglio Messaggio");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setFullScreen(true);
            Scene scene = new Scene(loader.load());
            MessageDetailController controller = loader.getController();
            controller.setMessage(message);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/
    }

