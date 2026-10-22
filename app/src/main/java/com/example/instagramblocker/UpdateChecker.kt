package com.example.instagramblocker

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val assetName: String,
)

class UpdateChecker(private val context: Context) {
    suspend fun findAvailableUpdate(): UpdateInfo? {
        val json = requestText(API_URL)
        val release = JSONObject(json)
        val version = release.optString("tag_name").removePrefix("v")
        if (!isNewer(version, BuildConfig.VERSION_NAME)) return null

        val assets = release.optJSONArray("assets") ?: return null
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            val name = asset.optString("name")
            val url = asset.optString("browser_download_url")
            if (name.endsWith(".apk", ignoreCase = true) && isAllowedDownload(url)) {
                return UpdateInfo(version, url, name)
            }
        }
        return null
    }

    suspend fun download(update: UpdateInfo): File {
        require(isAllowedDownload(update.downloadUrl)) { "Fonte de atualização não permitida" }
        val output = File(context.cacheDir, "instagram-blocker-${update.version}.apk")
        val connection = openConnection(update.downloadUrl)
        try {
            check(connection.responseCode in 200..299) { "Download HTTP ${connection.responseCode}" }
            connection.inputStream.use { input -> output.outputStream().use { input.copyTo(it) } }
            return output
        } finally {
            connection.disconnect()
        }
    }

    fun openInstaller(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun requestText(url: String): String {
        val connection = openConnection(url)
        try {
            check(connection.responseCode in 200..299) { "GitHub HTTP ${connection.responseCode}" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "InstagramBlocker/${BuildConfig.VERSION_NAME}")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        return connection
    }

    private fun isAllowedDownload(url: String): Boolean {
        val host = Uri.parse(url).host.orEmpty()
        return url.startsWith("https://") &&
            (host == "github.com" || host.endsWith(".github.com") || host.endsWith(".githubusercontent.com"))
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val remoteParts = remote.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        val localParts = local.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
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

    private companion object {
        const val API_URL = "https://api.github.com/repos/YkDLS/InstagramBlocker/releases/latest"
        const val APK_MIME = "application/vnd.android.package-archive"
    }
}
