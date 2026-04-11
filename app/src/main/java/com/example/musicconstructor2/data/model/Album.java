package com.example.musicconstructor2.data.model;

public class Album {
    private int    id;
    private String title;
    private int    ownerId;
    private String accessKey;
    private Thumb  thumb;
    private String mainColor;

    public int    getId()         { return id; }
    public String getTitle()      { return title; }
    public int    getOwnerId()    { return ownerId; }
    public String getAccessKey()  { return accessKey; }
    public Thumb  getThumb()      { return thumb; }
    public String getMainColor()  { return mainColor; }

    public void setId(int id)              { this.id = id; }
    public void setTitle(String title)     { this.title = title; }
    public void setOwnerId(int ownerId)    { this.ownerId = ownerId; }
    public void setAccessKey(String key)   { this.accessKey = key; }
    public void setThumb(Thumb thumb)      { this.thumb = thumb; }
    public void setMainColor(String color) { this.mainColor = color; }
}