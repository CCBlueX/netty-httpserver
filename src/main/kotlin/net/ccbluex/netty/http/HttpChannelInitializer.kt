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

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec

internal class HttpChannelInitializer(private val server: HttpServer) : ChannelInitializer<SocketChannel>() {

    /**
     * Initializes the channel with the necessary handlers.
     *
     * @param socketChannel The socket channel to initialize.
     */
    override fun initChannel(socketChannel: SocketChannel) {
        val pipeline = socketChannel.pipeline()
        pipeline.addLast("httpServerCodec", HttpServerCodec())
        pipeline.addLast("httpHandler", HttpServerHandler(server))
    }

}
