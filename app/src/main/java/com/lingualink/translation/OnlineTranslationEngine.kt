package com.lingualink.translation

import android.util.Log
import com.lingualink.domain.model.TranslationMode
import com.lingualink.domain.model.TranslationRequest
import com.lingualink.domain.model.TranslationResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

class OnlineTranslationEngine(
    private val httpClient: HttpClient
) : TranslationEngine {

    override val mode = TranslationMode.ONLINE

    var apiEndpoint: String = ""
    var apiKey: String = ""
    var selectedModel: String = "deepseek-chat"

    override suspend fun translate(request: TranslationRequest): TranslationResult {
        val targetLangName = SupportedLanguage.fromCode(request.targetLang)?.displayName
            ?: request.targetLang

        val startTime = System.currentTimeMillis()
        val response = httpClient.post("$apiEndpoint/v1/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                model = selectedModel,
                messages = listOf(
                    ChatMsg("system", SYSTEM_PROMPT),
                    ChatMsg("user", "Translate to $targetLangName:\n\n${request.text}")
                ),
                temperature = 0.3f,
                max_tokens = 2048
            ))
        }.body<ChatResponse>()

        val latency = System.currentTimeMillis() - startTime
        val translated = response.choices.firstOrNull()?.message?.content?.trim()
            ?: throw Exception("Empty response from LLM")

        return TranslationResult(
            translatedText = translated,
            sourceLang = request.sourceLang,
            targetLang = request.targetLang,
            latencyMs = latency,
            mode = TranslationMode.ONLINE
        )
    }

    override suspend fun detectLanguage(text: String): String = "auto"

    override fun isAvailable(): Boolean =
        apiEndpoint.isNotBlank() && apiKey.isNotBlank()

    companion object {
        const val SYSTEM_PROMPT = """You are a professional multilingual translator.
Translate the user's text accurately into the specified target language.
Preserve tone, style, and formatting.
For Japanese use polite form (です/ます) by default.
Output ONLY the translated text, no explanations."""
    }
}

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMsg>,
    val temperature: Float,
    val max_tokens: Int
)

@Serializable
private data class ChatMsg(val role: String, val content: String)

@Serializable
private data class ChatResponse(val choices: List<Choice>)

@Serializable
private data class Choice(val message: ChatMsg)
