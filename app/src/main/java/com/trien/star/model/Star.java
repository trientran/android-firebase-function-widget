package com.trien.star.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Star {

    public String starId; // should be ignored
    public Long starsAwarded;
    public String starsGiver ;
    public String starsReasoning;
    public String starsReceiver;
    public String starsReceiverUid;

    public Star() {
        // Default constructor required for calls to DataSnapshot.getValue(Star.class)
    }

    public Star(Long starsAwarded, String starsGiver, String starsReasoning, String starsReceiver, String starsReceiverUid) {
        this.starsAwarded = starsAwarded;
        this.starsGiver = starsGiver;
        this.starsReasoning = starsReasoning;
        this.starsReceiver = starsReceiver;
        this.starsReceiverUid = starsReceiverUid;
    }

    // might be used later if wanting to write data to multiple nodes at once
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("starsAwarded", starsAwarded);
        result.put("starsGiver", starsGiver);
        result.put("starsReasoning", starsReasoning);
        result.put("starsReceiver", starsReceiver);

        return result;
    }

    @Override
    public String toString() {
        return "Star{" +
                "starsAwarded=" + starsAwarded +
                ", starsGiver='" + starsGiver + '\'' +
                ", starsReasoning='" + starsReasoning + '\'' +
                ", starsReceiver='" + starsReceiver + '\'' +
                '}';
    }
}
