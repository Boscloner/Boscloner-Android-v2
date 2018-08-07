package com.boscloner.bosclonerv2;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.support.v7.preference.PreferenceManager;

import com.boscloner.bosclonerv2.di.AppInjector;
import com.crashlytics.android.Crashlytics;
import com.jakewharton.threetenabp.AndroidThreeTen;

import io.fabric.sdk.android.Fabric;
import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasServiceInjector;
import timber.log.Timber;

public class BosclonerApp extends Application implements HasActivityInjector, HasServiceInjector {

    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;

    @Inject
    DispatchingAndroidInjector<Service> dispatchingServiceInjector;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
        AppInjector.init(this);
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Fabric.with(this, new Crashlytics());
        }
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
    }

    @Override
    public AndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidInjector;
    }

    @Override
    public AndroidInjector<Service> serviceInjector() { return dispatchingServiceInjector; }
}