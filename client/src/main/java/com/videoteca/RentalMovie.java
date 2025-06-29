package com.videoteca;

import java.time.LocalDate;

public class RentalMovie {
    private Movie movie;
    private LocalDate rentalDate;
    private LocalDate expirationDate;

    public RentalMovie(Movie movie, LocalDate rentalDate, LocalDate expirationDate) {
        this.movie = movie;
        this.rentalDate = rentalDate;
        this.expirationDate = expirationDate;
    }

    public RentalMovie() {
        //cosa ci mettiamo qui?
    }

    public Movie getMovie() {
        return movie;
    }

    public String getTitle() {
        return movie.getTitle();
    }

    public String getGenre() {
        return movie.getGenre();
    }

    public int getDuration() {
        return movie.getDuration();
    }

    public int getId() {
        return movie.getId();
    }

    public LocalDate getRentalDate() {
        return rentalDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setRentalDate(LocalDate x){
        this.rentalDate = x;
    }

    public void setExpirationDate(LocalDate x){
        this.expirationDate = x;
    }



}
