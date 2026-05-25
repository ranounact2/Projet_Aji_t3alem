package com.example.dkhel_t3alem_raniabadi;

public class NearbyUser {
    private String userId;
    private String username;
    private double latitude;
    private double longitude;
    private int score;
    private float distance;

    public NearbyUser(String userId, String username, double latitude,
                      double longitude, int score, float distance) {
        this.userId = userId;
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
        this.score = score;
        this.distance = distance;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getScore() { return score; }
    public float getDistance() { return distance; }

    public String getFormattedDistance() {
        if (distance < 1000) {
            return String.format("%.0f m", distance);
        } else {
            return String.format("%.1f km", distance / 1000);
        }
    }
}