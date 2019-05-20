package com.trien.widget;

public class Track {

    String name;
    String artist;
    String imageUrl;
    boolean playing;

    public Track(String name, String artist, String imageUrl, boolean playing) {
        this.name = name;
        this.artist = artist;
        this.imageUrl = imageUrl;
        this.playing = playing;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    @Override
    public String toString() {
        return "Track{" +
                "name='" + name + '\'' +
                ", artist='" + artist + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", playing=" + playing +
                '}';
    }
}
