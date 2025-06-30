package com.videoteca;

public class RentalMovie {
    private int id;
    private String title;
    private String genre;
    private int duration;
    private String rentalDate;
    private String expirationDate;

    RentalMovie(int id, String title, String genre, int duration, String rentalDate, String expirationDate) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.duration = duration;
        this.rentalDate = rentalDate;
        this.expirationDate = expirationDate;
    }

    // public RentalMovie() {
    // // cosa ci mettiamo qui?
    // }

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public int getDuration() {
        return duration;
    }

    public int getId() {
        return id;
    }

    public String getRentalDate() {
        return rentalDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setRentalDate(String x) {
        this.rentalDate = x;
    }

    public void setExpirationDate(String x) {
        this.expirationDate = x;
    }

}
