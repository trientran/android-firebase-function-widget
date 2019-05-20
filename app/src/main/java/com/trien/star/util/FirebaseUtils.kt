package com.trien.star.util

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

/**
 * This util is to help enable offline capability of Realtime database, so all data where applied will be saved into user devices
 */

object FirebaseUtils {
    private var mDatabaseRef: DatabaseReference? = null
    private var database: FirebaseDatabase? = null
    val databaseRef: DatabaseReference
        get() {
            if (mDatabaseRef == null) {
                getDatabase()
                mDatabaseRef = database!!.reference
            }
            return mDatabaseRef as DatabaseReference
        }

    fun getDatabase(): FirebaseDatabase {
        if (database == null) {
            database = FirebaseDatabase.getInstance()
            // enable offline capability
            database!!.setPersistenceEnabled(true)
        }
        return database as FirebaseDatabase
    }
}
