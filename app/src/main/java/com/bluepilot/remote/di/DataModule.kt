package com.bluepilot.remote.di

import com.bluepilot.remote.data.settings.DataStoreSettings
import com.bluepilot.remote.domain.SettingsStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt wiring for the data layer: DataStore settings. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {
    @Binds
    @Singleton
    abstract fun bindSettingsStore(impl: DataStoreSettings): SettingsStore
}
