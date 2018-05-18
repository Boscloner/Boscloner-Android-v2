package com.boscloner.bosclonerv2.room;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface EventDao {
    @Query("select * from " + Event.TABLE_NAME)
    LiveData<List<Event>> getAllEvents();

    @Insert(onConflict = REPLACE)
    void addEvent(Event event);
}
