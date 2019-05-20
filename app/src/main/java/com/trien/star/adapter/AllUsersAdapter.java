package com.trien.star.adapter;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.trien.R;
import com.trien.star.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AllUsersAdapter extends RecyclerView.Adapter<AllUsersAdapter.SimpleViewHolder> {

    private List<User> userList;

    private Context mContext;

    private AdapterView.OnItemClickListener mOnItemClickListener;

    public AllUsersAdapter(Context context) {
        userList = new ArrayList<>();
        mContext = context;
    }

    public void updateAdapterData(List<User> users) {
        userList.clear();
        userList.addAll(users);
        notifyDataSetChanged();
        Log.v("trien1", userList.toString());
    }

    @NonNull
    @Override
    public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        View root = inflater.inflate(R.layout.user_item, container, false);

        SimpleViewHolder viewHolder = new SimpleViewHolder(root, this);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleViewHolder itemHolder, int position) {
        User user = userList.get(position);
        String userNameFirstLetterCap = user.name.substring(0,1).toUpperCase() + user.name.substring(1);
        itemHolder.nameTv.setText(formatText(mContext.getResources(), userNameFirstLetterCap));
        itemHolder.ratingTv.setText(String.format(Locale.getDefault(), "%s", user.rating));
        itemHolder.awardTv.setText(String.format(Locale.getDefault(), "- %s Awards received", String.valueOf(user.subscribed)));
    }

    @Override
    public int getItemCount() {
        return userList.size();
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

    public List<User> getUserList() {
        return userList;
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private AllUsersAdapter mAdapter;

        TextView nameTv;
        TextView ratingTv;
        TextView awardTv;

        public SimpleViewHolder(View itemView, AllUsersAdapter adapter) {
            super(itemView);
            itemView.setOnClickListener(this);

            mAdapter = adapter;

            nameTv = itemView.findViewById(R.id.nameTv);
            ratingTv = itemView.findViewById(R.id.ratingTv);
            awardTv = itemView.findViewById(R.id.awardTv);
        }

        @Override
        public void onClick(View v) {
            mAdapter.onItemHolderClick(this);
        }
    }


    /**
     * Helper method to format text nicely.
     */
    private static Spanned formatText(Resources res, CharSequence username) {

        return Html.fromHtml(res.getString(R.string.username, username));
    }
}
