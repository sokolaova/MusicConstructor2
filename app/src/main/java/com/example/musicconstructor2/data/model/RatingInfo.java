package com.example.musicconstructor2.data.model;

public class RatingInfo {
    private float averageRating;
    private int totalRatings;
    private float userRating;
    private boolean hasUserRated;

    public RatingInfo() {
        this.averageRating = 0f;
        this.totalRatings = 0;
        this.userRating = 0f;
        this.hasUserRated = false;
    }

    public float getAverageRating() { return averageRating; }
    public void setAverageRating(float averageRating) { this.averageRating = averageRating; }

    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }

    public float getUserRating() { return userRating; }
    public void setUserRating(float userRating) { this.userRating = userRating; }

    public boolean hasUserRated() { return hasUserRated; }
    public void setHasUserRated(boolean hasUserRated) { this.hasUserRated = hasUserRated; }

    public String getFormattedAverage() {
        return String.format("%.1f", averageRating);
    }

    public String getRatingText() {
        if (totalRatings == 0) return "Нет оценок";

        String ratingWord;
        int lastDigit = totalRatings % 10;
        int lastTwoDigits = totalRatings % 100;

        if (lastTwoDigits >= 11 && lastTwoDigits <= 19) {
            ratingWord = "оценок";
        } else if (lastDigit == 1) {
            ratingWord = "оценка";
        } else if (lastDigit >= 2 && lastDigit <= 4) {
            ratingWord = "оценки";
        } else {
            ratingWord = "оценок";
        }

        return totalRatings + " " + ratingWord;
    }
}