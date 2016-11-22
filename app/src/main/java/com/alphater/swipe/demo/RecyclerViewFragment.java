package com.alphater.swipe.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.alphater.swipe.SwipeMenuView;
import com.alphater.swipe.demo.dummy.Item;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RecyclerViewFragment() {
    }

    @SuppressWarnings("unused")
    public static RecyclerViewFragment newInstance(int columnCount) {
        RecyclerViewFragment fragment = new RecyclerViewFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_list, container, false);
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            final RecyclerView recyclerView = (RecyclerView) view;
            final GridLayoutManager layoutManager = new GridLayoutManager(context, mColumnCount);
            recyclerView.setLayoutManager(layoutManager);

            final List<Item> items = new ArrayList<>();
            String[] pics = getResources().getStringArray(R.array.pics);
            String[] txts = getResources().getStringArray(R.array.txts);
            int size = pics.length;
            for (int i = 0; i < size; i++) {
                Item item = new Item();
                item.setUrl(pics[i]);
                item.setContent(txts[i]);
                item.setId(i);
                items.add(item);
            }

            final DemoAdapter adapter = new DemoAdapter(items);
            recyclerView.setAdapter(adapter);

            adapter.setOnSwipeActionListener(new DemoAdapter.OnSwipeActionListener() {
                @Override
                public void onDelete(int position) {
                    if (position > 0 && position < items.size()) {
                        Snackbar.make(recyclerView, "Delete：" + position, Snackbar.LENGTH_LONG).show();
                        adapter.remove(position);
                    }
                }

                @Override
                public void onTop(int position) {
                    if (position > 0 && position < items.size()) {
                        Item item = items.get(position);
                        items.remove(item);
                        adapter.notifyItemInserted(0);
                        items.add(0, item);
                        adapter.notifyItemRemoved(position + 1);
                        if (layoutManager.findFirstVisibleItemPosition() == 0) {
                            recyclerView.scrollToPosition(0);
                        }
                    } else {
                        Snackbar.make(recyclerView, "Already at the top!", Snackbar.LENGTH_LONG).show();
                    }
                }
            });

            recyclerView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        SwipeMenuView viewCache = SwipeMenuView.getViewCache();
                        if (null != viewCache) {
                            viewCache.smoothClose();
                        }
                    }
                    return false;
                }
            });
        }
        return view;
    }
}
