package com.example.musicconstructor2.data.model;

public class Thumb {
    private int    width;
    private int    height;
    private String id;
    private String photo34;
    private String photo68;
    private String photo135;
    private String photo270;
    private String photo300;
    private String photo600;
    private String photo1200;

    public int    getWidth()     { return width; }
    public int    getHeight()    { return height; }
    public String getId()        { return id; }
    public String getPhoto34()   { return photo34; }
    public String getPhoto68()   { return photo68; }
    public String getPhoto135()  { return photo135; }
    public String getPhoto270()  { return photo270; }
    public String getPhoto300()  { return photo300; }
    public String getPhoto600()  { return photo600; }
    public String getPhoto1200() { return photo1200; }

    public void setWidth(int width)        { this.width = width; }
    public void setHeight(int height)      { this.height = height; }
    public void setId(String id)           { this.id = id; }
    public void setPhoto34(String p)       { this.photo34 = p; }
    public void setPhoto68(String p)       { this.photo68 = p; }
    public void setPhoto135(String p)      { this.photo135 = p; }
    public void setPhoto270(String p)      { this.photo270 = p; }
    public void setPhoto300(String p)      { this.photo300 = p; }
    public void setPhoto600(String p)      { this.photo600 = p; }
    public void setPhoto1200(String p)     { this.photo1200 = p; }

    // Получить лучшее качество обложки
    public String getBestQuality() {
        if (photo300  != null) return photo300;
        if (photo270  != null) return photo270;
        if (photo135  != null) return photo135;
        if (photo600  != null) return photo600;
        if (photo1200 != null) return photo1200;
        return "";
    }
}