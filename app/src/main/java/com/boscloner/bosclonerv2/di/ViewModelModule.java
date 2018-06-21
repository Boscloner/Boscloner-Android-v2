package com.boscloner.bosclonerv2.di;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import com.boscloner.bosclonerv2.SharedViewModel;
import com.boscloner.bosclonerv2.home.HistoryViewModel;
import com.boscloner.bosclonerv2.home.HomeViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel.class)
    abstract ViewModel bindHomeViewModel(HomeViewModel homeViewModel);

    @Binds
    @IntoMap
    @ViewModelKey(HistoryViewModel.class)
    abstract ViewModel bindHistoryViewModel(HistoryViewModel historyViewModel);

    @Binds
    @IntoMap
    @ViewModelKey(SharedViewModel.class)
    abstract ViewModel sharedViewModel(SharedViewModel sharedViewModel);

    @Binds
    abstract ViewModelProvider.Factory bindViewModelFactory(BosclonerViewModelFactory factory);
}