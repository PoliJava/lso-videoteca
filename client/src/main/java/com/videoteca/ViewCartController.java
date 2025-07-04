package com.videoteca;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class ViewCartController {

    @FXML
    private TableView<Movie> cartTableView;

    @FXML
    private TableColumn<Movie, String> titleColumn;

    @FXML
    private TableColumn<Movie, Void> actionColumn;

    @FXML 
    private Button checkoutButton;
    
    private ObservableList<Movie> movieList = FXCollections.observableArrayList();

    private String username;



    public void setUsername(String logName) {
        this.username = logName;
        System.out.println("setUsername chiamato!");
        loadCartItems(); // Carica i dati del carrello quando viene impostato l'username
    }

    private void addButtonToTable() {
    actionColumn.setCellFactory(col -> new TableCell<Movie, Void>() {
        private final Button deleteButton = new Button("Elimina");

        {
            deleteButton.setOnAction(event -> {
                Movie selectedMovie = getTableView().getItems().get(getIndex());
            //    System.out.println("Raggiunta cancellazione: cancelleremo il film" + selectedMovie.getTitle() + "dall'id " + selectedMovie.getId());
                deleteFromCart(selectedMovie.getId());
                
                cartTableView.getItems().remove(selectedMovie);
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                setGraphic(deleteButton);
            }
        }
    });

    checkoutButton.setOnAction(event ->{ //fa in modo che il checkout button salvi la lista degli id dei film da aggiungere al DB 
        ArrayList<Integer> allMovies = new ArrayList<>();
        int nrows = 0;

        for(Movie movie : movieList){
            allMovies.add(movie.getId());
            }

        nrows = allMovies.size();
        System.out.println(nrows);
        
        try {
            handleCheckout(allMovies, nrows);
        } catch (IOException e) {
            e.printStackTrace();
        }
    
    });
}

    @FXML
    public void initialize() {
        // Collega la colonna al metodo getTitle() della classe Movie
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        addButtonToTable(); //mette il pulsante
         // Aggiungi un pulsante "Elimina" per ogni riga
    /*actionColumn.setCellFactory(col -> {
        return new TableCell<Movie, Void>() {
            private final Button deleteButton = new Button("Elimina");
    }
    ;*/

   if (this.username != null) {
    loadCartItems();
} else {
    System.err.println("Username non disponibile.");
}

    }

    void loadCartItems() {
        if (username == null) {
            System.out.println("Username is null");
            return;
        }
        

         try {
        Socket socket = new Socket("localhost", 8080);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Invia la scelta 5 e l'username
        out.println("5");
        out.println(username);

        movieList.clear();

        String line;
        while ((line = in.readLine()) != null) {
            if (line.equals("END_OF_CART")) {
                break;
            }

            // Parsing: id|title|genre|duration|availableCopies
            String[] parts = line.split("\\|");
            if (parts.length == 5) {
                int id = Integer.parseInt(parts[0]);
                String title = parts[1];
                String genre = parts[2];
                int duration = Integer.parseInt(parts[3]);
                int availableCopies = Integer.parseInt(parts[4]);
                

                // Costruisci il Movie con solo i dati necessari
                Movie movie = new Movie();
                movie.setTitle(title);
                movie.setId(id);
                movie.setCopies(availableCopies);
                movieList.add(movie);
            } else {
                System.out.println("Riga malformata: " + line);
            }
        }

        cartTableView.setItems(movieList);
        socket.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
    }

    private void deleteFromCart(int id){
     try {
        Socket socket = new Socket("localhost", 8080);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("4");
        out.println(Session.username);
        out.println(id);

        String response = in.readLine();
        System.out.println(response);
        
        if (!response.contains("successo")) {
            // Mostra un messaggio di errore all'utente
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Errore");
                alert.setHeaderText("Impossibile rimuovere il film");
                alert.setContentText("Si è verificato un errore durante la rimozione dal carrello.");
                alert.showAndWait();
            });
        }

        socket.close();
    } catch (Exception e) {
        e.printStackTrace();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore di connessione");
            alert.setHeaderText("Connessione al server fallita");
            alert.setContentText("Impossibile connettersi al server: " + e.getMessage());
            alert.showAndWait();
        });
    }
    }

    @FXML
    private void handleCheckout(ArrayList<Integer> toRent, int rows) throws UnknownHostException, IOException {
    System.out.println("Checkout cliccato!");

        Socket socket = new Socket("localhost", 8080);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.write("6");
        out.newLine();
        out.flush();

        out.write(Session.username);
        out.newLine();
        out.flush();

        out.write(String.valueOf(rows));
        out.newLine();
        out.flush();

for (Integer id : toRent) {
    out.write(String.valueOf(id));
    out.newLine();
    out.flush();
}

        String response = in.readLine();
        System.out.println(response);

        if (!"SUCCESS".equals(response)) {
            // Mostra un messaggio di errore all'utente
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Errore");
                alert.setHeaderText("Impossibile noleggiare il film");
                alert.setContentText("Le copie sono esaurite!");
                alert.showAndWait();
            });
        }
        else{   movieList.clear();  }
        
}

}