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

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Controller for handling websocket connections.
 */
class WebSocketController {

    /**
     * Keeps track of all connected websocket connections to the server.
     * This is used to broadcast messages to all connected clients.
     */
    private val activeContexts = CopyOnWriteArrayList<ChannelHandlerContext>()

    /**
     * Broadcasts a message to all connected clients.
     *
     * @param message The message to broadcast.
     * @param failure The action to take if a failure occurs.
     */
    fun broadcast(text: String, failure: (ChannelHandlerContext, Throwable) -> Unit = { _, _ -> }) {
        activeContexts.forEach { handlerContext ->
            try {
                handlerContext.channel().writeAndFlush(TextWebSocketFrame(text))
            } catch (e: Throwable) {
                failure(handlerContext, e)
            }
        }
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