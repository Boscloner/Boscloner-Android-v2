package com.boscloner.bosclonerv2.di;

import com.boscloner.bosclonerv2.home.HomeFragment;
import com.boscloner.bosclonerv2.history.HistoryFragment;
import com.boscloner.bosclonerv2.history.SettingsFragment;
import com.boscloner.bosclonerv2.util.permissions_fragment.PermissionsFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class FragmentBuildersModule {

    @ContributesAndroidInjector
    abstract PermissionsFragment permissionsFragment();

    @ContributesAndroidInjector
    abstract HistoryFragment historyFragment();

    @ContributesAndroidInjector
    abstract SettingsFragment settingsFragment();

    @ContributesAndroidInjector
    abstract HomeFragment homeFragment();
}