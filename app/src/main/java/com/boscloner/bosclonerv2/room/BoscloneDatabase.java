package com.boscloner.bosclonerv2.room;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Event.class}, version = 1, exportSchema = false)
public abstract class BoscloneDatabase extends RoomDatabase {
    public abstract EventDao eventDao();
}
