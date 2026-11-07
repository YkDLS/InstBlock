package com.example.instagramblocker.updates

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.instagramblocker.BuildConfig
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.json.JSONObject

class UpdateChecker(private val context: Context) {
    suspend fun findAvailableUpdate(): UpdateInfo? {
        val json = requestText(API_URL)
        val release = JSONObject(json)
        val version = release.optString("tag_name").removePrefix("v")
        if (!UpdateSecurityPolicy.isNewerVersion(version, BuildConfig.VERSION_NAME)) return null

        val assets = release.optJSONArray("assets") ?: return null
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            val name = asset.optString("name")
            val url = asset.optString("browser_download_url")
            if (name.endsWith(".apk", ignoreCase = true) && UpdateSecurityPolicy.isAllowedReleaseAssetUrl(url)) {
                return UpdateInfo(version, url, name)
            }
        }
        return null
    }

    suspend fun download(update: UpdateInfo): File {
        require(UpdateSecurityPolicy.isAllowedReleaseAssetUrl(update.downloadUrl)) { "Fonte de atualização não permitida" }
        val updateDir = File(context.cacheDir, UPDATE_DIRECTORY).apply { mkdirs() }
        val output = File(updateDir, UPDATE_FILE_NAME)
        val partial = File(updateDir, "$UPDATE_FILE_NAME.part")
        partial.delete()
        val connection = openFollowingRedirects(
            update.downloadUrl,
            UpdateSecurityPolicy::isAllowedDownloadUrl,
        )
        try {
            check(connection.responseCode in 200..299) { "Download HTTP ${connection.responseCode}" }
            val contentLength = connection.contentLengthLong
            check(contentLength < 0L || contentLength <= UpdateSecurityPolicy.MAX_APK_BYTES) {
                "APK excede o limite permitido"
            }
            connection.inputStream.use { input ->
                partial.outputStream().use { destination ->
                    copyLimited(input, destination, UpdateSecurityPolicy.MAX_APK_BYTES)
                }
            }
            check(isTrustedUpdate(partial)) { "APK não pertence a este aplicativo ou possui assinatura diferente" }
            output.delete()
            check(partial.renameTo(output)) { "Não foi possível finalizar o APK" }
            return output
        } catch (error: Exception) {
            partial.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun openInstaller(apk: File) {
        val updateDir = File(context.cacheDir, UPDATE_DIRECTORY).canonicalFile
        val canonicalApk = apk.canonicalFile
        require(canonicalApk.parentFile == updateDir && canonicalApk.name == UPDATE_FILE_NAME) {
            "Arquivo de atualização fora do diretório permitido"
        }
        require(isTrustedUpdate(canonicalApk)) { "APK de atualização não confiável" }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", canonicalApk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun canInstallPackages(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun requestText(url: String): String {
        require(UpdateSecurityPolicy.isAllowedApiUrl(url)) { "API de atualização não permitida" }
        val connection = openFollowingRedirects(url, UpdateSecurityPolicy::isAllowedApiUrl)
        try {
            check(connection.responseCode in 200..299) { "GitHub HTTP ${connection.responseCode}" }
            val length = connection.contentLengthLong
            check(length < 0L || length <= UpdateSecurityPolicy.MAX_METADATA_BYTES) {
                "Resposta de atualização muito grande"
            }
            return connection.inputStream.use { input ->
                readLimited(input, UpdateSecurityPolicy.MAX_METADATA_BYTES)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "InstagramBlocker/${BuildConfig.VERSION_NAME}")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        return connection
    }

    private fun openFollowingRedirects(
        initialUrl: String,
        isAllowed: (String) -> Boolean,
    ): HttpURLConnection {
        var currentUrl = initialUrl
        repeat(UpdateSecurityPolicy.MAX_REDIRECTS + 1) { redirects ->
            require(isAllowed(currentUrl)) { "Redirecionamento para fonte não permitida" }
            val connection = openConnection(currentUrl)
            val status = connection.responseCode
            if (status !in 300..399) return connection
            val location = connection.getHeaderField("Location")
                ?: run {
                    connection.disconnect()
                    error("Redirecionamento sem destino")
                }
            connection.disconnect()
            check(redirects < UpdateSecurityPolicy.MAX_REDIRECTS) { "Redirecionamentos em excesso" }
            currentUrl = URL(URL(currentUrl), location).toString()
        }
        error("Redirecionamentos em excesso")
    }

    private fun copyLimited(input: InputStream, output: java.io.OutputStream, limit: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) return
            total += count
            check(total <= limit) { "Download excede o limite permitido" }
            output.write(buffer, 0, count)
        }
    }

    private fun readLimited(input: InputStream, limit: Long): String {
        val output = java.io.ByteArrayOutputStream()
        copyLimited(input, output, limit)
        return output.toString(Charsets.UTF_8.name())
    }

    private fun isTrustedUpdate(apk: File): Boolean {
        val archive = packageInfo(apk.absolutePath) ?: return false
        if (archive.packageName != context.packageName) return false
        val installed = packageInfo(context.packageName, installed = true) ?: return false
        if (!UpdateSecurityPolicy.isNewerVersionCode(archive.versionCodeCompat(), installed.versionCodeCompat())) return false
        val archiveCertificates = certificateDigests(archive.signaturesCompat())
        val installedCertificates = certificateDigests(installed.signaturesCompat())
        return archiveCertificates.isNotEmpty() && archiveCertificates == installedCertificates
    }

    @Suppress("DEPRECATION")
    private fun packageInfo(pathOrPackage: String, installed: Boolean = false): android.content.pm.PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return if (installed) {
            runCatching { context.packageManager.getPackageInfo(pathOrPackage, flags) }.getOrNull()
        } else {
            context.packageManager.getPackageArchiveInfo(pathOrPackage, flags)
        }
    }

    @Suppress("DEPRECATION")
    private fun android.content.pm.PackageInfo.signaturesCompat(): List<Signature> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingInfo?.apkContentsSigners.orEmpty().toList()
        } else {
            signatures.orEmpty().toList()
        }

    @Suppress("DEPRECATION")
    private fun android.content.pm.PackageInfo.versionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()

    private fun certificateDigests(signatures: List<Signature>): Set<String> = signatures.map { signature ->
        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { byte ->
            "%02x".format(byte)
        }
    }.toSet()

    private companion object {
        const val API_URL = "https://api.github.com/repos/YkDLS/InstBlock/releases/latest"
        const val APK_MIME = "application/vnd.android.package-archive"
        const val UPDATE_DIRECTORY = "updates"
        const val UPDATE_FILE_NAME = "instagram-blocker-update.apk"
    }
}
