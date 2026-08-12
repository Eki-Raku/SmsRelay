package com.raku.smsrelay.receiver

import com.raku.smsrelay.data.DedupeKey
import com.raku.smsrelay.data.ForwardMessageDao
import com.raku.smsrelay.data.ForwardMessageEntity
import com.raku.smsrelay.data.SettingsRepository
import com.raku.smsrelay.worker.ForwardScheduler
import java.util.UUID

enum class IngressDecision {
    DISABLED,
    DUPLICATE,
    SCHEDULE,
}

object ForwardIngressPolicy {
    fun decide(enabled: Boolean, inserted: Boolean): IngressDecision = when {
        !enabled -> IngressDecision.DISABLED
        !inserted -> IngressDecision.DUPLICATE
        else -> IngressDecision.SCHEDULE
    }
}

class ForwardIngress(
    private val settingsRepository: SettingsRepository,
    private val dao: ForwardMessageDao,
    private val scheduler: ForwardScheduler,
) {
    suspend fun accept(parsed: ParsedSms): IngressDecision {
        if (!settingsRepository.current().enabled) return IngressDecision.DISABLED

        val message = ForwardMessageEntity(
            id = UUID.randomUUID().toString(),
            dedupeKey = DedupeKey.create(
                sender = parsed.sender,
                receivedAtEpochMs = parsed.receivedAtEpochMs,
                body = parsed.body,
                subscriptionId = parsed.subscriptionId,
            ),
            sender = parsed.sender,
            body = parsed.body,
            receivedAtEpochMs = parsed.receivedAtEpochMs,
            subscriptionId = parsed.subscriptionId,
            simLabel = parsed.subscriptionId?.let { "SIM $it" },
        )
        val inserted = dao.insert(message) != -1L
        val decision = ForwardIngressPolicy.decide(enabled = true, inserted = inserted)
        if (decision == IngressDecision.SCHEDULE) scheduler.enqueue(message.id)
        return decision
    }
}
