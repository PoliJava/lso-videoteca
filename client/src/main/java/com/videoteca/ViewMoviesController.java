package com.videoteca;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;

import javafx.util.Callback;
import javafx.scene.control.TableCell;

public class ViewMoviesController {

    @FXML
    private TableView<Movie> movieTable;
    @FXML
    private TableColumn<Movie, String> titleColumn;
    @FXML
    private TableColumn<Movie, String> genreColumn;
    @FXML
    private TableColumn<Movie, Integer> durationColumn;
    @FXML
    private TableColumn<Movie, Void> actionColumn;

    private ObservableList<Movie> movieList = FXCollections.observableArrayList();

    @FXML
    private void initialize() throws SQLException {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));

        loadMoviesFromDatabase();

        addButtonToTable();
    }

    private void addToCart(int id) {
        try {
            Socket socket = new Socket("videoteca-server", 8080);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Mando al server il comando per aggiungere al carrello
            out.println("3");
            out.println(Session.username);
            out.println(id);

            // Ricevo la risposta dal server
            String response = in.readLine();
            System.out.println(response);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addButtonToTable() {
        Callback<TableColumn<Movie, Void>, TableCell<Movie, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Movie, Void> call(final TableColumn<Movie, Void> param) {
                final TableCell<Movie, Void> cell = new TableCell<>() {

                    private final Button btn = new Button("Aggiungi al carrello");

                    {
                        btn.setOnAction(event -> {
                            Movie movie = getTableView().getItems().get(getIndex());
                            addToCart(movie.getId());
                        });
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

        actionColumn.setCellFactory(cellFactory);
    }

    private void loadMoviesFromDatabase() throws SQLException {
    try (Socket socket = new Socket("videoteca-server", 8080);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

        // Request movies (scelta = 10)
        out.println("10");

        // Read response
        String line;
        while ((line = in.readLine()) != null && !line.equals("END_OF_DATA")) {
            String[] parts = line.split("\\|");
            if (parts.length >= 6) {
                movieList.add(new Movie(
                    Integer.parseInt(parts[0]),  // id
                    parts[1],                   // title
                    parts[2],                   // genre
                    Integer.parseInt(parts[3]),  // duration
                    Integer.parseInt(parts[4]),  // availableCopies
                    Integer.parseInt(parts[5])   // totalCopies
                ));
            }
        }

        // Update TableView on JavaFX thread
        Platform.runLater(() -> movieTable.setItems(movieList));

    } catch (IOException e) {
        e.printStackTrace();
        Platform.runLater(() -> 
            new Alert(Alert.AlertType.ERROR, "Server connection failed: " + e.getMessage()).show());
    }
    }
}