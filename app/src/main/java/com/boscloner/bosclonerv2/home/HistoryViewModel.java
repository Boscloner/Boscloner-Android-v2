package com.boscloner.bosclonerv2.home;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;

import com.boscloner.bosclonerv2.room.BosclonerDatabase;
import com.boscloner.bosclonerv2.room.HistoryItem;

import java.util.List;

import javax.inject.Inject;

public class HistoryViewModel extends ViewModel {

    BosclonerDatabase database;

    @Inject
    public HistoryViewModel(@NonNull BosclonerDatabase database) {
        this.database = database;
    }

    public LiveData<List<HistoryItem>> getHistoryItems() {
        return database.historyItemDao().getHistoryItems();
    }
}