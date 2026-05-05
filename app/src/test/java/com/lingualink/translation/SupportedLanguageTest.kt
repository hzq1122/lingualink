package com.lingualink.translation

import org.junit.Assert.*
import org.junit.Test

class SupportedLanguageTest {

    @Test
    fun `fromCode returns correct language for valid codes`() {
        assertEquals(SupportedLanguage.ZH, SupportedLanguage.fromCode("zh"))
        assertEquals(SupportedLanguage.EN, SupportedLanguage.fromCode("en"))
        assertEquals(SupportedLanguage.JA, SupportedLanguage.fromCode("ja"))
        assertEquals(SupportedLanguage.AUTO, SupportedLanguage.fromCode("auto"))
    }

    @Test
    fun `fromCode returns null for unknown code`() {
        assertNull(SupportedLanguage.fromCode("xx"))
        assertNull(SupportedLanguage.fromCode(""))
        assertNull(SupportedLanguage.fromCode("ZH")) // case-sensitive
    }

    @Test
    fun `onlineLanguages excludes AUTO`() {
        val online = SupportedLanguage.onlineLanguages()
        assertFalse(online.contains(SupportedLanguage.AUTO))
        assertTrue(online.contains(SupportedLanguage.ZH))
        assertTrue(online.contains(SupportedLanguage.EN))
    }

    @Test
    fun `offlineLanguages only includes offline-supported languages`() {
        val offline = SupportedLanguage.offlineLanguages()
        assertFalse(offline.contains(SupportedLanguage.AUTO))
        // ZH, EN, JA, KO are marked as offline-supported
        assertTrue(offline.contains(SupportedLanguage.ZH))
        assertTrue(offline.contains(SupportedLanguage.EN))
        assertTrue(offline.contains(SupportedLanguage.JA))
        assertTrue(offline.contains(SupportedLanguage.KO))
    }

    @Test
    fun `priorityCodes contains expected languages`() {
        val priorities = SupportedLanguage.priorityCodes
        assertTrue(priorities.contains("zh"))
        assertTrue(priorities.contains("en"))
        assertTrue(priorities.contains("ja"))
        assertTrue(priorities.contains("ko"))
    }

    @Test
    fun `all languages have non-empty display names`() {
        SupportedLanguage.entries.forEach { lang ->
            assertTrue("${lang.code} displayName is empty", lang.displayName.isNotBlank())
            assertTrue("${lang.code} localName is empty", lang.localName.isNotBlank())
        }
    }

    @Test
    fun `all language codes are unique`() {
        val codes = SupportedLanguage.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }
}
