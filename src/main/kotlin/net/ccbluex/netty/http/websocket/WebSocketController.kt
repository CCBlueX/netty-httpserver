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
package net.ccbluex.netty.http.websocket

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import net.ccbluex.netty.http.HttpServer.Companion.logger
import net.ccbluex.netty.http.coroutines.syncSuspend
import java.nio.channels.ClosedChannelException
import java.nio.charset.Charset
import java.util.concurrent.CopyOnWriteArraySet
import java.util.function.BiConsumer

/**
 * Controller for handling websocket connections.
 */
class WebSocketController(
    private val serverChannel: Channel,
) {

    private val scope = CoroutineScope(
        serverChannel.eventLoop().asCoroutineDispatcher() + SupervisorJob() + CoroutineExceptionHandler { _, err ->
            logger.error("Uncaught exception in websocket controller", err)
        }
    )

    init {
        serverChannel.closeFuture().addListener { scope.cancel("Channel closed") }
    }

    /**
     * Keeps track of all connected websocket connections to the server.
     * This is used to broadcast messages to all connected clients.
     */
    private val activeContexts = CopyOnWriteArraySet<ChannelHandlerContext>()

    /**
     * Broadcasts a message to all connected clients.
     *
     * @param text The message to broadcast.
     * @param charset The charset to use for encoding the message. Defaults to [Charsets.UTF_8].
     * @param onFailure The action to take if a failure occurs. Defaults to `null`.
     */
    fun broadcast(
        text: CharSequence,
        charset: Charset = Charsets.UTF_8,
        onFailure: BiConsumer<ChannelHandlerContext, Throwable>? = null,
    ): Job = scope.launch {
        val frameByteBuf = serverChannel.alloc().buffer()
        frameByteBuf.writeCharSequence(text, charset)

        val frame = TextWebSocketFrame(frameByteBuf)

        activeContexts.map { handlerContext ->
            launch {
                try {
                    handlerContext.channel()
                        .writeAndFlush(frame.retainedDuplicate())
                        .syncSuspend()
                } catch (_: ClosedChannelException) {
                    // Channel is not active, close and remove it
                    handlerContext.close().syncSuspend()
                    removeContext(handlerContext)
                } catch (e: Throwable) {
                    onFailure?.accept(handlerContext, e)
                }
            }
        }.joinAll()

        frame.release()
    }

    /**
     * Closes all active contexts.
     */
    fun disconnect() {
        activeContexts.removeIf { handlerContext ->
            runCatching {
                handlerContext.channel().close().sync()
            }.isSuccess
        }
    }

    /**
     * Adds a new context to the list of active contexts.
     *
     * @param context The context to add.
     */
    fun addContext(context: ChannelHandlerContext) {
        context.channel().closeFuture().addListener {
            removeContext(context)
        }
        activeContexts.add(context)
    }

    /**
     * Removes a context from the list of active contexts.
     *
     * @param context The context to remove.
     */
    fun removeContext(context: ChannelHandlerContext) {
        activeContexts.remove(context)
    }

}
