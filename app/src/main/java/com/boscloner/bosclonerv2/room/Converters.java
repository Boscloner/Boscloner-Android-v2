package com.boscloner.bosclonerv2.room;

import android.arch.persistence.room.TypeConverter;
import android.support.annotation.NonNull;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

public class Converters {

    @TypeConverter
    public static EventType toEventType(int ordinal) {
        return EventType.values()[ordinal];
    }

    @TypeConverter
    public static int toOrdinal(EventType eventType) {
        return eventType.ordinal();
    }

    @TypeConverter
    public static LocalDateTime toLocalDateTime(@NonNull String value) {
        return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
    }

    @TypeConverter
    public static String fromLocalDateTime(@NonNull LocalDateTime value) {
        return value.format(DateTimeFormatter.ISO_DATE_TIME);
    }
}
