package com.raku.smsrelay.receiver

import org.junit.Assert.assertEquals
import org.junit.Test

class ForwardIngressPolicyTest {
    @Test
    fun disabledForwardingDoesNotInsertOrSchedule() {
        assertEquals(
            IngressDecision.DISABLED,
            ForwardIngressPolicy.decide(enabled = false, inserted = true),
        )
    }

    @Test
    fun duplicateMessageDoesNotScheduleAgain() {
        assertEquals(
            IngressDecision.DUPLICATE,
            ForwardIngressPolicy.decide(enabled = true, inserted = false),
        )
    }

    @Test
    fun newlyInsertedEnabledMessageIsScheduled() {
        assertEquals(
            IngressDecision.SCHEDULE,
            ForwardIngressPolicy.decide(enabled = true, inserted = true),
        )
    }
}
