package com.trien.widget

class Track(var name: String, var artist: String, var imageUrl: String, var isPlaying: Boolean) {

    override fun toString(): String {
        return "Track{" +
                "name='" + name + '\''.toString() +
                ", artist='" + artist + '\''.toString() +
                ", imageUrl='" + imageUrl + '\''.toString() +
                ", playing=" + isPlaying +
                '}'.toString()
    }
}
