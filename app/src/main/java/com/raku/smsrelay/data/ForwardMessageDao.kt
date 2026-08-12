package com.raku.smsrelay.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardMessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: ForwardMessageEntity): Long

    @Query("SELECT * FROM forward_messages ORDER BY receivedAtEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ForwardMessageEntity>>

    @Query("SELECT * FROM forward_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ForwardMessageEntity?

    @Query(
        """
        UPDATE forward_messages
        SET status = 'SENDING', attempts = attempts + 1, lastError = NULL
        WHERE id = :id AND status != 'SENT'
        """,
    )
    suspend fun markSending(id: String)

    @Query(
        """
        UPDATE forward_messages
        SET status = 'SENT', sentAtEpochMs = :sentAtEpochMs, lastError = NULL
        WHERE id = :id
        """,
    )
    suspend fun markSent(id: String, sentAtEpochMs: Long)

    @Query("UPDATE forward_messages SET status = 'RETRY', lastError = :error WHERE id = :id")
    suspend fun markRetry(id: String, error: String)

    @Query("UPDATE forward_messages SET status = 'FAILED', lastError = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String)

    @Query("UPDATE forward_messages SET status = 'PENDING', lastError = NULL WHERE id = :id")
    suspend fun resetForRetry(id: String)

    @Query("DELETE FROM forward_messages WHERE status = 'SENT' AND sentAtEpochMs < :beforeEpochMs")
    suspend fun deleteSentBefore(beforeEpochMs: Long): Int
}
