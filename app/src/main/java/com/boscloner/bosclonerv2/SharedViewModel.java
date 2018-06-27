package com.boscloner.bosclonerv2;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import javax.inject.Inject;

public class SharedViewModel extends ViewModel {

    private MutableLiveData<ForegroundService.ConnectionState> connectionStateMutableLiveData;
    private MutableLiveData<HomeFragmentActions> homeFragmentActionsMutableLiveData;

    @Inject
    public SharedViewModel() {
        this.connectionStateMutableLiveData = new MutableLiveData<>();
        this.homeFragmentActionsMutableLiveData = new MutableLiveData<>();
    }

    public MutableLiveData<HomeFragmentActions> getHomeFragmentActionsMutableLiveData() {
        return homeFragmentActionsMutableLiveData;
    }

    public MutableLiveData<ForegroundService.ConnectionState> getConnectionStateMutableLiveData() {
        return connectionStateMutableLiveData;
    }

    public void setConnectionState(ForegroundService.ConnectionState state) {
        connectionStateMutableLiveData.postValue(state);
    }

    public void onCustomWriteClick() {
        homeFragmentActionsMutableLiveData.setValue(HomeFragmentActions.CUSTOM_WRITE);
        homeFragmentActionsMutableLiveData.setValue(null);
    }

    public void onAutoCloneClicked(boolean isChecked) {
        homeFragmentActionsMutableLiveData.setValue(isChecked ? HomeFragmentActions.AUTO_CLONE_ON :
                HomeFragmentActions.AUTO_CLONE_OFF);
        homeFragmentActionsMutableLiveData.setValue(null);
    }

    public enum HomeFragmentActions {
        CUSTOM_WRITE,
        AUTO_CLONE_ON,
        AUTO_CLONE_OFF
    }
}