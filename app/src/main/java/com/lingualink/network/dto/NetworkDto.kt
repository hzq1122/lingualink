package com.lingualink.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class MulticastDto(
    val alias: String,
    val deviceModel: String,
    val fingerprint: String,
    val port: Int,
    val announcement: Boolean,
    val version: String = "1.0"
)

@Serializable
data class DeviceInfoDto(
    val alias: String,
    val deviceModel: String,
    val fingerprint: String,
    val port: Int,
    val version: String = "1.0"
)

@Serializable
data class TranslateRequestDto(
    val requestId: String,
    val senderId: String,
    val senderAlias: String,
    val text: String,
    val sourceLang: String,
    val targetLang: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class TranslateResultDto(
    val requestId: String,
    val senderId: String,
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val latencyMs: Long,
    val translationMode: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class PingResponse(val timestamp: Long)

@Serializable
data class ErrorResponse(val error: String)
