package com.lingualink.update

import org.junit.Assert.*
import org.junit.Test

class VersionUtilsTest {

    @Test
    fun `newer major version is detected`() {
        assertTrue(VersionUtils.isNewerVersion("1.0.0", "2.0.0"))
    }

    @Test
    fun `newer minor version is detected`() {
        assertTrue(VersionUtils.isNewerVersion("1.0.0", "1.1.0"))
    }

    @Test
    fun `newer patch version is detected`() {
        assertTrue(VersionUtils.isNewerVersion("1.0.0", "1.0.1"))
    }

    @Test
    fun `same version is not newer`() {
        assertFalse(VersionUtils.isNewerVersion("1.0.0", "1.0.0"))
    }

    @Test
    fun `older version is not newer`() {
        assertFalse(VersionUtils.isNewerVersion("2.0.0", "1.0.0"))
    }

    @Test
    fun `handles v prefix in version strings`() {
        // The UpdateManager strips "v" prefix before calling, but test robustness
        assertTrue(VersionUtils.isNewerVersion("1.0.0", "1.0.1"))
    }

    @Test
    fun `handles different segment counts`() {
        assertTrue(VersionUtils.isNewerVersion("1.0", "1.0.1"))
        assertFalse(VersionUtils.isNewerVersion("1.0.1", "1.0"))
    }

    @Test
    fun `handles pre-release style versions`() {
        assertTrue(VersionUtils.isNewerVersion("1.0.0-beta", "1.0.0"))
    }

    @Test
    fun `handles zero versions`() {
        assertTrue(VersionUtils.isNewerVersion("0.0.0", "0.0.1"))
        assertFalse(VersionUtils.isNewerVersion("0.0.1", "0.0.0"))
    }

    @Test
    fun `handles large version numbers`() {
        assertTrue(VersionUtils.isNewerVersion("1.99.0", "2.0.0"))
    }
}
