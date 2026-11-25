package com.voidlauncher.util

import java.security.MessageDigest
import java.util.UUID

/** Deterministic offline UUID, identical to Minecraft's "OfflinePlayer:<name>" identity. */
fun offlineUuid(username: String): String {
    val digest = MessageDigest.getInstance("MD5")
        .digest("OfflinePlayer:$username".toByteArray(Charsets.UTF_8))
    digest[6] = ((digest[6].toInt() and 0x0f) or 0x30).toByte()
    digest[8] = ((digest[8].toInt() and 0x3f) or 0x80).toByte()
    val hex = digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
        "${hex.substring(16, 20)}-${hex.substring(20, 32)}"
}

fun newId(): String = UUID.randomUUID().toString()

fun parseUuid(value: String?): String =
    if (value.isNullOrBlank()) newId()
    else value.replace(Regex("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})"), "$1-$2-$3-$4-$5")
