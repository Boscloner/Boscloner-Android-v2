package com.boscloner.bosclonerv2.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ActionStatus<Status extends Enum> {
    @NonNull
    public final Status status;

    @Nullable
    public final String message_title;

    @Nullable
    public final String message_body;


    public ActionStatus(@NonNull Status status) {
        this.status = status;
        this.message_title = null;
        this.message_body = null;
    }

    public ActionStatus(@NonNull Status status, @Nullable String message_title) {
        this.status = status;
        this.message_title = message_title;
        this.message_body = null;
    }

    public ActionStatus(@NonNull Status status, @Nullable String message_title, @Nullable String message_body) {
        this.status = status;
        this.message_title = message_title;
        this.message_body = message_body;
    }
}
