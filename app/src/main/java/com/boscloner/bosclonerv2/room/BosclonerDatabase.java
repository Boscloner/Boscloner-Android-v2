package com.boscloner.bosclonerv2.room;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Event.class, HistoryItem.class}, version = 2, exportSchema = false)
public abstract class BosclonerDatabase extends RoomDatabase {
    public abstract EventDao eventDao();

    public abstract HistoryItemDao historyItemDao();
}
