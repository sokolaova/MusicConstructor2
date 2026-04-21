package com.example.musicconstructor2.data.model;

public enum SortMode {
    CUSTOM("Личная расстановка"),
    USER_RATING("По моей оценке"),
    AVERAGE_RATING("По среднему рейтингу");

    private final String displayName;

    SortMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}