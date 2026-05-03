package com.lingualink.app.di

import android.content.Context
import androidx.room.Room
import com.lingualink.data.db.AppDatabase
import com.lingualink.data.db.dao.TranslationDao
import com.lingualink.network.client.DeviceHttpClient
import com.lingualink.network.server.FanyiHttpServer
import com.lingualink.translation.OfflineTranslationEngine
import com.lingualink.translation.OnlineTranslationEngine
import com.lingualink.update.UpdateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

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
    fun provideOnlineEngine(httpClient: HttpClient): OnlineTranslationEngine =
        OnlineTranslationEngine(httpClient)

    @Provides
    @Singleton
    fun provideOfflineEngine(@ApplicationContext context: Context): OfflineTranslationEngine =
        OfflineTranslationEngine(context)

    @Provides
    @Singleton
    fun provideUpdateManager(@ApplicationContext context: Context): UpdateManager =
        UpdateManager(context)
}
