package com.videoteca;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Rental{
    private int movieId;
    private String title;
    private String username;
    private String rentalDate;
    private String expirationDate;
    

    Rental(int id, String title, String name, String date, String expiration){
        this.movieId = id;
        this.title = title;
        this.username = name;
        this.rentalDate = date;
        this.expirationDate = expiration;
    }


    public Rental() {
        //TODO Auto-generated constructor stub
    }


    public int getMovieId(){
        return this.movieId;
    }

    public void setMovieId(int id){
        this.movieId = id;
    }

    public String getTitle(){
        return this.title;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public String getUsername(){
        return this.username;
    }

    public void setUsername(String user){
        this.username = user;
    }

    public String getRentalDate(){
        return this.rentalDate;
    }

    public void setRentalDate(String rent){
        this.rentalDate = rent;
    }

    public String getExpirationDate(){
        return this.expirationDate;
    }

    public void setExpirationDate(String exp){
        this.expirationDate = exp;      
    }

        
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
