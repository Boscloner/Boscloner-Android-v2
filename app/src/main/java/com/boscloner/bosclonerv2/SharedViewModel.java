package com.boscloner.bosclonerv2;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import javax.inject.Inject;

public class SharedViewModel extends ViewModel {

    private MutableLiveData<ForegroundService.ConnectionState> connectionStateMutableLiveData;

    @Inject
    public SharedViewModel() {
        this.connectionStateMutableLiveData = new MutableLiveData<>();
    }

    public MutableLiveData<ForegroundService.ConnectionState> getConnectionStateMutableLiveData() {
        return connectionStateMutableLiveData;
    }

    public void setConnectionState(ForegroundService.ConnectionState state) {
        connectionStateMutableLiveData.postValue(state);
    }
}