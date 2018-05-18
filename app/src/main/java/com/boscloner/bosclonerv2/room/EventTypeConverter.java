package com.boscloner.bosclonerv2.room;

import android.arch.persistence.room.TypeConverter;

public class EventTypeConverter {
    @TypeConverter
    public static EventType toEventType(int ordinal) {
        return EventType.values()[ordinal];
    }

    @TypeConverter
    public static int toOrdinal(EventType eventType) {
        return eventType.ordinal();
    }
}
