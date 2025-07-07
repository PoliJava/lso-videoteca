package com.videoteca;

public class Movie {
    private int id;
    private String title;
    private String genre;
    private int duration;
    private int copies;
    private int totalCopies;

    Movie(int id, String title, String genre, int duration, int copies) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.duration = duration;
        this.copies = copies;
    }

    Movie(int id, String title, String genre, int duration, int copies, int totalCopies) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.duration = duration;
        this.copies = copies;
        this.totalCopies = totalCopies;
    }

    Movie(int id, String title, int copies, int totalCopies) {
        this.id = id;
        this.title = title;
        this.copies = copies;
        this.totalCopies = totalCopies;
    }

    public Movie() {
        this.id = 0;
        this.title = "";
        this.genre = "";
        this.duration = 0;
        this.copies = 0;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public int getDuration() {
        return duration;
    }

    public int getCopies() {
        return copies;
    }

    public void setTitle(String newTitle) {
        this.title = newTitle;
    }

    public void setDuration(int len) {
        this.duration = len;
    }

    public void setCopies(int availableCopies) {
        this.copies = availableCopies;
    }

    public void setId(int movieId) {
        this.id = movieId;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getRentedCopies() {
        return totalCopies - copies;
    }

}