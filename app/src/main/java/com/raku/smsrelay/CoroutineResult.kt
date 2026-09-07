package com.raku.smsrelay

import kotlin.coroutines.cancellation.CancellationException

/**
 * Equivalent to [runCatching], except coroutine cancellation is never converted
 * into an application failure. Cancellation is control flow and must propagate.
 */
internal inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Throwable) {
    Result.failure(error)
}
