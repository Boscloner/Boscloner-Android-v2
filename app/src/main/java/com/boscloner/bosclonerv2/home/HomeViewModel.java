package com.boscloner.bosclonerv2.home;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;

import com.boscloner.bosclonerv2.room.BosclonerDatabase;
import com.boscloner.bosclonerv2.room.Event;

import java.util.List;

import javax.inject.Inject;

public class HomeViewModel extends ViewModel {

    BosclonerDatabase database;

    @Inject
    public HomeViewModel(@NonNull BosclonerDatabase database) {
        this.database = database;
    }

    public LiveData<List<Event>> getEvents() {
        return database.eventDao().getAllEvents();
    }
}