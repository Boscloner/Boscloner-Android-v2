package com.boscloner.bosclonerv2.room;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface HistoryItemDao {
    @Query("select * from " + HistoryItem.TABLE_NAME + " order by datetime(localDateTime)")
    LiveData<List<HistoryItem>> getHistoryItems();
}
