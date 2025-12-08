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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.ccbluex.netty.http.HttpServer.Companion.logger
import net.ccbluex.netty.http.middleware.Middleware
import net.ccbluex.netty.http.model.RequestContext
import net.ccbluex.netty.http.util.forEachIsInstance
import net.ccbluex.netty.http.websocket.WebSocketHandler
import java.net.URLDecoder

/**
 * Handles HTTP requests
 *
 * @property server The instance of the http server.
 */
internal class HttpServerHandler(private val server: HttpServer) : ChannelInboundHandlerAdapter() {

    private val localRequestContext = ThreadLocal<RequestContext>()
    private lateinit var channelScope: CoroutineScope

    /**
     * Extension property to get the WebSocket URL from an HttpRequest.
     */
    private val HttpRequest.webSocketUrl: String
        get() = "ws://${headers().get("Host")}${uri()}"

    /**
     * Adds the [CoroutineScope] of current [io.netty.channel.Channel].
     */
    override fun handlerAdded(ctx: ChannelHandlerContext) {
        super.handlerAdded(ctx)

        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            val ctxName = ctx.name()
            val channelId = ctx.channel().id().asLongText()
            logger.error(
                "Uncaught coroutine error in [ctx: $ctxName, channel: $channelId]",
                throwable
            )
        }

        channelScope = CoroutineScope(
            ctx.channel().eventLoop().asCoroutineDispatcher()
                    + CoroutineName("${ctx.name()}#${ctx.channel().id().asShortText()}")
                    + exceptionHandler
        )
        ctx.channel().closeFuture().addListener { channelScope.cancel() }
    }

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

                    server.middlewares.forEachIsInstance<Middleware.OnWebSocketUpgrade> { middleware ->
                        val response = middleware.invoke(ctx, msg)
                        if (response != null) {
                            ctx.writeAndFlush(response)
                            return super.channelRead(ctx, msg)
                        }
                    }

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
                        msg.headers(),
                    )

                    localRequestContext.set(requestContext)
                }
            }

            is HttpContent -> {
                val requestContext = localRequestContext.get() ?: run {
                    logger.warn("Received HttpContent without HttpRequest")
                    return super.channelRead(ctx, msg)
                }

                // Append content to the buffer
                msg.content().readBytes(requestContext.contentBuffer, msg.content().readableBytes())

                // If this is the last content, process the request
                if (msg is LastHttpContent) {
                    localRequestContext.remove()

                    server.middlewares.forEachIsInstance<Middleware.OnRequest> { middleware ->
                        val response = middleware.invoke(requestContext)
                        if (response != null) {
                            ctx.writeAndFlush(response)
                            return super.channelRead(ctx, msg)
                        }
                    }

                    channelScope.launch {
                        var response = server.processRequestContext(requestContext)
                        server.middlewares.forEachIsInstance<Middleware.OnResponse> { middleware ->
                            response = middleware.invoke(requestContext, response)
                        }
                        ctx.writeAndFlush(response)
                    }
                }
            }

        }

        super.channelRead(ctx, msg)
    }

}