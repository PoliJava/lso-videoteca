package com.videoteca;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;

import javafx.util.Callback;
import javafx.scene.control.TableCell;

public class ViewRentalsController {
    private ObservableList<RentalMovie> rentalMovies = FXCollections.observableArrayList();

    @FXML
    private TableView<RentalMovie> rentalTableView;
    @FXML
    private TableColumn<RentalMovie, String> titleColumn;
    @FXML
    private TableColumn<RentalMovie, String> rentalDate;
    @FXML
    private TableColumn<RentalMovie, Integer> returnDate;
    @FXML
    private TableColumn<RentalMovie, Void> actionColumn;

    @FXML
    private void initialize() throws SQLException {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        rentalDate.setCellValueFactory(new PropertyValueFactory<>("rentalDate"));
        returnDate.setCellValueFactory(new PropertyValueFactory<>("expirationDate"));

        setButton();
    }

    private void returnMovieToServer(RentalMovie selectedMovie) {
    try (Socket socket = new Socket("videoteca-server", 8080);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

        // Send command
        out.println("8");
        out.println(Session.username);
        out.println(selectedMovie.getId());
        
        // Wait for response
        String response = in.readLine();
        System.out.println("Server response: " + response);
        
        if (!"SUCCESS".equals(response)) {
            System.err.println("Failed to return movie: " + response);
        }
        
    } catch (Exception e) {
        e.printStackTrace();
        // Show error to user
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Return Failed");
        alert.setContentText("Could not return movie: " + e.getMessage());
        alert.showAndWait();
    }
    
    // Refresh the table
    loadRentItems();
}

    private void setButton() {
        Callback<TableColumn<RentalMovie, Void>, TableCell<RentalMovie, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<RentalMovie, Void> call(final TableColumn<RentalMovie, Void> param) {
                return new TableCell<>() {

                    private final Button btn = new Button("Restituisci");

                    {
                        btn.setOnAction(event -> {
                            RentalMovie selectedMovie = getTableView().getItems().get(getIndex());
                            // Invia messaggio al server per restituire il film
                            returnMovieToServer(selectedMovie);
                            // Rimuovi il film dalla tabella dopo la restituzione
                            rentalMovies.remove(selectedMovie);
                            rentalTableView.setItems(rentalMovies);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        };

        actionColumn.setCellFactory(cellFactory);
        rentalTableView.setItems(rentalMovies); // Associa i dati alla tabella
    }

    void loadRentItems() {
        if (Session.username == null) {
            System.out.println("Username is null");
            return;
        }

        try {
            Socket socket = new Socket("videoteca-server", 8080);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Invia la scelta 7 e l'username
            out.println("7");
            out.println(Session.username);

            rentalMovies.clear();

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("END_OF_CART")) {
                    break;
                }

                System.out.println("Riga ricevuta: " + line);
                // Parsing: id|title|genre|duration|availableCopies|rentalDate|expirationDate
                String[] parts = line.split("\\|");
                if (parts.length == 6) {
                    int id = Integer.parseInt(parts[0]);
                    String title = parts[1];
                    String genre = parts[2];
                    int duration = Integer.parseInt(parts[3]);
                    String rentalDate = parts[4];
                    String expirationDate = parts[5];

                    System.out.println(rentalDate);
                    System.out.println(expirationDate);

                    // Costruisci il Movie con solo i dati necessari
                    RentalMovie newRentalMovie = new RentalMovie(id, title, genre, duration, rentalDate,
                            expirationDate);
                    rentalMovies.add(newRentalMovie);
                } else {
                    System.out.println("Riga malformata: " + line);
                }
            }

            rentalTableView.setItems(rentalMovies);
            socket.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
