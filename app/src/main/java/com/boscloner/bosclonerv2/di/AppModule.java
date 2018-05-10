package com.boscloner.bosclonerv2.di;

import android.app.Application;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = ViewModelModule.class)
class AppModule {
    public Application application;
    public AppModule(Application application) { this.application = application; }

    @Singleton
    @Provides
    Context provideApplication() {
        return application;
    }
}