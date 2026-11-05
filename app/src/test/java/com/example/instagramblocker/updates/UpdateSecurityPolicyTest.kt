package com.example.instagramblocker.updates

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSecurityPolicyTest {
    @Test fun allowsOnlyOfficialApiEndpointHost() {
        assertTrue(UpdateSecurityPolicy.isAllowedApiUrl("https://api.github.com/repos/YkDLS/InstBlock/releases/latest"))
        assertFalse(UpdateSecurityPolicy.isAllowedApiUrl("https://api.github.com.evil.example/releases/latest"))
        assertFalse(UpdateSecurityPolicy.isAllowedApiUrl("http://api.github.com/releases/latest"))
        assertFalse(UpdateSecurityPolicy.isAllowedApiUrl("https://api.github.com:444/releases/latest"))
    }

    @Test fun rejectsAmbiguousOrUntrustedDownloadUrls() {
        assertTrue(UpdateSecurityPolicy.isAllowedDownloadUrl("https://github.com/YkDLS/InstBlock/releases/download/v1/app.apk"))
        assertTrue(UpdateSecurityPolicy.isAllowedDownloadUrl("https://release-assets.githubusercontent.com/file.apk"))
        assertFalse(UpdateSecurityPolicy.isAllowedDownloadUrl("https://github.com.evil.example/app.apk"))
        assertFalse(UpdateSecurityPolicy.isAllowedDownloadUrl("https://github.com@evil.example/app.apk"))
        assertFalse(UpdateSecurityPolicy.isAllowedDownloadUrl("https://raw.githubusercontent.com/app.apk"))
        assertFalse(UpdateSecurityPolicy.isAllowedDownloadUrl("https://github.com/app.apk#fragment"))
    }

    @Test fun acceptsUpgradeAndRejectsDowngradeOrMalformedVersion() {
        assertTrue(UpdateSecurityPolicy.isNewerVersion("1.2.0", "1.1.2"))
        assertTrue(UpdateSecurityPolicy.isNewerVersion("v2.0.0", "1.9.9"))
        assertFalse(UpdateSecurityPolicy.isNewerVersion("1.1.2", "1.2.0"))
        assertFalse(UpdateSecurityPolicy.isNewerVersion("not-a-version", "1.2.0"))
        assertFalse(UpdateSecurityPolicy.isNewerVersion("999999999999999999999", "1.2.0"))
    }
}
