package com.raku.smsrelay.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "forward_messages",
    indices = [Index(value = ["dedupeKey"], unique = true)],
)
data class ForwardMessageEntity(
    @PrimaryKey val id: String,
    val dedupeKey: String,
    val sender: String,
    val body: String,
    val receivedAtEpochMs: Long,
    val subscriptionId: Int?,
    val simLabel: String?,
    val status: String = ForwardStatus.PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val sentAtEpochMs: Long? = null,
    val isTest: Boolean = false,
)

