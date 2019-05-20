package com.trien.star.adapter

import android.content.Context
import android.content.res.Resources
import androidx.recyclerview.widget.RecyclerView
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView

import com.trien.R
import com.trien.star.adapter.AllUsersAdapter.SimpleViewHolder
import com.trien.star.model.User

import java.util.ArrayList
import java.util.Locale

class AllUsersAdapter(private val mContext: Context) : RecyclerView.Adapter<AllUsersAdapter.SimpleViewHolder>() {

    private val userList: MutableList<User>

    private var mOnItemClickListener: AdapterView.OnItemClickListener? = null

    init {
        userList = ArrayList()
    }

    fun updateAdapterData(users: List<User>) {
        userList.clear()
        userList.addAll(users)
        notifyDataSetChanged()
        Log.v("trien1", userList.toString())
    }

    override fun onCreateViewHolder(container: ViewGroup, viewType: Int): SimpleViewHolder {
        val inflater = LayoutInflater.from(container.context)
        val root = inflater.inflate(R.layout.user_item, container, false)

        return SimpleViewHolder(root, this)
    }

    override fun onBindViewHolder(itemHolder: SimpleViewHolder, position: Int) {
        val user = userList[position]
        val userNameFirstLetterCap = user.name.substring(0, 1).toUpperCase() + user.name.substring(1)
        itemHolder.nameTv.text = formatText(mContext.resources, userNameFirstLetterCap)
        itemHolder.ratingTv.text = String.format(Locale.getDefault(), "%s", user.rating)
        itemHolder.awardTv.text = String.format(Locale.getDefault(), "- %s Awards received", user.subscribed.toString())
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    fun setOnItemClickListener(onItemClickListener: AdapterView.OnItemClickListener) {
        mOnItemClickListener = onItemClickListener
    }

    private fun onItemHolderClick(itemHolder: SimpleViewHolder) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener!!.onItemClick(null, itemHolder.itemView,
                    itemHolder.adapterPosition, itemHolder.itemId)
        }
    }

    fun getUserList(): List<User> {
        return userList
    }

    class SimpleViewHolder(itemView: View, private val mAdapter: AllUsersAdapter) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        internal var nameTv: TextView
        internal var ratingTv: TextView
        internal var awardTv: TextView

        init {
            itemView.setOnClickListener(this)

            nameTv = itemView.findViewById(R.id.nameTv)
            ratingTv = itemView.findViewById(R.id.ratingTv)
            awardTv = itemView.findViewById(R.id.awardTv)
        }

        override fun onClick(v: View) {
            mAdapter.onItemHolderClick(this)
        }
    }


    /**
     * Helper method to format text nicely.
     */
    private fun formatText(res: Resources, username: CharSequence): Spanned {

        return Html.fromHtml(res.getString(R.string.username, username))
    }
}
