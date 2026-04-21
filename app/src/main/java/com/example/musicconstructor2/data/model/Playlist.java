package com.example.musicconstructor2.data.model;
import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String id;
    private String title;
    private String description;
    private String coverUrl;
    private long   createdAt;
    private String ownerId;
    private int    trackCount;
    private List<Track> tracks;

    public Playlist() {
        this.tracks = new ArrayList<>();
    }

    public Playlist(String title, String description, String ownerId) {
        this.title       = title;
        this.description = description;
        this.ownerId     = ownerId;
        this.createdAt   = System.currentTimeMillis();
        this.tracks      = new ArrayList<>();
        this.trackCount  = 0;
    }

    // Геттеры
    public String      getId()          { return id; }
    public String      getTitle()       { return title; }
    public String      getDescription() { return description; }
    public String      getCoverUrl()    { return coverUrl; }
    public long        getCreatedAt()   { return createdAt; }
    public String      getOwnerId()     { return ownerId; }
    public int         getTrackCount()  { return trackCount; }
    public List<Track> getTracks()      { return tracks; }

    // Сеттеры
    public void setId(String id)               { this.id = id; }
    public void setTitle(String title)         { this.title = title; }
    public void setDescription(String desc)    { this.description = desc; }
    public void setCoverUrl(String url)        { this.coverUrl = url; }
    public void setCreatedAt(long createdAt)   { this.createdAt = createdAt; }
    public void setOwnerId(String ownerId)     { this.ownerId = ownerId; }
    public void setTrackCount(int trackCount)  { this.trackCount = trackCount; }
    public void setTracks(List<Track> tracks)  { this.tracks = tracks; }
}