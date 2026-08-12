package com.raku.smsrelay.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsModelsTest {
    @Test
    fun destinationAllowsPhoneCharactersButRejectsEmptyValues() {
        assertEquals("+86 138-0013-8000", SmsSendPolicy.normalizeDestination(" +86 138-0013-8000 "))
        assertNull(SmsSendPolicy.normalizeDestination("   "))
        assertNull(SmsSendPolicy.normalizeDestination("13800138000\nmalicious"))
    }

    @Test
    fun multipartResultFailsWhenAnyPartFails() {
        assertEquals(SmsSendAggregate.SENT, SmsSendPolicy.aggregate(listOf(true, true)))
        assertEquals(SmsSendAggregate.FAILED, SmsSendPolicy.aggregate(listOf(true, false)))
        assertEquals(SmsSendAggregate.FAILED, SmsSendPolicy.aggregate(emptyList()))
    }
}
