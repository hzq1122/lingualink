package com.lingualink.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val ip: String,
    val alias: String,
    val deviceModel: String,
    val fingerprint: String,
    val port: Int = DEFAULT_PORT,
    val role: DeviceRole = DeviceRole.GUEST
) {
    companion object {
        const val DEFAULT_PORT = 53318
    }
}

@Serializable
enum class DeviceRole { HOST, GUEST }

@Serializable
data class TranslationSession(
    val sessionId: String,
    val hostDevice: DeviceInfo,
    val participants: List<DeviceInfo> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val status: SessionStatus = SessionStatus.WAITING
)

@Serializable
enum class SessionStatus { WAITING, ACTIVE, CLOSED }

data class TranslationRequest(
    val text: String,
    val sourceLang: String,
    val targetLang: String
)

data class TranslationResult(
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val latencyMs: Long,
    val mode: TranslationMode
)

enum class TranslationMode { ONLINE, OFFLINE }
