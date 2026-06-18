package com.ai.assist.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppMatcherTest {
    private val apps = listOf(
        InstalledApp("Camera Assistant", "com.samsung.android.app.cameraassistant"),
        InstalledApp("Camera", "com.sec.android.app.camera"),
        InstalledApp("Settings", "com.android.settings"),
        InstalledApp("YouTube", "com.google.android.youtube"),
        InstalledApp("Google", "com.google.android.googlequicksearchbox"),
    )

    @Test
    fun cameraPrefersKnownCameraPackageOverCameraAssistant() {
        val match = AppMatcher.bestMatch("camera", apps)

        assertNotNull(match)
        assertEquals("com.sec.android.app.camera", match?.app?.packageName)
    }

    @Test
    fun koreanCameraQueryPrefersKnownCameraPackage() {
        val match = AppMatcher.bestMatch("1분 뒤에 카메라를 실행하라", apps)

        assertNotNull(match)
        assertEquals("com.sec.android.app.camera", match?.app?.packageName)
    }

    @Test
    fun koreanSettingsQueryMatchesSettings() {
        val match = AppMatcher.bestMatch("설정 열어줘", apps)

        assertNotNull(match)
        assertEquals("com.android.settings", match?.app?.packageName)
    }

    @Test
    fun youtubeQueryMatchesYoutube() {
        val match = AppMatcher.bestMatch("유튜브 실행", apps)

        assertNotNull(match)
        assertEquals("com.google.android.youtube", match?.app?.packageName)
    }
}
