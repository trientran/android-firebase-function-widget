package com.trien.star.model;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class User {

    public String uid; // this variable will be ignored when pairing with database as of 26/06/2018 as it does not exist as a value in the users/uid node
    public String email;
    public String name;
    public Map<String, Boolean> notificationTokens = new HashMap<>();
    public Long rating;
    public Long subscribed;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String email, String name, Long rating, Long subscribed) {
        this.email = email;
        this.name = name;
        this.rating = rating;
        this.subscribed = subscribed;
    }

    public User(String uid, String email, String name, Long rating, Long subscribed) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.rating = rating;
        this.subscribed = subscribed;
    }

    @Override
    public String toString() {
        return this.name;            // What to display in the Spinner list.
    }

    public String toStringFull() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", rating=" + rating +
                ", subscribed=" + subscribed +
                '}';
    }

}
