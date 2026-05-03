package com.lingualink.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translations")
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestId: String,
    val sessionId: String,
    val senderDeviceId: String,
    val senderAlias: String,
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val direction: String,
    val translationMode: String,
    val latencyMs: Long,
    val createdAt: Long
)
