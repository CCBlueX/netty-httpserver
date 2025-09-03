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
import net.ccbluex.netty.http.middleware.Middleware
import net.ccbluex.netty.http.rest.RouteController
import net.ccbluex.netty.http.util.TransportType
import net.ccbluex.netty.http.websocket.WebSocketController
import org.apache.logging.log4j.LogManager
import java.net.InetSocketAddress
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


/**
 * NettyRest - A Web Rest-API server with support for WebSocket and File Serving using Netty.
 *
 * @since 1.0
 */
class HttpServer {

    val routeController = RouteController()
    val webSocketController = WebSocketController()

    private val lock = ReentrantLock()

    internal val middlewares = mutableListOf<Middleware>()

    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null
    private var serverChannel: Channel? = null

    companion object {
        internal val logger = LogManager.getLogger("HttpServer")
    }

    fun middleware(middleware: Middleware) = apply {
        middlewares += middleware
    }

    /**
     * Starts the Netty server on the specified port.
     *
     * @param port The port of HTTP server. `0` means to auto select one.
     *
     * @return actual port of server.
     */
    fun start(port: Int): Int = lock.withLock {
        val b = ServerBootstrap()

        val groups = TransportType.apply(b)
        bossGroup = groups.first
        workerGroup = groups.second

        try {
            logger.info("Starting Netty server...")
            b.option(ChannelOption.SO_BACKLOG, 1024)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(HttpChannelInitializer(this))
            val ch = b.bind(port).sync().channel()
            serverChannel = ch

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
    fun stop() {
        lock.withLock {
            logger.info("Shutting down Netty server...")
            try {
                serverChannel?.close()?.sync()
                bossGroup?.shutdownGracefully()?.sync()
                workerGroup?.shutdownGracefully()?.sync()
            } catch (e: Exception) {
                logger.warn("Error during shutdown", e)
            } finally {
                serverChannel = null
                bossGroup = null
                workerGroup = null
            }
            logger.info("Netty server stopped.")
        }
    }

}
