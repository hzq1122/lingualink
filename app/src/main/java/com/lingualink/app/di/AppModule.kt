package com.lingualink.app.di

import android.content.Context
import androidx.room.Room
import com.lingualink.data.datastore.SettingsDataStore
import com.lingualink.data.db.AppDatabase
import com.lingualink.data.db.dao.TranslationDao
import com.lingualink.network.client.DeviceHttpClient
import com.lingualink.translation.OfflineTranslationEngine
import com.lingualink.translation.OnlineTranslationEngine
import com.lingualink.update.UpdateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "lingualink.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTranslationDao(db: AppDatabase): TranslationDao = db.translationDao()

    @Provides
    @Singleton
    fun provideDeviceHttpClient(): DeviceHttpClient = DeviceHttpClient()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore =
        SettingsDataStore(context)

    @Provides
    @Singleton
    fun provideOnlineEngine(settingsDataStore: SettingsDataStore): OnlineTranslationEngine =
        OnlineTranslationEngine(settingsDataStore)

    @Provides
    @Singleton
    fun provideOfflineEngine(): OfflineTranslationEngine = OfflineTranslationEngine()

    @Provides
    @Singleton
    fun provideUpdateManager(@ApplicationContext context: Context): UpdateManager =
        UpdateManager(context)
}
