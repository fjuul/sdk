package com.fjuul.sdk.activitysources.utils

import com.fjuul.sdk.core.entities.Callback
import com.fjuul.sdk.core.entities.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Synchronous dispatch: block returns a Result<T> directly,
 * and is passed to the callback without additional wrapping.
 */
inline fun <T : Any> runResultAndCallback(
    block: () -> Result<T>,
    callback: Callback<T>
) {
    callback.onResult(block())
}

/**
 * Synchronous dispatch with exception catching: block returns T,
 * exceptions are caught and wrapped into Result.error, success into Result.value.
 */
inline fun <T : Any> runCatchingAndCallback(
    block: () -> T,
    callback: Callback<T>
) {
    val result: Result<T> = runCatching(block).fold(
        onSuccess = { v -> Result.value(v) },
        onFailure = { e -> Result.error(e) }
    )
    callback.onResult(result)
}

/**
 * Asynchronous dispatch: suspend block returns T,
 * exceptions are caught and wrapped into Result.error, success into Result.value,
 * then delivered on Dispatchers.Main.
 */
inline fun <T : Any> runAsyncAndCallback(
    crossinline block: suspend () -> T,
    callback: Callback<T>
) {
    CoroutineScope(Dispatchers.IO).launch {
        val result: Result<T> = try {
            Result.value(block())
        } catch (e: Throwable) {
            Result.error(e)
        }
        withContext(Dispatchers.Main) {
            callback.onResult(result)
        }
    }
}
