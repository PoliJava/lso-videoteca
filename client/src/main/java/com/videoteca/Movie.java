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

    public Movie() {
        //TODO Auto-generated constructor stub
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

    public void setTitle(String newTitle){
        this.title = newTitle;
    }

    public void setDuration(int len){
        this.duration = len;
    }

    public void setCopies(int availableCopies){
        this.copies = availableCopies;
    }

    

}