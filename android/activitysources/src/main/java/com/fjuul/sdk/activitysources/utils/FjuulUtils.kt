package com.fjuul.sdk.activitysources.utils
import com.fjuul.sdk.core.entities.Result
import kotlin.Result as KtResult
import kotlin.runCatching

/**
 * Convert a Kotlin standard library [kotlin.Result] into the SDK’s own [Result].
 *
 * This preserves both success and failure states:
 * - On success, wraps the value in [Result.value].
 * - On failure, wraps the exception in [Result.error].
 *
 * @receiver The `kotlin.Result<T>` to adapt.
 * @return A `com.fjuul.sdk.core.entities.Result<T>` with the same outcome.
 */
inline fun <T : Any> KtResult<T>.toResult(): Result<T> = fold(
    onSuccess  = { v -> Result.value(v) },
    onFailure  = { e -> Result.error(e) }
)

/**
 * Execute the given [block] catching any thrown exception, and return
 * its result as the SDK’s [Result] type.
 *
 * Equivalent to `runCatching { block() }.toResult()`.
 *
 * @param block A lambda whose return value or thrown exception
 *              will be wrapped in [Result].
 * @return A `Result<T>` whose value is the block’s return on success,
 *         or whose error is the thrown exception on failure.
 */
inline fun <T : Any> runCatchingResult(block: () -> T): Result<T> =
    runCatching(block).toResult()
