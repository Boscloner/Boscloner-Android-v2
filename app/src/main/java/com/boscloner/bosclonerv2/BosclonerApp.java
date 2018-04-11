package com.boscloner.bosclonerv2;

import android.app.Application;

import timber.log.Timber;

public class BosclonerApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
