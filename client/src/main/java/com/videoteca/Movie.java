package com.videoteca;

public class Movie {
    private int id;
    private String title;
    private String genre;
    private int duration;
    private int copies;

    
    Movie(int id, String title, String genre, int duration, int copies) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.duration = duration;
        this.copies = copies;
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

    

}