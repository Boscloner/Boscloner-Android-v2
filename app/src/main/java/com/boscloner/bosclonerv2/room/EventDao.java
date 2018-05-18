package com.boscloner.bosclonerv2.room;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface EventDao {
    @Query("select * from " + Event.TABLE_NAME)
    LiveData<List<Event>> getAllEvents();
}
