package com.videoteca;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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
        rentalDate.setCellValueFactory(new PropertyValueFactory<>("genre"));
        returnDate.setCellValueFactory(new PropertyValueFactory<>("duration"));

        setButton();
    }

    private void returnMovieToServer(RentalMovie selectedMovie) {
        try (Socket socket = new Socket("localhost", 8080);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Invia comando al server
            out.println("8");
            System.out.println("8");
            out.println(Session.username);
            System.out.println(Session.username);
            out.println(selectedMovie.getId()); // supponendo che Movie abbia un ID
            System.out.println(selectedMovie.getId());
            // Puoi anche inviare il titolo o altri dati se necessario
            String response = in.readLine();
            System.out.println("Risposta dal server: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
        int idx = rentalMovies.indexOf(selectedMovie);
        rentalMovies.remove(idx);
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
            Socket socket = new Socket("localhost", 8080);
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
