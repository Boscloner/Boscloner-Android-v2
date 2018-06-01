package com.boscloner.bosclonerv2.room;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface HistoryItemDao {
    @Query("select * from " + HistoryItem.TABLE_NAME + " order by datetime(localDateTime)")
    LiveData<List<HistoryItem>> getHistoryItems();

    @Insert(onConflict = REPLACE)
    void add(HistoryItem item);
}
