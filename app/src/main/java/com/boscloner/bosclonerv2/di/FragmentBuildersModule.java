package com.boscloner.bosclonerv2.di;

import com.boscloner.bosclonerv2.util.permissions_fragment.PermissionsFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class FragmentBuildersModule {

    @ContributesAndroidInjector
    abstract PermissionsFragment permissionsFragment();
}