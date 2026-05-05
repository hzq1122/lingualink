package com.lingualink.data.db.dao

import androidx.room.*
import com.lingualink.data.db.entity.TranslationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Insert
    suspend fun insert(entity: TranslationEntity): Long

    @Query("SELECT * FROM translations ORDER BY createdAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<TranslationEntity>>

    @Query("SELECT * FROM translations WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun getBySession(sessionId: String): Flow<List<TranslationEntity>>

    @Query("""
        SELECT * FROM translations
        WHERE originalText LIKE '%' || :keyword || '%' ESCAPE '\'
           OR translatedText LIKE '%' || :keyword || '%' ESCAPE '\'
        ORDER BY createdAt DESC LIMIT :limit
    """)
    suspend fun search(keyword: String, limit: Int = 50): List<TranslationEntity>

    @Query("DELETE FROM translations")
    suspend fun deleteAll()

    @Query("DELETE FROM translations WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
