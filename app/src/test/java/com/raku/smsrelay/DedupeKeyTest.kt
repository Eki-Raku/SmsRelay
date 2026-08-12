package com.raku.smsrelay

import com.raku.smsrelay.data.DedupeKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DedupeKeyTest {
    @Test
    fun sameMessageProducesSameKey() {
        val first = DedupeKey.create("95561", 1234L, "hello", 1)
        val second = DedupeKey.create("95561", 1234L, "hello", 1)

        assertEquals(first, second)
        assertEquals(64, first.length)
    }

    @Test
    fun changedMessageProducesDifferentKey() {
        val first = DedupeKey.create("95561", 1234L, "hello", 1)
        val second = DedupeKey.create("95561", 1234L, "world", 1)

        assertNotEquals(first, second)
    }
}
