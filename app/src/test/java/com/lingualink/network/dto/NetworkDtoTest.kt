package com.lingualink.network.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class NetworkDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `MulticastDto serialization roundtrip`() {
        val dto = MulticastDto(
            alias = "Pixel 7",
            deviceModel = "Pixel 7",
            fingerprint = "abc123",
            port = 53318,
            announcement = true
        )
        val encoded = json.encodeToString(MulticastDto.serializer(), dto)
        val decoded = json.decodeFromString(MulticastDto.serializer(), encoded)

        assertEquals(dto.alias, decoded.alias)
        assertEquals(dto.deviceModel, decoded.deviceModel)
        assertEquals(dto.fingerprint, decoded.fingerprint)
        assertEquals(dto.port, decoded.port)
        assertEquals(dto.announcement, decoded.announcement)
        assertEquals("1.0", decoded.version)
    }

    @Test
    fun `TranslateRequestDto serialization roundtrip`() {
        val dto = TranslateRequestDto(
            requestId = "req-1",
            senderId = "device-a",
            senderAlias = "Alice",
            text = "你好",
            sourceLang = "zh",
            targetLang = "en"
        )
        val encoded = json.encodeToString(TranslateRequestDto.serializer(), dto)
        val decoded = json.decodeFromString(TranslateRequestDto.serializer(), encoded)

        assertEquals(dto.requestId, decoded.requestId)
        assertEquals(dto.text, decoded.text)
        assertEquals(dto.sourceLang, decoded.sourceLang)
        assertEquals(dto.targetLang, decoded.targetLang)
    }

    @Test
    fun `TranslateResultDto serialization roundtrip`() {
        val dto = TranslateResultDto(
            requestId = "req-1",
            senderId = "device-b",
            originalText = "你好",
            translatedText = "Hello",
            sourceLang = "zh",
            targetLang = "en",
            latencyMs = 150,
            translationMode = "online"
        )
        val encoded = json.encodeToString(TranslateResultDto.serializer(), dto)
        val decoded = json.decodeFromString(TranslateResultDto.serializer(), encoded)

        assertEquals(dto.translatedText, decoded.translatedText)
        assertEquals(dto.latencyMs, decoded.latencyMs)
        assertEquals(dto.translationMode, decoded.translationMode)
    }

    @Test
    fun `DeviceInfoDto default version is 1_0`() {
        val dto = DeviceInfoDto(
            alias = "Test",
            deviceModel = "Model",
            fingerprint = "fp1",
            port = 53318
        )
        assertEquals("1.0", dto.version)
    }

    @Test
    fun `PingResponse serialization`() {
        val dto = PingResponse(timestamp = 1234567890L)
        val encoded = json.encodeToString(PingResponse.serializer(), dto)
        val decoded = json.decodeFromString(PingResponse.serializer(), encoded)
        assertEquals(1234567890L, decoded.timestamp)
    }

    @Test
    fun `ErrorResponse serialization`() {
        val dto = ErrorResponse(error = "something went wrong")
        val encoded = json.encodeToString(ErrorResponse.serializer(), dto)
        val decoded = json.decodeFromString(ErrorResponse.serializer(), encoded)
        assertEquals("something went wrong", decoded.error)
    }
}
