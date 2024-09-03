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
import io.netty.handler.codec.http.websocketx.WebSocketFrame

/**
 * Controller for handling websocket connections.
 */
class WebSocketController {

    /**
     * Keeps track of all connected websocket connections to the server.
     * This is used to broadcast messages to all connected clients.
     */
    val activeContexts = mutableListOf<ChannelHandlerContext>()

    /**
     * Broadcasts a message to all connected clients.
     *
     * @param message The message to broadcast.
     * @param failure The action to take if a failure occurs.
     */
    fun broadcast(message: WebSocketFrame, failure: (ChannelHandlerContext, Throwable) -> Unit = { _, _ -> }) {
        activeContexts.forEach {
            try {
                it.writeAndFlush(message)
            } catch (e: Throwable) {
                failure(it, e)
            }
        }
    }

    /**
     * Closes all active contexts.
     */
    fun closeAll() {
        activeContexts.forEach {
            it.close()
        }
    }

}