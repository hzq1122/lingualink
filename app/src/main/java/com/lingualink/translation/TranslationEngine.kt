package com.lingualink.translation

import com.lingualink.domain.model.TranslationMode
import com.lingualink.domain.model.TranslationRequest
import com.lingualink.domain.model.TranslationResult

interface TranslationEngine {
    val mode: TranslationMode
    suspend fun translate(request: TranslationRequest): TranslationResult
    suspend fun detectLanguage(text: String): String
    fun isAvailable(): Boolean
}
