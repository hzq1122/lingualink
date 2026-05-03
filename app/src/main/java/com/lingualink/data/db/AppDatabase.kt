package com.lingualink.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lingualink.data.db.dao.TranslationDao
import com.lingualink.data.db.entity.TranslationEntity

@Database(entities = [TranslationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao
}
