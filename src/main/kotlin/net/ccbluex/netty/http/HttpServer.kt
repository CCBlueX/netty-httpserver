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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

private const val STATE_IDLE = 0
private const val STATE_STARTING = -1
private const val STATE_START_ERROR = -2
private const val STATE_STARTED = -3
private const val STATE_STOPPING = -4

/**
 * NettyRest - A Web Rest-API server with support for WebSocket and File Serving using Netty.
 *
 * @since 1.0
 */
class HttpServer {

    private val state = AtomicInteger(STATE_IDLE)

    val routeController = RouteController()
    val webSocketController = WebSocketController()

    internal val middlewares = CopyOnWriteArrayList<Middleware>()

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
     *
     * @throws IllegalStateException if server is not available to start.
     */
    fun start(port: Int = 0): Int {
        val canStart = state.compareAndSet(STATE_IDLE, STATE_STARTING)
                || state.compareAndSet(STATE_START_ERROR, STATE_STARTING)
        if (!canStart) {
            error("Server is not idle.")
        }

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
            val actualPort = (ch.localAddress() as InetSocketAddress).port
            ch.closeFuture().addListener {
                if (!it.isSuccess) {
                    logger.error("Server channel closed unexpectedly (port: $actualPort)", it.cause())
                    state.compareAndSet(STATE_STARTED, STATE_START_ERROR)
                    stop()
                } else {
                    logger.debug("Server channel closed normally (port: $actualPort)")
                    state.compareAndSet(STATE_STARTED, STATE_IDLE)
                }
            }

            logger.info("Netty server started on port $actualPort.")

            serverChannel = ch
            state.set(STATE_STARTED)

            return actualPort
        } catch (t: Throwable) {
            logger.error("Netty server failed - $port", t)
            stop()
            // Forward the exception because we ran into an unexpected error
            throw t
        }
    }

    /**
     * Stops the Netty server gracefully.
     *
     * @throws IllegalStateException if server is not available to stop.
     */
    fun stop() {
        val canStop = state.compareAndSet(STATE_STARTED, STATE_STOPPING)
                || state.compareAndSet(STATE_START_ERROR, STATE_STOPPING)
        if (!canStop) {
            error("Server is not started neither failed to start.")
        }

        logger.info("Shutting down Netty server...")
        try {
            webSocketController.disconnect()
            serverChannel?.close()?.sync()
            bossGroup?.shutdownGracefully()?.sync()
            workerGroup?.shutdownGracefully()?.sync()
            logger.info("Netty server stopped.")
        } catch (e: Exception) {
            logger.warn("Error during shutdown", e)
        } finally {
            serverChannel = null
            bossGroup = null
            workerGroup = null
            state.set(STATE_IDLE)
        }
    }

}
