package net.ccbluex.netty.http.coroutines

import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException

/**
 * Suspend until this Netty Future completes,
 * and rethrows the cause of the failure if this future failed.
 *
 * Returns the Future itself.
 */
suspend fun <V, F : Future<V>> F.syncSuspend(): F {
    if (isDone) return unwrapDone().getOrThrow()

    return suspendCancellableCoroutine { cont ->
        addListener(FutureResultContListener(cont))

        cont.invokeOnCancellation {
            this.cancel(false)
        }
    }
}

/**
 * Suspend until this Netty Future completes.
 *
 * Returns the Future itself.
 */
suspend fun <F : Future<*>> F.awaitSuspend(): F {
    if (isDone) return this

    return suspendCancellableCoroutine { cont ->
        addListener(FutureContListener(cont))

        cont.invokeOnCancellation {
            this.cancel(false)
        }
    }
}

private class FutureContListener<V, F : Future<V>>(
    private val cont: CancellableContinuation<F>
): GenericFutureListener<F> {
    override fun operationComplete(future: F) {
        if (cont.isActive) {
            cont.resumeWith(Result.success(future))
        }
    }
}

private class FutureResultContListener<V, F : Future<V>>(
    private val cont: CancellableContinuation<F>
): GenericFutureListener<F> {
    override fun operationComplete(future: F) {
        if (cont.isActive) {
            cont.resumeWith(future.unwrapDone())
        }
    }
}

private fun <V, F : Future<V>> F.unwrapDone(): Result<F> =
    when {
        isSuccess -> Result.success(this)
        isCancelled -> Result.failure(CancellationException("Netty Future was cancelled"))
        else -> Result.failure(
            this.cause() ?: IllegalStateException("Future failed without cause")
        )
    }
