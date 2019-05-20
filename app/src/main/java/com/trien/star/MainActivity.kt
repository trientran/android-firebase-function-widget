package com.trien.star

import android.app.Activity
import android.content.Intent
import androidx.databinding.DataBindingUtil

import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import com.trien.R
import com.trien.databinding.ActivityMainBinding
import com.trien.star.adapter.StarAdapter
import com.trien.star.fragment.StarFragment
import com.trien.star.model.Star
import com.trien.star.model.User
import com.trien.star.util.FirebaseUtils
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import com.trien.star.service.MyFirebaseMessagingService.Companion.STAR_KEY
import kotlinx.android.synthetic.main.activity_main.*

import java.util.ArrayList

import java.lang.Boolean.TRUE

class MainActivity : AppCompatActivity(), View.OnClickListener, StarFragment.OnFragmentInteractionListener {

    internal lateinit var mDatabase: DatabaseReference
    internal lateinit var mAuth: FirebaseAuth

    internal lateinit var mRecyclerViewAdapter: StarAdapter

    // enable data-binding so we don't have to initialize item views
    private var binding: ActivityMainBinding? = null
    private var fabOpenAnimation: Animation? = null
    private var fabCloseAnimation: Animation? = null
    private var isFabMenuOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hide the action bar.
        hideActionBar()

        // Check for Google Play services
        isGooglePlayServicesAvailable(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding!!.fabHandler = FabHandler()

        // load animations for FABs
        getAnimations()

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseUtils.databaseRef

        loginBtn.setOnClickListener(this)

        // when user clicks Done key on the virtual keyboard while the passwordEditText being focused,
        // it triggers like clicking the login button
        passwordEditText.setOnEditorActionListener{ v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                loginBtn.performClick()
                hideSoftKeyboard(this@MainActivity)
                true
            } else {
                false
            }
        }


        // set up recycler view
        recyclerView.setHasFixedSize(false)
        mRecyclerViewAdapter = StarAdapter(this)

