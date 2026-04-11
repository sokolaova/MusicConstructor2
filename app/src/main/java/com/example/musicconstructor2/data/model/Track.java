package com.example.musicconstructor2.data.model;
public class Track {
    private String id;
    private String artist;
    private String title;
    private String url;        // ссылка на mp3
    private String coverUrl;   // обложка альбома
    private int duration;      // в секундах
    private int position;      // позиция в топе

    public Track() {} // нужен для Firestore

    public Track(String id, String artist, String title,
                 String url, String coverUrl, int duration) {
        this.id       = id;
        this.artist   = artist;
        this.title    = title;
        this.url      = url;
        this.coverUrl = coverUrl;
        this.duration = duration;
    }

    // Геттеры и сеттеры
    public String getId()           { return id; }
    public String getArtist()       { return artist; }
    public String getTitle()        { return title; }
    public String getUrl()          { return url; }
    public String getCoverUrl()     { return coverUrl; }
    public int    getDuration()     { return duration; }
    public int    getPosition()     { return position; }

    public void setId(String id)            { this.id = id; }
    public void setArtist(String artist)    { this.artist = artist; }
    public void setTitle(String title)      { this.title = title; }
    public void setUrl(String url)          { this.url = url; }
    public void setCoverUrl(String url)     { this.coverUrl = url; }
    public void setDuration(int duration)   { this.duration = duration; }
    public void setPosition(int position)   { this.position = position; }

    // Форматирование времени — "3:45"
    public String getFormattedDuration() {
        int min = duration / 60;
        int sec = duration % 60;
        return String.format("%d:%02d", min, sec);
    }
}