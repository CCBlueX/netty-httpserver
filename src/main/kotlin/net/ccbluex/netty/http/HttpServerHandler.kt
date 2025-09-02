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
package net.ccbluex.netty.http

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.LastHttpContent
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import net.ccbluex.netty.http.HttpServer.Companion.logger
import net.ccbluex.netty.http.model.RequestContext
import net.ccbluex.netty.http.websocket.WebSocketHandler
import java.net.URLDecoder

/**
 * Handles HTTP requests
 *
 * @property server The instance of the http server.
 */
internal class HttpServerHandler(private val server: HttpServer) : ChannelInboundHandlerAdapter() {

    private val localRequestContext = ThreadLocal<RequestContext>()

    /**
     * Extension property to get the WebSocket URL from an HttpRequest.
     */
    private val HttpRequest.webSocketUrl: String
        get() = "ws://${headers().get("Host")}${uri()}"

    /**
     * Reads the incoming messages and processes HTTP requests.
     *
     * @param ctx The context of the channel handler.
     * @param msg The incoming message.
     */
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        when (msg) {
            is HttpRequest -> {
                val headers = msg.headers()
                val connection = headers.get(HttpHeaderNames.CONNECTION)
                val upgrade = headers.get(HttpHeaderNames.UPGRADE)

                logger.debug(
                    "Incoming connection {} with headers: \n{}",
                    ctx.channel(),
                    headers.joinToString { "${it.key}=${it.value}\n" })

                if (connection.equals("Upgrade", ignoreCase = true) &&
                    upgrade.equals("WebSocket", ignoreCase = true)) {
                    // Takes out Http Request Handler from the pipeline and replaces it with WebSocketHandler
                    ctx.pipeline().replace(this, "websocketHandler", WebSocketHandler(server))

                    // Upgrade connection from Http to WebSocket protocol
                    val wsFactory = WebSocketServerHandshakerFactory(msg.webSocketUrl, null, true)
                    val handshaker = wsFactory.newHandshaker(msg)

                    if (handshaker == null) {
                        // This means the version of the websocket protocol is not supported
                        // Unlikely to happen, but it's better to be safe than sorry
                        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel())
                    } else {
                        // Otherwise pass handshake to the handshaker
                        handshaker.handshake(ctx.channel(), msg)
                    }

                    server.webSocketController.addContext(ctx)
                } else {
                    val requestContext = RequestContext(
                        msg.method(),
                        URLDecoder.decode(msg.uri(), Charsets.UTF_8),
                        msg.headers().associate { it.key to it.value },
                    )

                    localRequestContext.set(requestContext)
                }
            }

            is HttpContent -> {
                val requestContext = localRequestContext.get() ?: run {
                    logger.warn("Received HttpContent without HttpRequest")
                    return
                }

                // Append content to the buffer
                requestContext
                    .contentBuffer
                    .append(msg.content().toString(Charsets.UTF_8))

                // If this is the last content, process the request
                if (msg is LastHttpContent) {
                    localRequestContext.remove()

                    val response = server.processRequestContext(requestContext)
                    val httpResponse = server.middlewares.fold(response) { acc, f -> f(requestContext, acc) }
                    ctx.writeAndFlush(httpResponse)
                }
            }

        }

        super.channelRead(ctx, msg)
    }

}