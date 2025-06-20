package com.videoteca;

public class CartItem {
    private int movieId;
    private String title;

    public CartItem(int movieId, String title) {
        this.movieId = movieId;
        this.title = title;
    }

    public int getMovieId() {
        return movieId;
    }

    public String getTitle() {
        return title;
    }
}
