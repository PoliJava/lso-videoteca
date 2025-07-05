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

public class ViewMoviesAdminController {

    @FXML
    private TableView<Movie> movieTable;
    @FXML
    private TableColumn<Movie, String> titleColumn;
    @FXML
    private TableColumn<Movie, Integer> copiesColumn;
    @FXML
    private TableColumn<Movie, Integer> rentedColumn;
    private ObservableList<Movie> movieList = FXCollections.observableArrayList();

    @FXML
    private void initialize() throws SQLException {
    try (Socket socket = new Socket("localhost", 8080);
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