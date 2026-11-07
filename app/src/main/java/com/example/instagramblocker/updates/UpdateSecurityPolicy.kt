package com.example.instagramblocker.updates

import java.net.URI

internal object UpdateSecurityPolicy {
    const val MAX_APK_BYTES = 200L * 1024L * 1024L
    const val MAX_METADATA_BYTES = 1024L * 1024L
    const val MAX_REDIRECTS = 5

    private val downloadHosts = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
        "github-releases.githubusercontent.com",
    )

    fun isAllowedApiUrl(url: String): Boolean = validHttpsUri(url)?.let { uri ->
        uri.host == "api.github.com" &&
            uri.rawPath == "/repos/YkDLS/InstBlock/releases/latest" &&
            uri.rawQuery == null
    } ?: false

    fun isAllowedReleaseAssetUrl(url: String): Boolean = validHttpsUri(url)?.let { uri ->
        val prefix = "/YkDLS/InstBlock/releases/download/"
        val segments = uri.rawPath.removePrefix(prefix).split('/')
        uri.host == "github.com" &&
            uri.rawPath.startsWith(prefix) &&
            segments.size == 2 && segments.all { it.isNotEmpty() } &&
            uri.rawQuery == null
    } ?: false

    fun isAllowedDownloadUrl(url: String): Boolean = validHttpsUri(url)?.host in downloadHosts

    fun isNewerVersionCode(candidate: Long, installed: Long): Boolean =
        candidate > installed

    fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = versionParts(remote) ?: return false
        val localParts = versionParts(local) ?: return false
        val size = maxOf(remoteParts.size, localParts.size)
        return (0 until size).firstNotNullOfOrNull { index ->
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val localPart = localParts.getOrElse(index) { 0 }
            when {
                remotePart > localPart -> true
                remotePart < localPart -> false
                else -> null
            }
        } ?: false
    }

    private fun validHttpsUri(url: String): URI? = runCatching { URI(url) }.getOrNull()
        ?.takeIf { uri ->
            uri.scheme == "https" &&
                uri.userInfo == null &&
                uri.host != null &&
                (uri.port == -1 || uri.port == 443) &&
                uri.fragment == null
        }

    private fun versionParts(version: String): List<Int>? {
        val normalized = version.removePrefix("v")
        if (!normalized.matches(Regex("[0-9]+(?:\\.[0-9]+){0,3}(?:[-+][0-9A-Za-z.-]+)?"))) return null
        return normalized.substringBefore('-').substringBefore('+').split('.').map { part ->
            part.toIntOrNull() ?: return null
        }
    }
}
