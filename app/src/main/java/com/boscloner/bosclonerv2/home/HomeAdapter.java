package com.boscloner.bosclonerv2.home;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boscloner.bosclonerv2.R;
import com.boscloner.bosclonerv2.room.Event;

import java.util.ArrayList;
import java.util.List;

public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.ViewHolder> {

    private List<Event> events;

    public HomeAdapter() {
        events = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.home_recycler_item, parent, false);
        return new HomeAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        holder.eventText.setText(event.value);
    }

    @Override
    public int getItemCount() {
        return events == null ? 0 : events.size();
    }

    public void setEvents(@NonNull List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView eventText;

        ViewHolder(View view) {
            super(view);
            eventText = view.findViewById(R.id.event_text);
        }
    }
}