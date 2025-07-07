package com.videoteca;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
    @FXML
    private TableColumn<Message, Void> detailsButtonColumn;

    private ObservableList<Message> messageList;

    @FXML
    public void initialize() throws Exception {
        try {
            // Initialize columns
            adminColumn.setCellValueFactory(new PropertyValueFactory<>("admin"));
            titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
            expireDateColumn.setCellValueFactory(new PropertyValueFactory<>("expireDate"));
            setupDetailsButtonColumn();

            messageList = FXCollections.observableArrayList();
            messagesTable.setItems(messageList);

            System.out.println("Loading messages for user: " + Session.username);
            List<Message> loadedMessages = loadMessagesFromDatabase();
            System.out.println("Successfully loaded " + loadedMessages.size() + " messages");

            messageList.setAll(loadedMessages);

        } catch (Exception e) {
            System.err.println("Error initializing messages view: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load messages");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }

    }

    private void setupDetailsButtonColumn() {
        Callback<TableColumn<Message, Void>, TableCell<Message, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Message, Void> call(final TableColumn<Message, Void> param) {
                final TableCell<Message, Void> cell = new TableCell<>() {
                    private final Button btn = new Button("Dettagli");

                    {
                        btn.setOnAction(event -> {
                            Message message = getTableView().getItems().get(getIndex());
                            showMessageDetail(message);
                        });

                        btn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        detailsButtonColumn.setCellFactory(cellFactory);
        detailsButtonColumn.setPrefWidth(100);
        detailsButtonColumn.setResizable(false);
    }

    private List<Message> loadMessagesFromDatabase() throws Exception {
        List<Message> messages = new ArrayList<>();
        System.out.println("Loading messages for user: " + Session.username);

        try (Socket socket = new Socket("videoteca-server", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("15");
            out.println(Session.username);

            Message currentMessage = null;
            String line;
            int messageCount = 0;

            while ((line = in.readLine()) != null && !line.equals("END")) {
                System.out.println("Received line: " + line);

                if (line.startsWith("TITLE:")) {
                    // Starta un nuovo messaggio
                    currentMessage = new Message();
                    currentMessage.setTitle(line.substring(6));
                    System.out.println("New message started with title: " + currentMessage.getTitle());
                } else if (line.startsWith("SENDER:") && currentMessage != null) {
                    currentMessage.setSender(line.substring(7));
                } else if (line.startsWith("USER:") && currentMessage != null) {
                    currentMessage.setUser(line.substring(5));
                } else if (line.startsWith("TEXT:") && currentMessage != null) {
                    currentMessage.setContent(line.substring(5));
                } else if (line.startsWith("EXPIRE:") && currentMessage != null) {
                    currentMessage.setExpireDate(line.substring(7));

                    messages.add(currentMessage);
                    messageCount++;
                    System.out.println("Added complete message: " + currentMessage);

                    // Resetta per il prossimo messaggio
                    currentMessage = null;
                }
            }
            System.out.println("Finished loading, total messages: " + messageCount);
        } catch (IOException e) {
            System.err.println("Network error loading messages: " + e.getMessage());
            throw e;
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