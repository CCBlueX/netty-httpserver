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
import io.netty.channel.ChannelOption
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import net.ccbluex.netty.http.middleware.Middleware
import net.ccbluex.netty.http.rest.RouteController
import net.ccbluex.netty.http.util.TransportType
import net.ccbluex.netty.http.websocket.WebSocketController
import org.apache.logging.log4j.LogManager


/**
 * NettyRest - A Web Rest-API server with support for WebSocket and File Serving using Netty.
 *
 * @since 1.0
 */
class HttpServer {

    val routeController = RouteController()
    val webSocketController = WebSocketController()

    val middlewares = mutableListOf<Middleware>()

    companion object {
        internal val logger = LogManager.getLogger("HttpServer")
    }

    fun middleware(middleware: Middleware) {
        middlewares += middleware
    }

    /**
     * Starts the Netty server on the specified port.
     */
    fun start(port: Int) {
        val b = ServerBootstrap()

        val (bossGroup, workerGroup) = TransportType.apply(b)

        try {
            logger.info("Starting Netty server...")
            b.option(ChannelOption.SO_BACKLOG, 1024)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(HttpChannelInitializer(this))
            val ch = b.bind(port).sync().channel()

            logger.info("Netty server started on port $port.")
            ch.closeFuture().sync()
        } catch (e: InterruptedException) {
            logger.error("Netty server interrupted", e)
        } catch (t: Throwable) {
            logger.error("Netty server failed - $port", t)

            // Forward the exception because we ran into an unexpected error
            throw t
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }

        logger.info("Netty server stopped.")
    }

}
