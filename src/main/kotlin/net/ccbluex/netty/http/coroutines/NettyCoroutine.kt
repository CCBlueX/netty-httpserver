package net.ccbluex.netty.http.coroutines

import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Suspend until this Netty Future completes.
 *
 * Returns the Future result. Throws on failure or cancellation.
 */
suspend fun <V, F : Future<V>> F.suspend(): V {
    if (isDone) return unwrapDone().getOrThrow()

    return suspendCancellableCoroutine { cont ->
        addListener(futureContinuationListener(cont))

        cont.invokeOnCancellation {
            this.cancel(false)
        }
    }
}

private fun <V, F : Future<V>> futureContinuationListener(
    cont: CancellableContinuation<V>
): GenericFutureListener<F> = GenericFutureListener { future ->
    if (cont.isActive) {
        cont.resumeWith(future.unwrapDone())
    }
}

private fun <V, F : Future<V>> F.unwrapDone(): Result<V> =
    when {
        isSuccess -> Result.success(this.now)
        isCancelled -> Result.failure(CancellationException("Netty Future was cancelled"))
        else -> Result.failure(
            this.cause() ?: IllegalStateException("Future failed without cause")
        )
    }
