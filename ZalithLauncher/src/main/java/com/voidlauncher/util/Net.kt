package com.voidlauncher.util

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min

class DownloadException(message: String) : IOException(message)

/** Runtime-tunable download behaviour, set from VOID's persisted settings. */
object NetConfig {
    @Volatile var maxAttempts: Int = 3
    @Volatile var parallelDownloads: Int = 4
    @Volatile var verifyDownloads: Boolean = true
}

object Net {
    private const val USER_AGENT = "VOIDLauncher/1.0 (Android; +https://github.com/ddarknessbg22-creator/void-launcher)"
    private const val CONNECT_TIMEOUT = 20_000
    private const val READ_TIMEOUT = 30_000

    /** Plain text/JSON fetch. Called from an IO dispatcher. */
    fun fetchText(url: String): String {
        var lastError: Exception? = null
        repeat(NetConfig.maxAttempts.coerceIn(1, 6)) { attempt ->
            var connection: HttpURLConnection? = null
            try {
                connection = open(url)
                val stream = connection.inputStream
                val result = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (result.isBlank()) throw DownloadException("Empty response from $url")
                return result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                if (attempt < NetConfig.maxAttempts - 1) Thread.sleep(400L * (1 shl attempt))
            } finally {
                connection?.disconnect()
            }
        }
        throw DownloadException("Could not fetch $url: ${lastError?.message ?: "unknown error"}")
    }

    /**
     * Streams a file to [destination] with retry, checksum verification and atomic commit.
     * Nothing is ever marked installed unless the SHA-1 (when supplied) matches exactly.
     */
    fun downloadToFile(
        url: String,
        destination: File,
        expectedSha1: String?,
        onProgress: (bytesDone: Long, bytesTotal: Long) -> Unit = { _, _ -> },
        isCancelled: () -> Boolean = { false },
    ) {
        if (isCancelled()) throw CancellationException()
        val expected = if (NetConfig.verifyDownloads) expectedSha1 else null
        if (destination.exists() && expected != null && sha1(destination).equals(expected, true)) {
            onProgress(destination.length(), destination.length())
            return
        }

        destination.parentFile?.mkdirs()
        val partial = File(destination.parentFile, "${destination.name}.part")
        if (partial.exists()) partial.delete()

        var lastError: Exception? = null
        repeat(NetConfig.maxAttempts.coerceIn(1, 6)) { attempt ->
            var connection: HttpURLConnection? = null
            try {
                if (isCancelled()) throw CancellationException()
                connection = open(url)
                val total = connection.contentLengthLong
                val digest = MessageDigest.getInstance("SHA-1")
                var done = 0L
                var lastReport = 0L

                BufferedInputStream(connection.inputStream, 64 * 1024).use { input ->
                    FileOutputStream(partial).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            if (isCancelled()) throw CancellationException()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            done += read
                            if (done - lastReport >= 64 * 1024 || total <= 0) {
                                lastReport = done
                                onProgress(done, if (total > 0) total else done)
                            }
                        }
                    }
                }

                if (isCancelled()) throw CancellationException()
                if (expected != null) {
                    val actual = digest.digest().toHex()
                    if (!actual.equals(expected, true)) {
                        partial.delete()
                        throw DownloadException("Checksum mismatch for ${destination.name}: expected $expected, got $actual")
                    }
                }
                if (destination.exists()) destination.delete()
                if (!partial.renameTo(destination)) {
                    // renameTo can fail across mounts; fall back to a copy
                    FileInputStream(partial).use { src ->
                        FileOutputStream(destination).use { dst -> src.copyTo(dst) }
                    }
                    partial.delete()
                }
                onProgress(destination.length(), destination.length())
                return
            } catch (e: CancellationException) {
                partial.delete()
                throw e
            } catch (e: Exception) {
                lastError = e
                partial.delete()
                if (attempt < NetConfig.maxAttempts - 1) Thread.sleep(500L * (1 shl attempt))
            } finally {
                connection?.disconnect()
            }
        }
        throw DownloadException("Download failed for ${destination.name}: ${lastError?.message ?: "unknown error"}")
    }

    /** Form POST used by the Microsoft OAuth token endpoint. */
    fun postForm(url: String, params: Map<String, String>): String {
        val body = params.entries.joinToString("&") { (k, v) ->
            "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
        }
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw DownloadException("HTTP $code: ${text.take(300)}")
            text
        } finally {
            connection.disconnect()
        }
    }

    /** JSON POST with an optional bearer token, used for the Xbox / Minecraft services chain. */
    fun postJson(url: String, json: String, bearer: String? = null): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            if (bearer != null) connection.setRequestProperty("Authorization", "Bearer $bearer")
            connection.doOutput = true
            connection.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw DownloadException("HTTP $code: ${text.take(300)}")
            text
        } finally {
            connection.disconnect()
        }
    }

    fun getJson(url: String, bearer: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Authorization", "Bearer $bearer")
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw DownloadException("HTTP $code: ${text.take(300)}")
            text
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT
        connection.readTimeout = READ_TIMEOUT
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept-Encoding", "identity")
        connection.instanceFollowRedirects = true
        connection.requestMethod = "GET"
        connection.connect()
        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            throw DownloadException("HTTP $code from $url")
        }
        return connection
    }

    fun sha1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    /** Human readable byte count used across installation UI. */
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB")
        var value = bytes / 1024.0
        var index = 0
        while (value >= 1024 && index < units.size - 1) {
            value /= 1024.0
            index++
        }
        return if (value >= 100) "%.0f %s".format(value, units[index])
        else "%.1f %s".format(value, units[index])
    }
}
