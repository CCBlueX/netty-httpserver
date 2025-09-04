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

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.websocketx.*
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.HttpServer.Companion.logger
import net.ccbluex.netty.http.util.copyOf

/**
 * Handles WebSocket frames for the http server.
 *
 * @property server The instance of the http server.
 * @see [https://tools.ietf.org/html/rfc6455]
 */
internal class WebSocketHandler(
    private val server: HttpServer,
) : ChannelInboundHandlerAdapter() {

    /**
     * Registers close listener for the channel.
     */
    override fun handlerAdded(ctx: ChannelHandlerContext) {
        super.handlerAdded(ctx)
        ctx.channel().closeFuture().addListener { future ->
            if (future.isSuccess) {
                server.webSocketController.removeContext(ctx)
            } else {
                logger.warn("WebSocket close failed (channel: ${ctx.channel()})", future.cause())
            }
        }
    }

    /**
     * Reads the incoming messages and processes WebSocket frames.
     *
     * @param ctx The context of the channel handler.
     * @param msg The incoming message.
     */
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is WebSocketFrame) {
            val channel = ctx.channel()
            logger.debug("WebSocketFrame received ({}): {}", channel, msg.javaClass.name)

            when (msg) {
                is PingWebSocketFrame -> {
                    val pongBuffer = channel.alloc().copyOf(msg.content())
                    channel.writeAndFlush(PongWebSocketFrame(pongBuffer))
                }

                is CloseWebSocketFrame -> {
                    // Accept close frame and send close frame back
                    channel.writeAndFlush(msg.retainedDuplicate())
                        .addListener(ChannelFutureListener.CLOSE)
                    logger.debug("WebSocket closing due to ${msg.reasonText()} (${msg.statusCode()})")
                }
                else -> logger.error("Unknown WebSocketFrame type: ${msg.javaClass.name}")
            }
        } else {
            // Non-WebSocket frame, pass it to the next handler
            ctx.fireChannelRead(msg)
        }
    }

}