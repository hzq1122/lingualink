package com.lingualink.translation

import com.lingualink.domain.model.TranslationMode
import com.lingualink.domain.model.TranslationRequest
import com.lingualink.domain.model.TranslationResult

/**
 * Offline translation placeholder.
 * ML Kit removed to reduce APK size and prevent native lib crashes.
 * Can be re-added later as an optional module.
 */
class OfflineTranslationEngine : TranslationEngine {
    override val mode = TranslationMode.OFFLINE
    override suspend fun translate(request: TranslationRequest): TranslationResult {
        throw UnsupportedOperationException("离线翻译暂不可用，请配置在线 API")
    }
    override suspend fun detectLanguage(text: String): String = "auto"
    override fun isAvailable(): Boolean = false
}
