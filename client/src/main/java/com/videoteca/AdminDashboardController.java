package com.videoteca;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController {

    @FXML
    private ImageView avatarImage;
    
    @FXML
    private Label welcomeLabel;
    
    @FXML
    private StackPane contentPane;
    
    @FXML
    private Button viewMoviesButton;
    
    @FXML
    private Button viewRentedMoviesButton;
    
    @FXML
    private Button viewNotificationsButton;

    // Initialize method is automatically called after FXML loading
    @FXML
    private void initialize() {
        welcomeLabel.setText("Benvenuto, Amministratore");
        handleViewMoviesAdmin(); // Load movies by default
    }
    
    @FXML
    private void handleViewMoviesAdmin() {
        try {
            List<Movie> movies = fetchMoviesFromServer();
            TableView<Movie> tableView = new TableView<>();
            
            // Create and configure columns
            TableColumn<Movie, Integer> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            
            TableColumn<Movie, String> titleCol = new TableColumn<>("Title");
            titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
            
            TableColumn<Movie, String> genreCol = new TableColumn<>("Genre");
            genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));
            
            TableColumn<Movie, Integer> durationCol = new TableColumn<>("Duration (min)");
            durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));
            
            TableColumn<Movie, Integer> totalCol = new TableColumn<>("Total Copies");
            totalCol.setCellValueFactory(new PropertyValueFactory<>("totalCopies"));
            
            TableColumn<Movie, Integer> copiesCol = new TableColumn<>("Available");
            copiesCol.setCellValueFactory(new PropertyValueFactory<>("copies"));
            
            TableColumn<Movie, Integer> rentedCol = new TableColumn<>("Rented");
            rentedCol.setCellValueFactory(new PropertyValueFactory<>("rentedCopies"));
            
            // Add columns in logical order
            tableView.getColumns().addAll(idCol, titleCol, genreCol, durationCol, 
                                        totalCol, copiesCol, rentedCol);
            
            // Set data with ACTUAL movies list
            tableView.setItems(FXCollections.observableArrayList(movies));
            tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            
            contentPane.getChildren().clear();
            contentPane.getChildren().add(tableView);
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading movies");
        }
    }
    
    @FXML
    private void handleViewRentedMoviesAdmin() {
        try {
            List<Rental> rentals = fetchRentalsFromServer();
            TableView<Rental> tableView = new TableView<>();
            
            // Create and configure columns
            TableColumn<Rental, Integer> movieIdCol = new TableColumn<>("Movie ID");
            movieIdCol.setCellValueFactory(new PropertyValueFactory<>("movieId"));
            
            TableColumn<Rental, String> usernameCol = new TableColumn<>("Username");
            usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
            
            TableColumn<Rental, String> rentalDateCol = new TableColumn<>("Rental Date");
            rentalDateCol.setCellValueFactory(new PropertyValueFactory<>("rentalDate"));
            
            TableColumn<Rental, String> expDateCol = new TableColumn<>("Expiration Date");
            expDateCol.setCellValueFactory(new PropertyValueFactory<>("expirationDate"));
            
            tableView.getColumns().addAll(movieIdCol, usernameCol, rentalDateCol, expDateCol);
            
            // Set data with ACTUAL rentals list
            tableView.setItems(FXCollections.observableArrayList(rentals));
            tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            
            contentPane.getChildren().clear();
            contentPane.getChildren().add(tableView);
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading rentals");
        }
    }

    private void showError(String message) {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(new Label(message));
    }

    
    @FXML
    private void handleViewNotifications() {
        // Implement notifications view if needed
        contentPane.getChildren().clear();
        contentPane.getChildren().add(new Label("Notifications will be shown here"));
    }
    
   private List<Movie> fetchMoviesFromServer() throws Exception {
    List<Movie> movies = new ArrayList<>();
    
    try (Socket socket = new Socket("localhost", 8080);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
        
        // Send request type (10 for movies)
        out.println("10");
        
        // Read response
        String line;
        while ((line = in.readLine()) != null && !line.equals("END_OF_DATA")) {
            String[] parts = line.split("\\|");
            if (parts.length >= 6) {
                movies.add(new Movie(
                    Integer.parseInt(parts[0]),  // id
                    parts[1],                  // title
                    parts[2],                  // genre
                    Integer.parseInt(parts[3]), // duration
                    Integer.parseInt(parts[4]), // copies
                    Integer.parseInt(parts[5]) // totalCopies
                ));
            }
        }
    }
    return movies;
}

private List<Rental> fetchRentalsFromServer() throws Exception {
    List<Rental> rentals = new ArrayList<>();
    
    try (Socket socket = new Socket("localhost", 8080);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
        
        out.println("11");
        
        int expectedRecords = 0;
        int currentRecord = 0;
        Rental rental = null;
        
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("COUNT:")) {
                expectedRecords = Integer.parseInt(line.substring(6));
                System.out.println("Expecting " + expectedRecords + " records");
            }
            else if (line.startsWith("MOVIEID:")) {
                rental = new Rental();
                rental.setMovieId(Integer.parseInt(line.substring(8)));
            }
            else if (line.startsWith("USERNAME:")) {
                if (rental != null) rental.setUsername(line.substring(9));
            }
            else if (line.startsWith("RENTALDATE:")) {
                if (rental != null) rental.setRentalDate(line.substring(11));
            }
            else if (line.startsWith("RETURNDATE:")) {
                if (rental != null) {
                    rental.setExpirationDate(line.substring(11));
                    rentals.add(rental);
                    currentRecord++;
                }
            }
            else if (line.equals("END")) {
                break;
            }
        }
        
        System.out.println("Received " + currentRecord + " of " + expectedRecords + " records");
    }
    return rentals;
}


}