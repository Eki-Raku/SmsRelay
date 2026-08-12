package com.raku.smsrelay.data

import java.security.MessageDigest

object DedupeKey {
    fun create(
        sender: String,
        receivedAtEpochMs: Long,
        body: String,
        subscriptionId: Int?,
    ): String {
        val canonical = listOf(sender, receivedAtEpochMs.toString(), body, subscriptionId?.toString().orEmpty())
            .joinToString(separator = "\u001F")
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}

