package com.example.musicconstructor2.data.model;

import java.util.List;

public class SongsResponseData {
    private int          count;
    private List<VkSong> items;

    public int          getCount() { return count; }
    public List<VkSong> getItems() { return items; }

    public void setCount(int count)          { this.count = count; }
    public void setItems(List<VkSong> items) { this.items = items; }
}