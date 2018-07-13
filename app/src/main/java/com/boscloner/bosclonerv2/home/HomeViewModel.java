package com.boscloner.bosclonerv2.home;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.support.annotation.NonNull;

import com.boscloner.bosclonerv2.ForegroundService;
import com.boscloner.bosclonerv2.room.BosclonerDatabase;
import com.boscloner.bosclonerv2.room.Event;

import java.util.List;

import javax.inject.Inject;

public class HomeViewModel extends ViewModel {

    public ObservableBoolean connectionStateProblem;
    public ObservableField<String> connectionStateMessage;
    private BosclonerDatabase database;

    @Inject
    public HomeViewModel(@NonNull BosclonerDatabase database) {
        this.database = database;
        connectionStateProblem = new ObservableBoolean(false);
        connectionStateMessage = new ObservableField<>();
    }

    public LiveData<List<Event>> getEvents() {
        return database.eventDao().getAllEvents();
    }

    public void setConnectionStateProblem(ForegroundService.ConnectionState events) {
        switch (events) {
            case LOADING:
                connectionStateProblem.set(true);
                connectionStateMessage.set("Boscloner is Starting up");
                break;
            case DISCONNECTED:
                connectionStateProblem.set(true);
                connectionStateMessage.set("Device Disconnected");
                break;
            case ATTEMPTING_TO_CONNECT:
                connectionStateProblem.set(true);
                connectionStateMessage.set("Attempting to Connect");
                break;
            case ATTEMPTING_TO_RECONNECT:
                connectionStateProblem.set(true);
                connectionStateMessage.set("Attempting to Reconnect");
                break;
            case CONNECTION_LOST:
                connectionStateProblem.set(true);
                connectionStateMessage.set("Connection Lost");
                break;
            case SCANNING:
                connectionStateProblem.set(true);
                connectionStateMessage.set("Attempting Connection to Boscloner");
                break;
            case CONNECTED:
                connectionStateProblem.set(false);
                connectionStateMessage.set("Device Connected");
                break;
            case RECONNECTED:
                connectionStateProblem.set(false);
                connectionStateMessage.set("Device Reconnected");
                break;
        }
    }
}