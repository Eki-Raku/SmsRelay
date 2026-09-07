package com.raku.smsrelay

import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class CoroutineResultTest {
    @Test
    fun cancellationPropagatesInsteadOfBecomingAnApplicationFailure() {
        val cancellation = CancellationException("refresh superseded")

        val thrown = assertThrows(CancellationException::class.java) {
            runCatchingCancellable<Unit> { throw cancellation }
        }

        assertSame(cancellation, thrown)
    }

    @Test
    fun ordinaryFailuresRemainAvailableToUserFacingErrorHandling() {
        val result = runCatchingCancellable<Unit> { error("provider failed") }

        assertEquals("provider failed", result.exceptionOrNull()?.message)
    }
}
