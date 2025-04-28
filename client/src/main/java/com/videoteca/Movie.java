package com.videoteca;

public class Movie {
    private String title;
    private String genre;
    private int duration;
    private int copies;


    Movie(String title, String genre, int duration, int copies) {
        this.title = title;
        this.genre = genre;
        this.duration = duration;
        this.copies = copies;
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