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
    private TableColumn<Movie, Integer> copiesColumn;
    @FXML
    private TableColumn<Movie, Void> actionColumn;

    private ObservableList<Movie> movieList = FXCollections.observableArrayList();

    private String currentUsername;

    private Socket socket;
    private DataOutputStream outputStream;

    @FXML
    private void initialize() throws SQLException {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));
        copiesColumn.setCellValueFactory(new PropertyValueFactory<>("copies"));

        loadMoviesFromDatabase();

        addButtonToTable();
    }

    private void addToCart(int id){
        try {
            Socket socket = new Socket("localhost", 8080);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //Mando al server il comando per aggiungere al carrello
            out.println("3");
            out.println(Session.username);
            out.println(id);

            //Ricevo la risposta dal server
            String response = in.readLine();
            System.out.println(response);
            //ViewCartController v = new ViewCartController();
            //v.loadCartItems();
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
        // Costruisce un percorso relativo alla directory corrente
        String dbRelativePath = "server/videoteca.db";

        // Costruisce il percorso assoluto indipendente dal sistema operativo
        String absolutePath = Paths.get(dbRelativePath).toAbsolutePath().toString();

        // Costruisce l'URL JDBC per SQLite
        String url = "jdbc:sqlite:" + absolutePath;
            
        String sql = "SELECT id, title, genre, duration, availableCopies FROM movies";

        try(Connection conn = DriverManager.getConnection(url);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
            while(rs.next()) {
                movieList.add(new Movie(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("genre"),
                    rs.getInt("duration"),
                    rs.getInt("availableCopies")
                ));
            }
            movieTable.setItems(movieList);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}