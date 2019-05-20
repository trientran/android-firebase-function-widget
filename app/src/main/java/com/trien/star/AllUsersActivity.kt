package com.trien.star

import androidx.core.app.NavUtils
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar

import android.util.Log
import android.view.MenuItem

import com.trien.R
import com.trien.star.adapter.AllUsersAdapter
import com.trien.star.model.User
import com.trien.star.util.FirebaseUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_all_users.*

import java.util.ArrayList

import java.lang.Boolean.FALSE

class AllUsersActivity : AppCompatActivity() {

    internal lateinit var mDatabase: DatabaseReference

    internal lateinit var mRecyclerViewAdapter: AllUsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_users)

        setSupportActionBar(my_toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mDatabase = FirebaseUtils.databaseRef

        // set up recycler view
        allUsersRecyclerView.setHasFixedSize(false)
        mRecyclerViewAdapter = AllUsersAdapter(this)

        // set up layout manager
        val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, FALSE)
        allUsersRecyclerView.layoutManager = layoutManager
        allUsersRecyclerView.adapter = mRecyclerViewAdapter
        val dividerItemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        allUsersRecyclerView.addItemDecoration(dividerItemDecoration)

    }

    public override fun onStart() {
        super.onStart()
        loadUsersDetails()
    }

    // load all users' details and populate on recycler view
    private fun loadUsersDetails() {
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userList = ArrayList<User>()
                for (userSnapshot in dataSnapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user!!.uid = userSnapshot.key
                    userList.add(user)
                }
                mRecyclerViewAdapter.updateAdapterData(userList)

                Log.v("trienList", userList.size.toString())
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("CancelDN", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        }
        mDatabase.child("users").addValueEventListener(postListener)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Respond to the action bar's Up/Home button
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
