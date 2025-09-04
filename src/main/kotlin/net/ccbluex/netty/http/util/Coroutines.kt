/*
 * This file is part of Netty-Rest (https://github.com/CCBlueX/netty-rest)
 *
 * Copyright (c) 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Netty-Rest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Netty-Rest. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package net.ccbluex.netty.http.util

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Await the completion of this [ChannelFuture] and return it.
 *
 * In order to replace [ChannelFuture.sync] in coroutine.
 *
 * If the future is already done, it will be returned immediately.
 * If the future is not done, it will be awaited until it is done.
 * If the future is cancelled, a [CancellationException] will be thrown.
 * If the future is failed, the cause will be thrown.
 */
suspend fun ChannelFuture.awaitSuspend(): ChannelFuture {
    if (isDone) {
        if (isSuccess) return this
        throw cause() ?: IllegalStateException("Future failed without cause")
    }

    return suspendCancellableCoroutine { cont ->
        addListener(ChannelFutureListener { future ->
            if (future.isSuccess) {
                cont.resume(future)
            } else {
                cont.resumeWithException(
                    future.cause() ?: IllegalStateException("Future failed without cause")
                )
            }
        })

        cont.invokeOnCancellation {
            if (!isDone) cancel(false)
        }
    }
}