        // set up layout manager
        val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, TRUE)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = mRecyclerViewAdapter
    }

    // method to hide action bar
    private fun hideActionBar() {
        val actionBar = supportActionBar
        actionBar?.hide()
    }

    public override fun onStart() {
        super.onStart()
        loadStars()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = mAuth.currentUser
        updateUI(currentUser)
    }

    // update ui method
    private fun updateUI(currentUser: FirebaseUser?) {

        if (currentUser != null) {
            emailEditText.visibility = View.GONE
            passwordEditText.visibility = View.GONE
            loginBtn.visibility = View.GONE
            binding!!.baseFloatingActionButton.show()
        } else {
            emailEditText.visibility = View.VISIBLE
            passwordEditText.visibility = View.VISIBLE
            loginBtn.visibility = View.VISIBLE
            binding!!.baseFloatingActionButton.hide()

            // remove fragment if existing
            removeFragment()
        }
    }

    // method to remove fragment
    private fun removeFragment() {
        val starFragment = supportFragmentManager
                .findFragmentById(R.id.fragmentContainer) as StarFragment?
        if (starFragment != null) {
            supportFragmentManager.beginTransaction()
                    .remove(starFragment).commit()
        }
    }

    override fun onResume() {
        super.onResume()
        // Hide the action bar.
        hideActionBar()

        // hide FAB
        val starFragment = supportFragmentManager
                .findFragmentById(R.id.fragmentContainer) as StarFragment?
        if (starFragment != null) {
            binding!!.baseFloatingActionButton.hide()
        }

        // Check for Google Play services
        isGooglePlayServicesAvailable(this)

        // scroll to the newly added star if user has clicked on the push notification
        smoothScrollToNewStar(mRecyclerViewAdapter.starsList)
    }

    // load all stars from database and populate them on recycler view
    private fun loadStars() {
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val starList = ArrayList<Star>()
                for (userSnapshot in dataSnapshot.children) {
                    val star = userSnapshot.getValue(Star::class.java)
                    star!!.starId = userSnapshot.key
                    starList.add(star)
                }
                mRecyclerViewAdapter.updateAdapterData(starList)
                smoothScrollToNewStar(starList)

                Log.v("trienList", starList.size.toString())
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("CancelDN", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        }
        mDatabase.child("stars").addValueEventListener(postListener)
    }

    // method to smoothly scroll to the newly added star if user has clicked on the push notification
    private fun smoothScrollToNewStar(starList: List<Star>) {

        val starKey = intent.getStringExtra(STAR_KEY)

        if (starKey != null) {
            Log.v("trienKey", starKey)
            for (star in starList) {
                if (star.starId == starKey) {
                    recyclerView.smoothScrollToPosition(starList.indexOf(star))
                    break
                }
            }
        } else {
            if (starList.size > 0) {
                recyclerView.smoothScrollToPosition(starList.size - 1)
            }
        }
    }

    // sign up method
    private fun signUp(email: String, password: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.v("CreateUserDN", "createUserWithEmail:success")
                        val user = mAuth.currentUser
                        // write to /online node the email address of current user to indicate that he or she is online
                        mDatabase.child("online").child(user!!.uid).setValue(user.email)
                        // write all user details to database
                        writeNewUser(user.uid, user.email)
                        // update UI accordingly
                        updateUI(user)
                        // write the current device's FCM token to realtime database. This is needed for push notification
                        writeDeviceTokenToDatabase(user)

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("CreateUserDN", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(this@MainActivity, "Wrong email or password.",
                                Toast.LENGTH_SHORT).show()
                        // update UI accordingly
                        updateUI(null)
                    }
                }
    }

    // send a verification email to new user
    private fun sendEmailVerification(user: FirebaseUser) {
        user.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("TrienEmail", "Email sent.")
                    }
                }
    }

    // write the current device's FCM token to realtime database. This is needed for push notification
    private fun writeDeviceTokenToDatabase(user: FirebaseUser?) {
        val token = FirebaseInstanceId.getInstance().token
        if (token != null) {
            mDatabase.child("users").child(user!!.uid).child("notificationTokens").child(token).setValue(true)
            Log.d("tokenT", token)
        }
    }

    // sign in method
    private fun signIn(email: String, password: String) {
        Toast.makeText(this@MainActivity, "Signing in.",
                Toast.LENGTH_SHORT).show()
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.v("SigninDN", "signInWithEmail:success")
                        val user = mAuth.currentUser
                        // write to /online node the email address of current user to indicate that he or she is online
                        mDatabase.child("online").child(user!!.uid).setValue(user.email)
                        // update UI accordingly
                        updateUI(user)
                        // write the current device's FCM token to realtime database. This is needed for push notification
                        writeDeviceTokenToDatabase(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("SigninDN", "signInWithEmail:failure", task.exception)
                        // if unable to sign in, then try signing up new user
                        signUp(email, password)
                    }
                }
    }

    // method to write all user details to database
    private fun writeNewUser(userId: String, email: String?) {
        val name = usernameFromEmail(email!!)
        val user = User(email, name, 0L, 0L)
        mDatabase.child("users").child(userId).setValue(user)

        // update a child without rewriting the entire object. allow users to update their profiles as follows:
        /*mDatabase.child("users").child(userId).child("email").setValue(email);
        String name = usernameFromEmail(email);
        mDatabase.child("users").child(userId).child("name").setValue(name);*/
    }

    // helper method to get the characters before '@'. Eg. troy@gmail.com --> troy
    private fun usernameFromEmail(email: String): String {
        return if (email.contains("@")) {
            email.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        } else {
            email
        }
    }

    override fun onClick(v: View) {

        // action when clicking loginBtn
        if (v === loginBtn) {
            signIn(emailEditText.text.toString(), passwordEditText.text.toString())
        }
    }

    // method to load award-star fragment
    private fun awardStars() {
        // get fragment manager
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.replace(R.id.fragmentContainer, StarFragment())
        ft.commit()
    }

    // method to go to AllUsersActivity
    private fun viewAllUsers() {
        val i = Intent(this, AllUsersActivity::class.java)
        startActivity(i)
    }

    // method to log out
    private fun logOut() {
        Log.v("trien", "out")
        // first display a dialog to confirm log-out
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setCancelable(true)
        alertBuilder.setTitle("Sign out")
        alertBuilder.setMessage("You want to sign out?")
        alertBuilder.setPositiveButton(android.R.string.yes) { dialog, which ->
            // once user clicks ok, remove its data from /online node, it means user is offline
            val user = mAuth.currentUser
            mDatabase.child("online").child(user!!.uid).removeValue()
            FirebaseAuth.getInstance().signOut() // sign out
            updateUI(null) // update UI
        }

        val alert = alertBuilder.create()
        alert.show()

        // set up fonts for dialog texts
        val typeface = ResourcesCompat.getFont(this, R.font.courier_new)
        val message = alert.window!!.findViewById<View>(android.R.id.message) as TextView
        val title = alert.window!!.findViewById<View>(R.id.alertTitle) as TextView
        val button1 = alert.window!!.findViewById<View>(android.R.id.button1) as Button
        val button2 = alert.window!!.findViewById<View>(android.R.id.button2) as Button

        if (message != null) {
            message.typeface = typeface
        }
        if (title != null) {
            title.typeface = typeface
        }
        if (button1 != null) {
            button1.typeface = typeface
        }
        if (button2 != null) {
            button2.typeface = typeface
        }
    }

    // triggered event for StarFragment's Close button
    override fun onCloseBtnClick() {
        // remove fragment
        val starFragment = supportFragmentManager
                .findFragmentById(R.id.fragmentContainer) as StarFragment?
        supportFragmentManager.beginTransaction()
                .remove(starFragment!!).commit()

        Log.v("trienaa", "dfd")
        updateUI(mAuth.currentUser)
        hideSoftKeyboard(this@MainActivity)
    }

    // triggered event for StarFragment's Done button
    override fun onDoneBtnClick(star: Star, receiver: User) {
        Log.v("trien", star.toString())
        Log.v("trien", receiver.toStringFull())

        // generate new key and set star value
        mDatabase.child("stars").push().setValue(star)
        // write rating and subscribers to where applicable
        calRatingAndSubscribed(mDatabase.child("users").child(receiver.uid), star.starsAwarded)
        // close fragment window
        onCloseBtnClick()
    }

    // method to calculate and write rating and subscribers to database
    private fun calRatingAndSubscribed(userRef: DatabaseReference, newRating: Long) {
        userRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val user = mutableData.getValue(User::class.java)
                        ?: return Transaction.success(mutableData)
                Log.v("trien1", user.toStringFull())
                // do an accumulated addition for user rating (stars) and subscribers number
                user.rating = user.rating + newRating
                user.subscribed = user.subscribed + 1
                Log.v("trien2", user.toStringFull())
                // Set value and report transaction success
                mutableData.value = user
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, b: Boolean,
                                    dataSnapshot: DataSnapshot?) {
                // Transaction completed
                Log.d("calRatingDN", "postTransaction:onComplete:" + databaseError!!)
            }
        })
    }

    /**
     * Check whether Google Play Services are available because The Firebase SDK for Android is based on Google Play Services
     *
     *
     * If not, then display dialog allowing user to update Google Play Services
     *
     * @return true if available, or false if not
     */
    fun isGooglePlayServicesAvailable(activity: Activity): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(activity)
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404).show()
            }
            return false
        }
        return true
    }

    // method to get animations from xml
    private fun getAnimations() {

        fabOpenAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_open)

        fabCloseAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_close)

    }

    private fun expandFabMenu() {

        ViewCompat.animate(binding!!.baseFloatingActionButton).rotation(45.0f).withLayer().setDuration(300).setInterpolator(OvershootInterpolator(10.0f)).start()
        binding!!.allUsersLayout.startAnimation(fabOpenAnimation)
        binding!!.awardStarsLayout.startAnimation(fabOpenAnimation)
        binding!!.logOutLayout.startAnimation(fabOpenAnimation)
        binding!!.allUsersFab.isClickable = true
        binding!!.awardStarsFab.isClickable = true
        binding!!.logOutFab.isClickable = true
        binding!!.allUsersTextView.isClickable = true
        binding!!.awardStarsTextView.isClickable = true
        binding!!.logOutTextView.isClickable = true
        isFabMenuOpen = true
    }

    private fun collapseFabMenu() {

        ViewCompat.animate(binding!!.baseFloatingActionButton).rotation(0.0f).withLayer().setDuration(300).setInterpolator(OvershootInterpolator(10.0f)).start()
        binding!!.allUsersLayout.startAnimation(fabCloseAnimation)
        binding!!.awardStarsLayout.startAnimation(fabCloseAnimation)
        binding!!.logOutLayout.startAnimation(fabCloseAnimation)
        binding!!.allUsersFab.isClickable = false
        binding!!.awardStarsFab.isClickable = false
        binding!!.logOutFab.isClickable = false
        binding!!.allUsersTextView.isClickable = false
        binding!!.awardStarsTextView.isClickable = false
        binding!!.logOutTextView.isClickable = false
        isFabMenuOpen = false
    }

    // internal class to handle on click events for fabs
    inner class FabHandler {

        fun onBaseFabClick(view: View) {

            if (isFabMenuOpen)
                collapseFabMenu()
            else
                expandFabMenu()
        }

        fun onAwardStarsFabClick(view: View) {
            if (isFabMenuOpen)
                collapseFabMenu()
            else
                expandFabMenu()

            binding!!.baseFloatingActionButton.hide()
            awardStars()
        }

        fun onAllUsersFabClick(view: View) {
            if (isFabMenuOpen)
                collapseFabMenu()
            else
                expandFabMenu()

            viewAllUsers()
        }

        fun onLogOutFabClick(view: View) {
            if (isFabMenuOpen)
                collapseFabMenu()
            else
                expandFabMenu()

            logOut()
        }
    }

    override fun onBackPressed() {

        // collapse fab menu if not
        if (isFabMenuOpen)
            collapseFabMenu()
        else
            super.onBackPressed()
    }

    companion object {

        // method to hide soft keyboard
        fun hideSoftKeyboard(activity: Activity) {
            val inputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(activity.currentFocus!!.windowToken, 0)
        }
    }
}
