package com.raku.smsrelay.sms

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.raku.smsrelay.receiver.ParsedSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SmsInboxInsert(
    val uri: Uri,
    val threadId: Long,
)

class SystemSmsRepository(context: Context) {
    private val resolver: ContentResolver = context.applicationContext.contentResolver
    private val appContext = context.applicationContext

    suspend fun insertInbox(message: ParsedSms): SmsInboxInsert = withContext(Dispatchers.IO) {
        val threadId = Telephony.Threads.getOrCreateThreadId(appContext, message.sender)
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, message.sender)
            put(Telephony.Sms.BODY, message.body)
            put(Telephony.Sms.DATE, message.receivedAtEpochMs)
            put(Telephony.Sms.DATE_SENT, message.receivedAtEpochMs)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
            put(Telephony.Sms.THREAD_ID, threadId)
            message.subscriptionId?.let { put(COLUMN_SUBSCRIPTION_ID, it) }
        }
        val uri = requireNotNull(resolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)) {
            "系统短信收件箱写入失败"
        }
        SmsInboxInsert(uri = uri, threadId = threadId)
    }

    suspend fun unreadCount(threadId: Long): Int = withContext(Dispatchers.IO) {
        resolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
            arrayOf(threadId.toString()),
            null,
        )?.use { it.count } ?: 0
    }

    suspend fun markThreadRead(threadId: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        resolver.update(
            Telephony.Sms.CONTENT_URI,
            values,
            "${Telephony.Sms.THREAD_ID} = ? AND (${Telephony.Sms.READ} = 0 OR ${Telephony.Sms.SEEN} = 0)",
            arrayOf(threadId.toString()),
        )
    }

    suspend fun conversations(limit: Int = 200): List<SmsConversation> = withContext(Dispatchers.IO) {
        val result = LinkedHashMap<Long, SmsConversation>()
        resolver.query(
            Telephony.Sms.CONTENT_URI,
            MESSAGE_PROJECTION,
            null,
            null,
            "${Telephony.Sms.DATE} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val readIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
            while (cursor.moveToNext() && result.size < limit) {
                cursor.getLong(idIndex)
                val threadId = cursor.getLong(threadIndex)
                result.putIfAbsent(
                    threadId,
                    SmsConversation(
                        threadId = threadId,
                        address = cursor.getString(addressIndex).orEmpty(),
                        snippet = cursor.getString(bodyIndex).orEmpty(),
                        dateEpochMs = cursor.getLong(dateIndex),
                        unread = cursor.getInt(readIndex) == 0,
                    ),
                )
            }
        }
        result.values.toList()
    }

    suspend fun messages(threadId: Long, limit: Int = 500): List<SystemSmsMessage> =
        withContext(Dispatchers.IO) {
            buildList {
                resolver.query(
                    Telephony.Sms.CONTENT_URI,
                    MESSAGE_PROJECTION,
                    "${Telephony.Sms.THREAD_ID} = ?",
                    arrayOf(threadId.toString()),
                    "${Telephony.Sms.DATE} ASC",
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val threadIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                    val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                    val readIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
                    val subscriptionIndex = cursor.getColumnIndex(COLUMN_SUBSCRIPTION_ID)
                    while (cursor.moveToNext() && size < limit) {
                        add(
                            SystemSmsMessage(
                                id = cursor.getLong(idIndex),
                                threadId = cursor.getLong(threadIndex),
                                address = cursor.getString(addressIndex).orEmpty(),
                                body = cursor.getString(bodyIndex).orEmpty(),
                                dateEpochMs = cursor.getLong(dateIndex),
                                type = cursor.getInt(typeIndex),
                                read = cursor.getInt(readIndex) != 0,
                                subscriptionId = if (subscriptionIndex >= 0 && !cursor.isNull(subscriptionIndex)) {
                                    cursor.getInt(subscriptionIndex)
                                } else {
                                    null
                                },
                            ),
                        )
                    }
                }
            }
        }

    suspend fun insertOutbox(address: String, body: String, subscriptionId: Int?): Uri =
        withContext(Dispatchers.IO) {
            val threadId = Telephony.Threads.getOrCreateThreadId(appContext, address)
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
                put(Telephony.Sms.THREAD_ID, threadId)
                subscriptionId?.let { put(COLUMN_SUBSCRIPTION_ID, it) }
            }
            requireNotNull(resolver.insert(Telephony.Sms.Outbox.CONTENT_URI, values)) {
                "系统短信发件箱写入失败"
            }
        }

    suspend fun markSent(uri: Uri) = updateType(uri, Telephony.Sms.MESSAGE_TYPE_SENT, errorCode = 0)

    suspend fun markFailed(uri: Uri, errorCode: Int) =
        updateType(uri, Telephony.Sms.MESSAGE_TYPE_FAILED, errorCode)

    private suspend fun updateType(uri: Uri, type: Int, errorCode: Int) = withContext(Dispatchers.IO) {
        val id = ContentUris.parseId(uri)
        val values = ContentValues().apply {
            put(Telephony.Sms.TYPE, type)
            put(Telephony.Sms.ERROR_CODE, errorCode)
            if (type == Telephony.Sms.MESSAGE_TYPE_SENT) put(Telephony.Sms.DATE_SENT, System.currentTimeMillis())
        }
        resolver.update(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id), values, null, null)
    }

    private companion object {
        const val COLUMN_SUBSCRIPTION_ID = "sub_id"
        val MESSAGE_PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            COLUMN_SUBSCRIPTION_ID,
        )
    }
}
