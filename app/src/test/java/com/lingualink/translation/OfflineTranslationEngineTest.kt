package com.lingualink.translation

import com.lingualink.domain.model.TranslationMode
import com.lingualink.domain.model.TranslationRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OfflineTranslationEngineTest {

    private lateinit var engine: OfflineTranslationEngine

    @Before
    fun setup() {
        engine = OfflineTranslationEngine()
    }

    @Test
    fun `mode is OFFLINE`() {
        assertEquals(TranslationMode.OFFLINE, engine.mode)
    }

    @Test
    fun `isAvailable always returns true`() {
        assertTrue(engine.isAvailable())
    }

    @Test
    fun `translate zh to en for known word`() = runTest {
        val result = engine.translate(TranslationRequest("你好", "zh", "en"))
        assertEquals("Hello", result.translatedText)
        assertEquals("zh", result.sourceLang)
        assertEquals("en", result.targetLang)
        assertEquals(TranslationMode.OFFLINE, result.mode)
        assertTrue(result.latencyMs >= 0)
    }

    @Test
    fun `translate en to zh for known word`() = runTest {
        val result = engine.translate(TranslationRequest("Hello", "en", "zh"))
        assertEquals("你好", result.translatedText)
    }

    @Test
    fun `translate zh to ja for known word`() = runTest {
        val result = engine.translate(TranslationRequest("你好", "zh", "ja"))
        assertEquals("こんにちは", result.translatedText)
    }

    @Test
    fun `translate en to ja for known word`() = runTest {
        val result = engine.translate(TranslationRequest("hello", "en", "ja"))
        assertEquals("こんにちは", result.translatedText)
    }

    @Test
    fun `translate ja to zh for known word`() = runTest {
        val result = engine.translate(TranslationRequest("こんにちは", "ja", "zh"))
        assertEquals("你好", result.translatedText)
    }

    @Test
    fun `translate multiple words`() = runTest {
        val result = engine.translate(TranslationRequest("早上好", "zh", "en"))
        assertEquals("Good morning", result.translatedText)
    }

    @Test
    fun `translate word by word for known words`() = runTest {
        // "大" and "水" are both in the dictionary
        val result = engine.translate(TranslationRequest("大 水", "zh", "en"))
        assertEquals("Big Water", result.translatedText)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `translate throws for unsupported language pair`() = runTest {
        engine.translate(TranslationRequest("hello", "en", "fr"))
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `translate throws for unknown text`() = runTest {
        engine.translate(TranslationRequest("xyzabc123", "zh", "en"))
    }

    @Test
    fun `detectLanguage returns auto`() = runTest {
        assertEquals("auto", engine.detectLanguage("任何文本"))
    }

    @Test
    fun `translate zh to ko for known word`() = runTest {
        val result = engine.translate(TranslationRequest("你好", "zh", "ko"))
        assertEquals("안녕하세요", result.translatedText)
    }

    @Test
    fun `translate ko to zh for known word`() = runTest {
        val result = engine.translate(TranslationRequest("안녕하세요", "ko", "zh"))
        assertEquals("你好", result.translatedText)
    }

    @Test
    fun `translate zh to ko for thanks`() = runTest {
        val result = engine.translate(TranslationRequest("谢谢", "zh", "ko"))
        assertEquals("감사합니다", result.translatedText)
    }
}
