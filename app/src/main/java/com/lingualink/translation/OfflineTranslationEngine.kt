package com.lingualink.translation

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.lingualink.domain.model.TranslationMode
import com.lingualink.domain.model.TranslationRequest
import com.lingualink.domain.model.TranslationResult
import kotlinx.coroutines.tasks.await

class OfflineTranslationEngine(
    private val context: Context
) : TranslationEngine {

    override val mode = TranslationMode.OFFLINE
    private val downloadedLanguages = mutableSetOf<String>()

    override suspend fun translate(request: TranslationRequest): TranslationResult {
        val sourceLang = if (request.sourceLang == "auto") detectLanguage(request.text) else request.sourceLang

        val source = TranslateLanguage.fromBCP47(sourceLang)
            ?: throw IllegalArgumentException("Unsupported language: $sourceLang")
        val target = TranslateLanguage.fromBCP47(request.targetLang)
            ?: throw IllegalArgumentException("Unsupported language: ${request.targetLang}")

        ensureModelDownloaded(sourceLang)
        ensureModelDownloaded(request.targetLang)

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val translator = Translation.getClient(options)

        val startTime = System.currentTimeMillis()
        val result = translator.translate(request.text).await()
        val latency = System.currentTimeMillis() - startTime

        return TranslationResult(
            translatedText = result,
            sourceLang = sourceLang,
            targetLang = request.targetLang,
            latencyMs = latency,
            mode = TranslationMode.OFFLINE
        )
    }

    override suspend fun detectLanguage(text: String): String {
        val detector = LanguageIdentification.getClient()
        return detector.identifyLanguage(text).await()
    }

    override fun isAvailable(): Boolean = true

    private suspend fun ensureModelDownloaded(langCode: String) {
        if (langCode in downloadedLanguages) return
        val language = TranslateLanguage.fromBCP47(langCode) ?: return
        val model = com.google.mlkit.common.model.RemoteModel.Builder("translate").build()
        val conditions = DownloadConditions.Builder().build()
        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(language)
                .setTargetLanguage(language)
                .build()
        )
        translator.downloadModelIfNeeded(conditions).await()
        downloadedLanguages.add(langCode)
    }
}
