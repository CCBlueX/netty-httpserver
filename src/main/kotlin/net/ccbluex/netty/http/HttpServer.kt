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

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ccbluex.netty.http.coroutines.awaitSuspend
import net.ccbluex.netty.http.coroutines.syncSuspend
import net.ccbluex.netty.http.middleware.Middleware
import net.ccbluex.netty.http.rest.Node
import net.ccbluex.netty.http.rest.RouteController
import net.ccbluex.netty.http.util.setup
import net.ccbluex.netty.http.websocket.WebSocketController
import org.apache.logging.log4j.LogManager
import java.net.InetSocketAddress


/**
 * NettyRest - A Web Rest-API server with support for WebSocket and File Serving using Netty.
 *
 * @since 1.0
 */
class HttpServer {

    internal val routeController = RouteController()

    private val lock = Mutex()

    internal val middlewares = mutableListOf<Middleware>()

    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null
    private var serverChannel: Channel? = null
    var webSocketController: WebSocketController? = null
        private set

    companion object {
        internal val logger = LogManager.getLogger("HttpServer")
    }

    fun middleware(middleware: Middleware) = apply {
        middlewares += middleware
    }

    fun routing(block: Node.() -> Unit) {
        routeController.apply(block)
    }

    /**
     * Starts the Netty server on the specified port.
     *
     * @param port The port of HTTP server. `0` means to auto select one.
     * @param useNativeTransport Whether to use native transport (Epoll or KQueue).
     *
     * @return actual port of server.
     */
    suspend fun start(port: Int, useNativeTransport: Boolean = true): Int = lock.withLock {
        val b = ServerBootstrap()

        val groups = b.setup(useNativeTransport)
        bossGroup = groups.first
        workerGroup = groups.second

        try {
            logger.info("Starting Netty server...")
            b.option(ChannelOption.SO_BACKLOG, 1024)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(HttpChannelInitializer(this))
            val ch = b.bind(port).syncSuspend().channel()
            serverChannel = ch
            webSocketController = WebSocketController(ch)

            logger.info("Netty server started on port $port.")

            return@withLock (ch.localAddress() as InetSocketAddress).port
        } catch (t: Throwable) {
            logger.error("Netty server failed - $port", t)
            stop()
            // Forward the exception because we ran into an unexpected error
            throw t
        }
    }

    /**
     * Stops the Netty server gracefully.
     */
    suspend fun stop() = lock.withLock {
        logger.info("Shutting down Netty server...")
        try {
            webSocketController?.disconnect()
            serverChannel?.close()?.awaitSuspend()
            bossGroup?.shutdownGracefully()?.awaitSuspend()
            workerGroup?.shutdownGracefully()?.awaitSuspend()
        } catch (e: Exception) {
            logger.warn("Error during shutdown", e)
        } finally {
            serverChannel = null
            bossGroup = null
            workerGroup = null
        }
        logger.info("Netty server stopped.")
    }

    fun stopBlocking() = runBlocking {
        stop()
    }

}
