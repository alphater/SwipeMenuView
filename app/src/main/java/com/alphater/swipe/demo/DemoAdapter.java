package com.alphater.swipe.demo;

import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alphater.swipe.SwipeMenuView;
import com.alphater.swipe.demo.dummy.Item;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

class DemoAdapter extends RecyclerView.Adapter<DemoAdapter.ViewHolder> {

    private List<Item> mValues;

    DemoAdapter(List<Item> items) {
        if (null == mValues) {
            mValues = new ArrayList<>();
        }
        mValues = items;
    }

    void remove(int index) {
        mValues.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Item item = mValues.get(position);
        holder.mItem = item;
        holder.mContentView.setText(item.getContent());
        Glide.with(holder.mImage.getContext().getApplicationContext())
                .load(item.getUrl())
                .centerCrop()
                .crossFade()
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .priority(Priority.IMMEDIATE)
                .into(holder.mImage);

        holder.mDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mOnSwipeListener) {
                    mOnSwipeListener.onDelete(holder.getAdapterPosition());
                }
            }
        });

        holder.mUnReadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.mSwipeMenu.smoothClose();
                Snackbar.make(view, "Un Read。", Snackbar.LENGTH_LONG).show();
            }
        });

        holder.mTopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mOnSwipeListener) {
                    mOnSwipeListener.onTop(holder.getAdapterPosition());
                }
            }
        });
    }

    interface OnSwipeActionListener {
        void onDelete(int position);

        void onTop(int position);
    }

    private OnSwipeActionListener mOnSwipeListener;

    void setOnSwipeActionListener(OnSwipeActionListener swipeActionListener) {
        this.mOnSwipeListener = swipeActionListener;
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final AppCompatImageView mImage;
        final AppCompatTextView mContentView;

        final AppCompatButton mDeleteBtn;
        final AppCompatButton mUnReadBtn;
        final AppCompatButton mTopBtn;

        final SwipeMenuView mSwipeMenu;

        Item mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            mImage = (AppCompatImageView) view.findViewById(R.id.image);
            mContentView = (AppCompatTextView) view.findViewById(R.id.content);

            mDeleteBtn = (AppCompatButton) view.findViewById(R.id.delete_btn);
            mUnReadBtn = (AppCompatButton) view.findViewById(R.id.unread_btn);
            mTopBtn = (AppCompatButton) view.findViewById(R.id.top_btn);

            mSwipeMenu = (SwipeMenuView) view.findViewById(R.id.swipe_menu);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
