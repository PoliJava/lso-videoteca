package com.videoteca;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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

    public RentalMovie() {
    // // cosa ci mettiamo qui?
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

    //controlla se la data di scadenza del prestito è arrivata
    
    public boolean isExpired() {
        if (this.expirationDate == null || this.expirationDate.isEmpty()) {
            return false; // Cannot determine if expired without a return date
        }
        try {
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate dueDate = LocalDate.parse(this.expirationDate, formatter);
            return today.isAfter(dueDate);
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing date: " + this.expirationDate + ". " + e.getMessage());
            return false; // Treat as not expired if date is unparseable
        }
    }

}
