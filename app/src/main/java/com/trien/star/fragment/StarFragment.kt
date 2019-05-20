package com.trien.star.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView

import com.trien.R
import com.trien.star.adapter.SpinnerUserAdapter
import com.trien.star.model.Star
import com.trien.star.model.User
import com.trien.star.util.FirebaseUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_new_star.*

import java.util.ArrayList

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [StarFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 */
class StarFragment : Fragment(), View.OnClickListener {

    internal lateinit var mDatabase: DatabaseReference
    internal lateinit var mAuth: FirebaseAuth

    internal lateinit var mContext: Context

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_new_star, container, false)

        mDatabase = FirebaseUtils.databaseRef
        mAuth = FirebaseAuth.getInstance()

        setUpUserSpinner()

        closeBtn.setOnClickListener(this)
        doneBtn.setOnClickListener(this)
        increaseBtn.setOnClickListener(this)
        decreaseBtn.setOnClickListener(this)

        return rootView
    }

    // set up user spinner after loading data from server
    private fun setUpUserSpinner() {
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userList = ArrayList<User>()
                for (userSnapshot in dataSnapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user!!.uid = userSnapshot.key
                    userList.add(user)
                }
                val userListAdapter = SpinnerUserAdapter(mContext, userList)
                userListAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                userListSpinner.adapter = userListAdapter
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("CancelDN", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        }
        mDatabase.child("users").addListenerForSingleValueEvent(postListener)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
            mContext = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onClick(v: View) {
        // action when clicking closeBtn
        if (v === closeBtn) {
            if (mListener != null) {
                // send trigger event to Main Activity
                mListener!!.onCloseBtnClick()
            }
        } else if (v === doneBtn) {
            if (mListener != null) {
                val receiver = userListSpinner.selectedItem as User
                val newStar = Star(java.lang.Long.parseLong(starsTv.text.toString()),
                        mAuth.currentUser!!.email,
                        reasonEditText.text.toString().trim { it <= ' ' },
                        receiver.email, receiver.uid)
                // send trigger event to Main Activity
                mListener!!.onDoneBtnClick(newStar, receiver)
            }
        } else if (v === increaseBtn) {
            starNum += 1
            starsTv.text = starNum.toString()
        } else if (v === decreaseBtn) {
            starNum -= 1
            starsTv.text = starNum.toString()
        }// action when clicking decreaseBtn
        // action when clicking increaseBtn
        // action when clicking doneBtn
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        fun onCloseBtnClick()
        fun onDoneBtnClick(star: Star, receiver: User)
    }

    companion object {

        // auxiliary variable for increasing/decreasing stars
        internal var starNum = 0
    }
}// Required empty public constructor