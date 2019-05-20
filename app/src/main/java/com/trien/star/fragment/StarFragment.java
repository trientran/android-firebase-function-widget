package com.trien.star.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.trien.R;
import com.trien.star.adapter.SpinnerUserAdapter;
import com.trien.star.model.Star;
import com.trien.star.model.User;
import com.trien.star.util.FirebaseUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StarFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class StarFragment extends Fragment implements View.OnClickListener{

    DatabaseReference mDatabase;
    FirebaseAuth mAuth;

    Context mContext;

    ImageButton closeBtn;
    Spinner userListSpinner;
    Button increaseBtn;
    Button decreaseBtn;
    TextView starsTv;
    EditText reasonEditText;
    Button doneBtn;

    // auxiliary variable for increasing/decreasing stars
    static int starNum = 0;

    private OnFragmentInteractionListener mListener;

    public StarFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_new_star, container, false);

        mDatabase = FirebaseUtils.getDatabaseRef();
        mAuth = FirebaseAuth.getInstance();

        closeBtn = rootView.findViewById(R.id.closeBtn);
        increaseBtn = rootView.findViewById(R.id.increaseBtn);
        decreaseBtn = rootView.findViewById(R.id.decreaseBtn);
        doneBtn = rootView.findViewById(R.id.doneBtn);
        starsTv = rootView.findViewById(R.id.starsTv);
        reasonEditText = rootView.findViewById(R.id.reasonEditText);
        userListSpinner = rootView.findViewById(R.id.userListSpinner);

        setUpUserSpinner();

        closeBtn.setOnClickListener(this);
        doneBtn.setOnClickListener(this);
        increaseBtn.setOnClickListener(this);
        decreaseBtn.setOnClickListener(this);

        return rootView;
    }

    // set up user spinner after loading data from server
    private void setUpUserSpinner() {
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<User> userList = new ArrayList<>();
                for (DataSnapshot userSnapshot: dataSnapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    user.uid = userSnapshot.getKey();
                    userList.add(user);
                }
                SpinnerUserAdapter userListAdapter = new SpinnerUserAdapter(mContext, userList);
                userListAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                userListSpinner.setAdapter(userListAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("CancelDN", "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };
        mDatabase.child("users").addListenerForSingleValueEvent(postListener);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
            mContext = context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        // action when clicking closeBtn
        if (v == closeBtn) {
            if (mListener != null) {
                // send trigger event to Main Activity
                mListener.onCloseBtnClick();
            }
        }
        // action when clicking doneBtn
        else if (v == doneBtn) {
            if (mListener != null) {
                User receiver = ((User) userListSpinner.getSelectedItem());
                Star newStar = new Star(Long.parseLong(starsTv.getText().toString()),
                        mAuth.getCurrentUser().getEmail(),
                        reasonEditText.getText().toString().trim(),
                        receiver.email, receiver.uid);
                // send trigger event to Main Activity
                mListener.onDoneBtnClick(newStar, receiver);
            }
        }
        // action when clicking increaseBtn
        else if (v == increaseBtn) {
            starNum += 1;
            starsTv.setText(String.valueOf(starNum));
        }
        // action when clicking decreaseBtn
        else if (v == decreaseBtn) {
            starNum -= 1;
            starsTv.setText(String.valueOf(starNum));
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onCloseBtnClick();
        void onDoneBtnClick(Star star, User receiver);
    }
}