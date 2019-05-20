package com.trien.star.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.trien.R;
import com.trien.star.model.Star;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StarAdapter extends RecyclerView.Adapter<StarAdapter.SimpleViewHolder> {

    private List<Star> starList;

    private Context mContext;

    private AdapterView.OnItemClickListener mOnItemClickListener;

    public StarAdapter(Context context) {
        starList = new ArrayList<>();
        mContext = context;
    }

    public void updateAdapterData(List<Star> stars) {
        starList.clear();
        starList.addAll(stars);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        View root = inflater.inflate(R.layout.star_item, container, false);

        SimpleViewHolder viewHolder = new SimpleViewHolder(root, this);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleViewHolder itemHolder, int position) {
        Star currentStar = starList.get(position);

        itemHolder.receiverTv.setText(String.format(Locale.getDefault(), "%s", currentStar.starsReceiver));
        itemHolder.giverTv.setText(String.format(Locale.getDefault(), "%s", currentStar.starsGiver));
        itemHolder.reasonTv.setText(String.format(Locale.getDefault(), "%s", currentStar.starsReasoning.equals("")? "Nothing to say" : currentStar.starsReasoning));
        itemHolder.starsAwardedTv.setText(String.format(Locale.getDefault(), "%s", String.valueOf(currentStar.starsAwarded)));
    }

    @Override
    public int getItemCount() {
        return starList.size();
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    private void onItemHolderClick(SimpleViewHolder itemHolder) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(null, itemHolder.itemView,
                    itemHolder.getAdapterPosition(), itemHolder.getItemId());
        }
    }

    public List<Star> getStarsList() {
        return starList;
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private StarAdapter mAdapter;

        TextView receiverTv;
        TextView giverTv;
        TextView starsAwardedTv;
        TextView reasonTv;

        public SimpleViewHolder(View itemView, StarAdapter adapter) {
            super(itemView);
            itemView.setOnClickListener(this);

            mAdapter = adapter;

            receiverTv = itemView.findViewById(R.id.receiverTv);
            giverTv = itemView.findViewById(R.id.giverTv);
            starsAwardedTv = itemView.findViewById(R.id.starsAwardedTv);
            reasonTv = itemView.findViewById(R.id.reasonTv);
        }

        @Override
        public void onClick(View v) {
            mAdapter.onItemHolderClick(this);
        }
    }
}
