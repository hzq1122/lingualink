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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OfflineTranslationEngine(
    private val context: Context
) : TranslationEngine {

    override val mode = TranslationMode.OFFLINE
    private val downloadedLanguages = mutableSetOf<String>()

    override suspend fun translate(request: TranslationRequest): TranslationResult {
        val sourceLang = if (request.sourceLang == "auto") detectLanguage(request.text) else request.sourceLang

        val source = languageToMlKit(sourceLang)
            ?: throw IllegalArgumentException("Unsupported language: $sourceLang")
        val target = languageToMlKit(request.targetLang)
            ?: throw IllegalArgumentException("Unsupported language: ${request.targetLang}")

        ensureModelDownloaded(sourceLang)
        ensureModelDownloaded(request.targetLang)

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val translator = Translation.getClient(options)

        val startTime = System.currentTimeMillis()
        val result = translator.translate(request.text).awaitAny()
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
        return suspendCancellableCoroutine { cont ->
            detector.identifyLanguage(text)
                .addOnSuccessListener { langCode ->
                    cont.resume(langCode)
                }
                .addOnFailureListener { e ->
                    cont.resume("en")
                }
        }
    }

    override fun isAvailable(): Boolean = true

    private suspend fun ensureModelDownloaded(langCode: String) {
        if (langCode in downloadedLanguages) return
        val mlKitLang = languageToMlKit(langCode) ?: return

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(mlKitLang)
            .setTargetLanguage(mlKitLang)
            .build()
        val translator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder().build()

        translator.downloadModelIfNeeded(conditions).awaitAny()
        downloadedLanguages.add(langCode)
    }

    /** Map BCP-47 codes to ML Kit language string constants */
    private fun languageToMlKit(code: String): String? {
        return when (code) {
            "zh" -> TranslateLanguage.CHINESE
            "en" -> TranslateLanguage.ENGLISH
            "ja" -> TranslateLanguage.JAPANESE
            "ko" -> TranslateLanguage.KOREAN
            "fr" -> TranslateLanguage.FRENCH
            "de" -> TranslateLanguage.GERMAN
            "es" -> TranslateLanguage.SPANISH
            "ru" -> TranslateLanguage.RUSSIAN
            "pt" -> TranslateLanguage.PORTUGUESE
            "it" -> TranslateLanguage.ITALIAN
            "ar" -> TranslateLanguage.ARABIC
            "th" -> TranslateLanguage.THAI
            "vi" -> TranslateLanguage.VIETNAMESE
            "nl" -> TranslateLanguage.DUTCH
            "pl" -> TranslateLanguage.POLISH
            "tr" -> TranslateLanguage.TURKISH
            "id" -> TranslateLanguage.INDONESIAN
            "uk" -> TranslateLanguage.UKRAINIAN
            "hi" -> TranslateLanguage.HINDI
            else -> null
        }
    }
}

/** Extension to await any Task<T> as a suspend function */
suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitAny(): T {
    return suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { e ->
            if (cont.isActive) cont.resumeWithException(e)
        }
    }
}
