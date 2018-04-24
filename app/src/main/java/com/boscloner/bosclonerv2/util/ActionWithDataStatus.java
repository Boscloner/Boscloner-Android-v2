package com.boscloner.bosclonerv2.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ActionWithDataStatus<Status extends Enum, Data> extends ActionStatus<Status> {

    @Nullable
    public final Data data;

    public ActionWithDataStatus(@NonNull Status status, @Nullable Data data, @Nullable String message_title, @Nullable String message_body) {
        super(status, message_title, message_body);
        this.data = data;
    }

    public ActionWithDataStatus(@NonNull Status status, @Nullable String message_title, @Nullable String message_body) {
        super(status, message_title, message_body);
        data = null;
    }

    public ActionWithDataStatus(@NonNull Status status, @Nullable Data data, @Nullable String message_title) {
        super(status, message_title);
        this.data = data;
    }

    public ActionWithDataStatus(@NonNull Status status, @Nullable String message_title) {
        super(status, message_title);
        this.data = null;
    }

    public ActionWithDataStatus(@NonNull Status status, @NonNull Data data) {
        super(status, null);
        this.data = data;
    }

    public ActionWithDataStatus(@NonNull Status status) {
        super(status, null);
        this.data = null;
    }
}