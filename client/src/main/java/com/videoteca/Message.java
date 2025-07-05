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
            this.admin = "";
            this.user = "";
            this.title = "";
            this.expireDate = "";
            this.content = "";
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

    public void setSender(String substring) {
        this.admin = substring;
    }

        public void setExpireDate(String substring) {
        this.expireDate = substring;
    }

    @Override
    public String toString() {
     return "Message{" +
            "title='" + title + '\'' +
            ", sender='" + admin + '\'' +
            ", user='" + user + '\'' +
            ", content='" + content + '\'' +
            '}';
}


}
