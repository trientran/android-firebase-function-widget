package com.trien.star.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView

import com.trien.R
import com.trien.star.adapter.StarAdapter.SimpleViewHolder
import com.trien.star.model.Star

import java.util.ArrayList
import java.util.Locale

class StarAdapter(private val mContext: Context) : RecyclerView.Adapter<StarAdapter.SimpleViewHolder>() {

    private val starList: MutableList<Star>

    private var mOnItemClickListener: AdapterView.OnItemClickListener? = null

    val starsList: List<Star>
        get() = starList

    init {
        starList = ArrayList()
    }

    fun updateAdapterData(stars: List<Star>) {
        starList.clear()
        starList.addAll(stars)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(container: ViewGroup, viewType: Int): SimpleViewHolder {
        val inflater = LayoutInflater.from(container.context)
        val root = inflater.inflate(R.layout.star_item, container, false)

        return SimpleViewHolder(root, this)
    }

    override fun onBindViewHolder(itemHolder: SimpleViewHolder, position: Int) {
        val currentStar = starList[position]

        itemHolder.receiverTv.text = String.format(Locale.getDefault(), "%s", currentStar.starsReceiver)
        itemHolder.giverTv.text = String.format(Locale.getDefault(), "%s", currentStar.starsGiver)
        itemHolder.reasonTv.text = String.format(Locale.getDefault(), "%s", if (currentStar.starsReasoning == "") "Nothing to say" else currentStar.starsReasoning)
        itemHolder.starsAwardedTv.text = String.format(Locale.getDefault(), "%s", currentStar.starsAwarded.toString())
    }

    override fun getItemCount(): Int {
        return starList.size
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

    class SimpleViewHolder(itemView: View, private val mAdapter: StarAdapter) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        internal var receiverTv: TextView
        internal var giverTv: TextView
        internal var starsAwardedTv: TextView
        internal var reasonTv: TextView

        init {
            itemView.setOnClickListener(this)

            receiverTv = itemView.findViewById(R.id.receiverTv)
            giverTv = itemView.findViewById(R.id.giverTv)
            starsAwardedTv = itemView.findViewById(R.id.starsAwardedTv)
            reasonTv = itemView.findViewById(R.id.reasonTv)
        }

        override fun onClick(v: View) {
            mAdapter.onItemHolderClick(this)
        }
    }
}
