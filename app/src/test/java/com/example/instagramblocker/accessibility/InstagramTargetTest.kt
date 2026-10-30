package com.example.instagramblocker.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramTargetTest {
    @Test
    fun onlyOfficialInstagramPackageMatches() {
        assertTrue(InstagramTarget.matches("com.instagram.android"))
        assertFalse(InstagramTarget.matches("com.android.chrome"))
        assertFalse(InstagramTarget.matches("com.example.instagramblocker"))
        assertFalse(InstagramTarget.matches(null))
    }
}
