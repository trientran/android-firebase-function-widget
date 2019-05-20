package com.trien.star.util;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * This util is to help enable offline capability of Realtime database, so all data where applied will be saved into user devices
 */

public class FirebaseUtils {
    private static DatabaseReference mDatabaseRef;
    private static FirebaseDatabase database;
    public static DatabaseReference getDatabaseRef() {
        if (mDatabaseRef == null) {
            getDatabase();
            mDatabaseRef = database.getReference();
        }
        return mDatabaseRef;
    }

    public static FirebaseDatabase getDatabase() {
        if (database == null) {
            database = FirebaseDatabase.getInstance();
            // enable offline capability
            database.setPersistenceEnabled(true);
        }
        return database;
    }
}
