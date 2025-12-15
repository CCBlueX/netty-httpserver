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
package net.ccbluex.netty.http.util

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFactory
import io.netty.channel.EventLoopGroup
import io.netty.channel.IoHandlerFactory
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.ServerChannel
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueIoHandler
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel
import net.ccbluex.netty.http.util.TransportType.EPOLL
import net.ccbluex.netty.http.util.TransportType.KQUEUE
import net.ccbluex.netty.http.util.TransportType.NIO

internal enum class TransportType(
    val serverChannelFactory: ChannelFactory<out ServerChannel>,
) {
    NIO(::NioServerSocketChannel) {
        override val isAvailable get() = true
        override val ioHandlerFactory: IoHandlerFactory = NioIoHandler.newFactory()
    },

    EPOLL(::EpollServerSocketChannel) {
        override val isAvailable get() = try {
            Epoll.isAvailable()
        } catch (_: Throwable) { false }
        override val ioHandlerFactory: IoHandlerFactory get() = EpollIoHandler.newFactory()
    },

    KQUEUE(::KQueueServerSocketChannel) {
        override val isAvailable get() = try {
            KQueue.isAvailable()
        } catch (_: Throwable) { false }
        override val ioHandlerFactory: IoHandlerFactory get() = KQueueIoHandler.newFactory()
    };

    abstract val isAvailable: Boolean

    abstract val ioHandlerFactory: IoHandlerFactory
}

private val available by lazy {
    arrayOf(EPOLL, KQUEUE, NIO).first { it.isAvailable }
}

/**
 * Set the channel factory and event loop groups for the given server bootstrap.
 *
 * @receiver The server bootstrap to configure.
 * @param useNativeTransport Whether to use native transport (Epoll or KQueue).
 *
 * @return Parent and child group.
 */
@JvmOverloads
fun ServerBootstrap.setup(useNativeTransport: Boolean = true): Pair<EventLoopGroup, EventLoopGroup> {
    val type = if (useNativeTransport) available else NIO

    val parentGroup = MultiThreadIoEventLoopGroup(1, type.ioHandlerFactory)
    val childGroup = MultiThreadIoEventLoopGroup(type.ioHandlerFactory)
    group(parentGroup, childGroup)
        .channelFactory(type.serverChannelFactory)
    return parentGroup to childGroup
}
