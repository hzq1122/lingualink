package com.lingualink.translation

import com.lingualink.domain.model.TranslationMode
import com.lingualink.domain.model.TranslationRequest
import com.lingualink.domain.model.TranslationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OnlineTranslationEngine : TranslationEngine {

    override val mode = TranslationMode.ONLINE

    var apiEndpoint: String = ""
    var apiKey: String = ""
    var selectedModel: String = "deepseek-chat"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    override suspend fun translate(request: TranslationRequest): TranslationResult =
        withContext(Dispatchers.IO) {
            val targetLangName = SupportedLanguage.fromCode(request.targetLang)?.displayName
                ?: request.targetLang

            val body = json.encodeToString(ChatRequest(
                model = selectedModel,
                messages = listOf(
                    ChatMsg("system", SYSTEM_PROMPT),
                    ChatMsg("user", "Translate to $targetLangName:\n\n${request.text}")
                ),
                temperature = 0.3f,
                max_tokens = 2048
            ))

            val startTime = System.currentTimeMillis()
            val httpRequest = Request.Builder()
                .url("$apiEndpoint/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body.toRequestBody(JSON_TYPE))
                .build()

            val response = client.newCall(httpRequest).execute()
            val latency = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                throw Exception("API error: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            val chatResponse = json.decodeFromString<ChatResponse>(responseBody)
            val translated = chatResponse.choices.firstOrNull()?.message?.content?.trim()
                ?: throw Exception("No translation in response")

            TranslationResult(
                translatedText = translated,
                sourceLang = request.sourceLang,
                targetLang = request.targetLang,
                latencyMs = latency,
                mode = TranslationMode.ONLINE
            )
        }

    suspend fun fetchModels(): List<String> = withContext(Dispatchers.IO) {
        if (apiEndpoint.isBlank() || apiKey.isBlank()) {
            throw Exception("请先填写 API 端点和 Key")
        }
        val httpRequest = Request.Builder()
            .url("$apiEndpoint/v1/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()

        val response = client.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            throw Exception("获取模型列表失败: ${response.code}")
        }

        val body = response.body?.string() ?: throw Exception("Empty response")
        val modelsResponse = json.decodeFromString<ModelsResponse>(body)
        modelsResponse.data.map { it.id }.sorted()
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

@Serializable
private data class ModelsResponse(val data: List<ModelItem>)

@Serializable
private data class ModelItem(val id: String)
