package com.boscloner.bosclonerv2.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;

import org.threeten.bp.LocalDateTime;

import static com.boscloner.bosclonerv2.room.HistoryItem.TABLE_NAME;

@Entity(tableName = TABLE_NAME)
public class HistoryItem {

    public static final String TABLE_NAME = "history";

    @PrimaryKey(autoGenerate = true)
    public int id;

    @TypeConverters(Converters.class)
    public LocalDateTime localDateTime;

    public String deviceMacAddress;
}