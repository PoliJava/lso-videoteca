package com.videoteca;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            TableColumn<Rental, String> titleCol = new TableColumn<>("Movie Title");
            titleCol.setCellValueFactory(new PropertyValueFactory<>("title")); 
            
            TableColumn<Rental, String> usernameCol = new TableColumn<>("Username");
            usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
            
            TableColumn<Rental, String> rentalDateCol = new TableColumn<>("Rental Date");
            rentalDateCol.setCellValueFactory(new PropertyValueFactory<>("rentalDate"));
            
            TableColumn<Rental, String> expDateCol = new TableColumn<>("Expiration Date");
            expDateCol.setCellValueFactory(new PropertyValueFactory<>("expirationDate"));
            
            tableView.getColumns().addAll(titleCol, usernameCol, rentalDateCol, expDateCol);
            
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
        while ((line = in.readLine()) != null && !line.equals("END")) {
            if (line.startsWith("MOVIEID:")) {
                rental = new Rental();
                rental.setMovieId(Integer.parseInt(line.substring(8)));
            } else if (line.startsWith("TITLE:")) {
                if (rental != null) rental.setTitle(line.substring(6)); // Set the movie title
            } else if (line.startsWith("USERNAME:")) {
                if (rental != null) rental.setUsername(line.substring(9));
            } else if (line.startsWith("RENTALDATE:")) {
                if (rental != null) rental.setRentalDate(line.substring(11));
            } else if (line.startsWith("RETURNDATE:")) {
                if (rental != null) {
                    rental.setExpirationDate(line.substring(11));
                    rentals.add(rental);
                }
            }
        }
        
        System.out.println("Received " + currentRecord + " of " + expectedRecords + " records");
    }
    return rentals;
}

@FXML
private void handleAddMovie() {
    // Create a dialog for adding a new movie
    Dialog<Movie> dialog = new Dialog<>();
    dialog.setTitle("Add New Movie");
    dialog.setHeaderText("Enter movie details");

    // Set the button types
    ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

    // Create the form
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));

    TextField titleField = new TextField();
    titleField.setPromptText("Title");
    TextField genreField = new TextField();
    genreField.setPromptText("Genre");
    TextField durationField = new TextField();
    durationField.setPromptText("Duration (minutes)");
    TextField copiesField = new TextField();
    copiesField.setPromptText("Total Copies");

    grid.add(new Label("Title:"), 0, 0);
    grid.add(titleField, 1, 0);
    grid.add(new Label("Genre:"), 0, 1);
    grid.add(genreField, 1, 1);
    grid.add(new Label("Duration:"), 0, 2);
    grid.add(durationField, 1, 2);
    grid.add(new Label("Copies:"), 0, 3);
    grid.add(copiesField, 1, 3);

    dialog.getDialogPane().setContent(grid);

    // Convert the result to a Movie object when the Add button is clicked
    dialog.setResultConverter(dialogButton -> {
        if (dialogButton == addButtonType) {
            try {
                return new Movie(
                    0, // ID will be assigned by database
                    titleField.getText(),
                    genreField.getText(),
                    Integer.parseInt(durationField.getText()),
                    0, // Available copies will be set equal to total copies by server
                    Integer.parseInt(copiesField.getText())
                );
            } catch (NumberFormatException e) {
                showError("Invalid number format");
                return null;
            }
        }
        return null;
    });

    Optional<Movie> result = dialog.showAndWait();

    result.ifPresent(movie -> {
        try {
            boolean success = addMovieToServer(movie);
            if (success) {
                showSuccess("Movie added successfully!");
                handleViewMoviesAdmin(); // Refresh the movie list
            } else {
                showError("Failed to add movie");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error adding movie: " + e.getMessage());
        }
    });
}

private boolean addMovieToServer(Movie movie) throws Exception {
    System.out.println("Attempting to add movie: " + movie.getTitle());
    
    try (Socket socket = new Socket("localhost", 8080);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
        
        System.out.println("Sending request type (12)");
        out.println("12");
        
        System.out.println("Sending movie data:");
        System.out.println("Title: " + movie.getTitle());
        System.out.println("Genre: " + movie.getGenre());
        System.out.println("Duration: " + movie.getDuration());
        System.out.println("Copies: " + movie.getTotalCopies());
        
        out.println(movie.getTitle());
        out.println(movie.getGenre());
        out.println(String.valueOf(movie.getDuration()));
        out.println(String.valueOf(movie.getTotalCopies()));
        
        System.out.println("Waiting for response...");
        String response = in.readLine();
        System.out.println("Server response: " + response);
        
            boolean success = "SUCCESS".equals(response);
    if (success) {
        debugCheckMovies(); // Add this line
    }
    return success;
    } catch (Exception e) {
        System.out.println("Error in addMovieToServer: " + e.getMessage());
        e.printStackTrace();
        throw e;
    }
}

private void showSuccess(String message) {
    contentPane.getChildren().clear();
    Label label = new Label(message);
    label.setTextFill(Color.GREEN);
    contentPane.getChildren().add(label);
}

private void debugCheckMovies() {
    try (Socket socket = new Socket("localhost", 8080);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
        
        out.println("10"); // Request movie list
        
        System.out.println("DEBUG - Current movies in database:");
        String line;
        while ((line = in.readLine()) != null && !line.equals("END_OF_DATA")) {
            System.out.println(line);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

}