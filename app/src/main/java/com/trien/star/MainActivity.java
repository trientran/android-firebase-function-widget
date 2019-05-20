package com.trien.star;

import android.app.Activity;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.graphics.Typeface;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.trien.R;
import com.trien.databinding.ActivityMainBinding;
import com.trien.star.adapter.StarAdapter;
import com.trien.star.fragment.StarFragment;
import com.trien.star.model.Star;
import com.trien.star.model.User;
import com.trien.star.util.FirebaseUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.List;

import static com.trien.star.service.MyFirebaseMessagingService.STAR_KEY;
import static java.lang.Boolean.TRUE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, StarFragment.OnFragmentInteractionListener {

    DatabaseReference mDatabase;
    FirebaseAuth mAuth;

    EditText emailEditText;
    EditText passwordEditText;
    Button loginBtn;

    RecyclerView mRecyclerView;
    StarAdapter mRecyclerViewAdapter;

    // enable data-binding so we don't have to initialize item views
    private ActivityMainBinding binding;
    private Animation fabOpenAnimation;
    private Animation fabCloseAnimation;
    private boolean isFabMenuOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide the action bar.
        hideActionBar();

        // Check for Google Play services
        isGooglePlayServicesAvailable(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setFabHandler(new FabHandler());

        // load animations for FABs
        getAnimations();

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseUtils.getDatabaseRef();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginBtn = findViewById(R.id.loginBtn);

        loginBtn.setOnClickListener(this);

        // when user clicks Done key on the virtual keyboard while the passwordEditText being focused,
        // it triggers like clicking the login button
        passwordEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginBtn.performClick();
                    hideSoftKeyboard(MainActivity.this);
                    return true;
                }
                return false;
            }
        });

        // set up recycler view
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerViewAdapter = new StarAdapter(this);

        // set up layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, TRUE);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mRecyclerViewAdapter);
    }

    // method to hide action bar
    private void hideActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        loadStars();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    // update ui method
    private void updateUI(FirebaseUser currentUser) {

        if (currentUser != null) {
            emailEditText.setVisibility(View.GONE);
            passwordEditText.setVisibility(View.GONE);
            loginBtn.setVisibility(View.GONE);
            binding.baseFloatingActionButton.show();
        } else {
            emailEditText.setVisibility(View.VISIBLE);
            passwordEditText.setVisibility(View.VISIBLE);
            loginBtn.setVisibility(View.VISIBLE);
            binding.baseFloatingActionButton.hide();

            // remove fragment if existing
            removeFragment();
        }
    }

    // method to remove fragment
    private void removeFragment() {
        StarFragment starFragment = (StarFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainer);
        if (starFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(starFragment).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Hide the action bar.
        hideActionBar();

        // hide FAB
        StarFragment starFragment = (StarFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainer);
        if (starFragment != null) {
            binding.baseFloatingActionButton.hide();
        }

        // Check for Google Play services
        isGooglePlayServicesAvailable(this);

        // scroll to the newly added star if user has clicked on the push notification
        smoothScrollToNewStar(mRecyclerViewAdapter.getStarsList());
    }

    // load all stars from database and populate them on recycler view
    private void loadStars() {
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Star> starList = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    Star star = userSnapshot.getValue(Star.class);
                    star.starId = userSnapshot.getKey();
                    starList.add(star);
                }
                mRecyclerViewAdapter.updateAdapterData(starList);
                smoothScrollToNewStar(starList);

                Log.v("trienList", String.valueOf(starList.size()));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("CancelDN", "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };
        mDatabase.child("stars").addValueEventListener(postListener);
    }

    // method to smoothly scroll to the newly added star if user has clicked on the push notification
    private void smoothScrollToNewStar(List<Star> starList) {

        String starKey = getIntent().getStringExtra(STAR_KEY);

        if (starKey != null) {
            Log.v("trienKey", starKey);
            for (Star star : starList) {
                if (star.starId.equals(starKey)) {
                    mRecyclerView.smoothScrollToPosition(starList.indexOf(star));
                    break;
                }
            }
        } else {
            if (starList.size() > 0) {
                mRecyclerView.smoothScrollToPosition(starList.size() - 1);
            }
        }
    }

    // sign up method
    private void signUp(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.v("CreateUserDN", "createUserWithEmail:success");
                            final FirebaseUser user = mAuth.getCurrentUser();
                            // write to /online node the email address of current user to indicate that he or she is online
                            mDatabase.child("online").child(user.getUid()).setValue(user.getEmail());
                            // write all user details to database
                            writeNewUser(user.getUid(), user.getEmail());
                            // update UI accordingly
                            updateUI(user);
                            // write the current device's FCM token to realtime database. This is needed for push notification
                            writeDeviceTokenToDatabase(user);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("CreateUserDN", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Wrong email or password.",
                                    Toast.LENGTH_SHORT).show();
                            // update UI accordingly
                            updateUI(null);
                        }
                    }
                });
    }

    // send a verification email to new user
    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("TrienEmail", "Email sent.");
                        }
                    }
                });
    }

    // write the current device's FCM token to realtime database. This is needed for push notification
    private void writeDeviceTokenToDatabase(FirebaseUser user) {
        String token = FirebaseInstanceId.getInstance().getToken();
        if (token != null) {
            mDatabase.child("users").child(user.getUid()).child("notificationTokens").child(token).setValue(true);
            Log.d("tokenT", token);
        }
    }

    // sign in method
    private void signIn(final String email, final String password) {
        Toast.makeText(MainActivity.this, "Signing in.",
                Toast.LENGTH_SHORT).show();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.v("SigninDN", "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            // write to /online node the email address of current user to indicate that he or she is online
                            mDatabase.child("online").child(user.getUid()).setValue(user.getEmail());
                            // update UI accordingly
                            updateUI(user);
                            // write the current device's FCM token to realtime database. This is needed for push notification
                            writeDeviceTokenToDatabase(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("SigninDN", "signInWithEmail:failure", task.getException());
                            // if unable to sign in, then try signing up new user
                            signUp(email, password);
                        }
                    }
                });
    }

    // method to write all user details to database
    private void writeNewUser(String userId, String email) {
        String name = usernameFromEmail(email);
        User user = new User(email, name, 0L, 0L);
        mDatabase.child("users").child(userId).setValue(user);

        // update a child without rewriting the entire object. allow users to update their profiles as follows:
        /*mDatabase.child("users").child(userId).child("email").setValue(email);
        String name = usernameFromEmail(email);
        mDatabase.child("users").child(userId).child("name").setValue(name);*/
    }

    // helper method to get the characters before '@'. Eg. troy@gmail.com --> troy
    private String usernameFromEmail(String email) {
        if (email.contains("@")) {
            return email.split("@")[0];
        } else {
            return email;
        }
    }

    @Override
    public void onClick(View v) {

        // action when clicking loginBtn
        if (v == loginBtn) {
            signIn(emailEditText.getText().toString(), passwordEditText.getText().toString());
        }
    }

    // method to load award-star fragment
    private void awardStars() {
        // get fragment manager
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragmentContainer, new StarFragment());
        ft.commit();
    }

    // method to go to AllUsersActivity
    private void viewAllUsers() {
        Intent i = new Intent(this, AllUsersActivity.class);
        startActivity(i);
    }

    // method to log out
    private void logOut() {
        Log.v("trien", "out");
        // first display a dialog to confirm log-out
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Sign out");
        alertBuilder.setMessage("You want to sign out?");
        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // once user clicks ok, remove its data from /online node, it means user is offline
                FirebaseUser user = mAuth.getCurrentUser();
                mDatabase.child("online").child(user.getUid()).removeValue();
                FirebaseAuth.getInstance().signOut(); // sign out
                updateUI(null); // update UI
            }
        });

        AlertDialog alert = alertBuilder.create();
        alert.show();

        // set up fonts for dialog texts
        Typeface typeface = ResourcesCompat.getFont(this, R.font.courier_new);
        TextView message = (TextView) alert.getWindow().findViewById(android.R.id.message);
        TextView title = (TextView) alert.getWindow().findViewById(R.id.alertTitle);
        Button button1 = (Button) alert.getWindow().findViewById(android.R.id.button1);
        Button button2 = (Button) alert.getWindow().findViewById(android.R.id.button2);

        if (message != null) {
            message.setTypeface(typeface);
        }
        if (title != null) {
            title.setTypeface(typeface);
        }
        if (button1 != null) {
            button1.setTypeface(typeface);
        }
        if (button2 != null) {
            button2.setTypeface(typeface);
        }
    }

    // triggered event for StarFragment's Close button
    @Override
    public void onCloseBtnClick() {
        // remove fragment
        StarFragment starFragment = (StarFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainer);
        getSupportFragmentManager().beginTransaction()
                .remove(starFragment).commit();

        Log.v("trienaa", "dfd");
        updateUI(mAuth.getCurrentUser());
        hideSoftKeyboard(MainActivity.this);
    }

    // triggered event for StarFragment's Done button
    @Override
    public void onDoneBtnClick(Star star, User receiver) {
        Log.v("trien", star.toString());
        Log.v("trien", receiver.toStringFull());

        // generate new key and set star value
        mDatabase.child("stars").push().setValue(star);
        // write rating and subscribers to where applicable
        calRatingAndSubscribed(mDatabase.child("users").child(receiver.uid), star.starsAwarded);
        // close fragment window
        onCloseBtnClick();
    }

    // method to calculate and write rating and subscribers to database
    private void calRatingAndSubscribed(DatabaseReference userRef, final long newRating) {
        userRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                User user = mutableData.getValue(User.class);
                if (user == null) {
                    return Transaction.success(mutableData);
                }
                Log.v("trien1", user.toStringFull());
                // do an accumulated addition for user rating (stars) and subscribers number
                user.rating = user.rating + newRating;
                user.subscribed = user.subscribed + 1;
                Log.v("trien2", user.toStringFull());
                // Set value and report transaction success
                mutableData.setValue(user);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b,
                                   DataSnapshot dataSnapshot) {
                // Transaction completed
                Log.d("calRatingDN", "postTransaction:onComplete:" + databaseError);
            }
        });
    }

    /**
     * Check whether Google Play Services are available because The Firebase SDK for Android is based on Google Play Services
     * <p>
     * If not, then display dialog allowing user to update Google Play Services
     *
     * @return true if available, or false if not
     */
    public boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404).show();
            }
            return false;
        }
        return true;
    }

    // method to get animations from xml
    private void getAnimations() {

        fabOpenAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_open);

        fabCloseAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_close);

    }

    private void expandFabMenu() {

        ViewCompat.animate(binding.baseFloatingActionButton).rotation(45.0F).withLayer().setDuration(300).setInterpolator(new OvershootInterpolator(10.0F)).start();
        binding.allUsersLayout.startAnimation(fabOpenAnimation);
        binding.awardStarsLayout.startAnimation(fabOpenAnimation);
        binding.logOutLayout.startAnimation(fabOpenAnimation);
        binding.allUsersFab.setClickable(true);
        binding.awardStarsFab.setClickable(true);
        binding.logOutFab.setClickable(true);
        binding.allUsersTextView.setClickable(true);
        binding.awardStarsTextView.setClickable(true);
        binding.logOutTextView.setClickable(true);
        isFabMenuOpen = true;
    }

    private void collapseFabMenu() {

        ViewCompat.animate(binding.baseFloatingActionButton).rotation(0.0F).withLayer().setDuration(300).setInterpolator(new OvershootInterpolator(10.0F)).start();
        binding.allUsersLayout.startAnimation(fabCloseAnimation);
        binding.awardStarsLayout.startAnimation(fabCloseAnimation);
        binding.logOutLayout.startAnimation(fabCloseAnimation);
        binding.allUsersFab.setClickable(false);
        binding.awardStarsFab.setClickable(false);
        binding.logOutFab.setClickable(false);
        binding.allUsersTextView.setClickable(false);
        binding.awardStarsTextView.setClickable(false);
        binding.logOutTextView.setClickable(false);
        isFabMenuOpen = false;
    }

    // internal class to handle on click events for fabs
    public class FabHandler {

        public void onBaseFabClick(View view) {

            if (isFabMenuOpen)
                collapseFabMenu();
            else
                expandFabMenu();
        }

        public void onAwardStarsFabClick(View view) {
            if (isFabMenuOpen)
                collapseFabMenu();
            else
                expandFabMenu();

            binding.baseFloatingActionButton.hide();
            awardStars();
        }

        public void onAllUsersFabClick(View view) {
            if (isFabMenuOpen)
                collapseFabMenu();
            else
                expandFabMenu();

            viewAllUsers();
        }

        public void onLogOutFabClick(View view) {
            if (isFabMenuOpen)
                collapseFabMenu();
            else
                expandFabMenu();

            logOut();
        }
    }

    @Override
    public void onBackPressed() {

        // collapse fab menu if not
        if (isFabMenuOpen)
            collapseFabMenu();
        else
            super.onBackPressed();
    }

    // method to hide soft keyboard
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }
}
