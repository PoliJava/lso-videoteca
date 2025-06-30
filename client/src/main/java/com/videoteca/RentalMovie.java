package com.videoteca;



public class RentalMovie {
    private Movie movie;
    private String rentalDate;
    private String expirationDate;

    public RentalMovie(Movie movie, String rentalDate, String expirationDate) {
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

    public String getRentalDate() {
        return rentalDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setRentalDate(String x){
        this.rentalDate = x;
    }

    public void setExpirationDate(String x){
        this.expirationDate = x;
    }



}
