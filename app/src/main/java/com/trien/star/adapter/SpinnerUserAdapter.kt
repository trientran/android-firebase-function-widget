package com.trien.star.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import com.trien.R
import com.trien.star.model.User

class SpinnerUserAdapter(context: Context, users: List<User>) : ArrayAdapter<User>(context, 0, users) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        // Get the data item for this position
        val user = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.spinner_user_item, parent, false)
        }
        // Lookup view for data population
        val tvName = convertView!!.findViewById<View>(R.id.tvName) as TextView
        // Populate the data into the template view using the data object
        tvName.text = user!!.name
        // Return the completed view to render on screen
        return convertView
    }
}