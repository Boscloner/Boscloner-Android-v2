package com.boscloner.bosclonerv2.di;

import com.boscloner.bosclonerv2.ForegroundService;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
abstract class MainServiceModule {

    @ContributesAndroidInjector
    abstract ForegroundService contributeForegroundService();
}