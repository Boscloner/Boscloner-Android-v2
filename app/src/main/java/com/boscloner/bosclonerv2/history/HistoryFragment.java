package com.boscloner.bosclonerv2.history;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.boscloner.bosclonerv2.R;
import com.boscloner.bosclonerv2.di.Injectable;
import com.boscloner.bosclonerv2.home.HistoryViewModel;
import com.boscloner.bosclonerv2.room.HistoryItem;

import javax.inject.Inject;

public class HistoryFragment extends Fragment implements Injectable {

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private OnListFragmentInteractionListener mListener;

    private HistoryViewModel viewModel;
    private HistoryRecyclerViewAdapter adapter;

    public HistoryFragment() {
    }

    public static Fragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.history_fragment_item_list, container, false);

        // Set the adapter
        Context context = view.getContext();
        RecyclerView recyclerView = view.findViewById(R.id.list_history_fragment);

        ImageView shareImageView = view.findViewById(R.id.image_view_history_fragment_item_list_share);
        shareImageView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.shareItems();
            }
        });

        ImageView deleteImageView = view.findViewById(R.id.image_view_history_fragment_item_list_delete);
        deleteImageView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.clearHistory();
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new HistoryRecyclerViewAdapter(mListener);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(HistoryViewModel.class);
        viewModel.getHistoryItems().observe(this, events -> {
            if (events != null) {
                adapter.setEvents(events);
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(HistoryItem item);
        void shareItems();
        void clearHistory();
    }
}
