package com.videoteca;

public class Message {
    private String admin;
    private String user;
    private String title;
    private String expireDate;
    private int movieId;
    private String content;

    public Message(String admin, String user, String title, String expireDate, int movieId, String content) {
        this.admin = admin;
        this.user = user;
        this.title = title;
        this.expireDate = expireDate;
        this.movieId = movieId;
        this.content = content;
    }

    public Message() {
        //TODO Auto-generated constructor stub
    }

    public String getAdmin() {
        return admin;
    }

    public String getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public int getMovieId() {
        return movieId;
    }

    public String getContent() {
        return content;
    }

    public void setTitle(String substring) {
        this.title = substring;
    }

    public void setUser(String substring) {
       this.user = substring;
    }

    public void setContent(String substring) {
        this.content = substring;
    }
}
