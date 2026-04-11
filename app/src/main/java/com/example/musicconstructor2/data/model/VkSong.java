package com.example.musicconstructor2.data.model;

public class VkSong {
    private String artist;
    private long   id;
    private long   ownerId;
    private String title;
    private int    duration;
    private String accessKey;
    private Boolean isExplicit;
    private String trackCode;
    private String url;
    private Album  album;
    private String subtitle;

    public String  getArtist()     { return artist; }
    public long    getId()         { return id; }
    public long    getOwnerId()    { return ownerId; }
    public String  getTitle()      { return title; }
    public int     getDuration()   { return duration; }
    public String  getAccessKey()  { return accessKey; }
    public Boolean isExplicit()    { return isExplicit; }
    public String  getTrackCode()  { return trackCode; }
    public String  getUrl()        { return url; }
    public Album   getAlbum()      { return album; }
    public String  getSubtitle()   { return subtitle; }

    public void setArtist(String artist)       { this.artist = artist; }
    public void setId(long id)                 { this.id = id; }
    public void setOwnerId(long ownerId)       { this.ownerId = ownerId; }
    public void setTitle(String title)         { this.title = title; }
    public void setDuration(int duration)      { this.duration = duration; }
    public void setAccessKey(String key)       { this.accessKey = key; }
    public void setExplicit(Boolean explicit)  { this.isExplicit = explicit; }
    public void setTrackCode(String code)      { this.trackCode = code; }
    public void setUrl(String url)             { this.url = url; }
    public void setAlbum(Album album)          { this.album = album; }
    public void setSubtitle(String subtitle)   { this.subtitle = subtitle; }

    // Конвертация в твой Track для адаптера
    public Track toTrack(int position) {
        String coverUrl = "";
        if (album != null && album.getThumb() != null) {
            coverUrl = album.getThumb().getPhoto300() != null
                    ? album.getThumb().getPhoto300() : "";
        }
        Track track = new Track(
                String.valueOf(id), artist, title, url, coverUrl, duration
        );
        track.setPosition(position);
        return track;
    }
}