package com.videoteca;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;

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

    private ObservableList<Movie> movieList = FXCollections.observableArrayList();

    @FXML
    private void initialize() throws SQLException {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));
        copiesColumn.setCellValueFactory(new PropertyValueFactory<>("copies"));

        loadMoviesFromDatabase();

        
    }

    private void loadMoviesFromDatabase() throws SQLException {
        String url = "jdbc:sqlite:/Users/simone/Documents/Universita/Github Reps/Videoteca/server/videoteca.db"; //da rendere universale

        String sql = "SELECT title, genre, duration, availableCopies FROM movies";

        try(Connection conn = DriverManager.getConnection(url);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
            while(rs.next()) {
                movieList.add(new Movie(
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