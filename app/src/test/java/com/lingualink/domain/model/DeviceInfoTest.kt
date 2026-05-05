package com.lingualink.domain.model

import org.junit.Assert.*
import org.junit.Test

class DeviceInfoTest {

    @Test
    fun `default port is 53318`() {
        val device = DeviceInfo(
            ip = "192.168.1.1",
            alias = "Test",
            deviceModel = "Pixel 7",
            fingerprint = "abc123"
        )
        assertEquals(53318, device.port)
    }

    @Test
    fun `default role is GUEST`() {
        val device = DeviceInfo(
            ip = "192.168.1.1",
            alias = "Test",
            deviceModel = "Pixel 7",
            fingerprint = "abc123"
        )
        assertEquals(DeviceRole.GUEST, device.role)
    }

    @Test
    fun `custom port and role`() {
        val device = DeviceInfo(
            ip = "192.168.1.1",
            alias = "Host",
            deviceModel = "Samsung S24",
            fingerprint = "def456",
            port = 8080,
            role = DeviceRole.HOST
        )
        assertEquals(8080, device.port)
        assertEquals(DeviceRole.HOST, device.role)
    }

    @Test
    fun `TranslationRequest stores fields correctly`() {
        val req = TranslationRequest(text = "hello", sourceLang = "en", targetLang = "zh")
        assertEquals("hello", req.text)
        assertEquals("en", req.sourceLang)
        assertEquals("zh", req.targetLang)
    }

    @Test
    fun `TranslationResult stores fields correctly`() {
        val result = TranslationResult(
            translatedText = "你好",
            sourceLang = "en",
            targetLang = "zh",
            latencyMs = 150,
            mode = TranslationMode.ONLINE
        )
        assertEquals("你好", result.translatedText)
        assertEquals(150, result.latencyMs)
        assertEquals(TranslationMode.ONLINE, result.mode)
    }

    @Test
    fun `TranslationSession default values`() {
        val device = DeviceInfo("10.0.0.1", "A", "Model", "fp1")
        val session = TranslationSession(
            sessionId = "s1",
            hostDevice = device
        )
        assertEquals(SessionStatus.WAITING, session.status)
        assertTrue(session.participants.isEmpty())
        assertTrue(session.createdAt > 0)
    }
}
