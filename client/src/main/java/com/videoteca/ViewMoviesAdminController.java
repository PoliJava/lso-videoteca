package com.videoteca;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.BufferedReader;
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
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        copiesColumn.setCellValueFactory(new PropertyValueFactory<>("copies"));
        rentedColumn.setCellValueFactory(new PropertyValueFactory<>("rents"));

        loadMoviesFromDatabase();
    }

    private void loadMoviesFromDatabase() throws SQLException {
        String url = "jdbc:sqlite:" + System.getProperty("user.dir") + "/server/videoteca.db";
       

        String sql = "SELECT id, title, totalCopies, availableCopies FROM movies";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                movieList.add(new Movie(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getInt("copies"),
                        rs.getInt("rentedCopies")));
            }
            movieTable.setItems(movieList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}