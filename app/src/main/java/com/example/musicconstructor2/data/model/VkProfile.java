package com.example.musicconstructor2.data.model;

public class VkProfile {
    private long   id;
    private String firstName;
    private String lastName;
    private String photo;

    public VkProfile() {}

    public long   getId()        { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName; }
    public String getPhoto()     { return photo; }

    public void setId(long id)              { this.id = id; }
    public void setFirstName(String name)   { this.firstName = name; }
    public void setLastName(String name)    { this.lastName = name; }
    public void setPhoto(String photo)      { this.photo = photo; }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}