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
import io.netty.channel.ServerChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel

internal enum class TransportType {
    Nio {
        override val isAvailable get() = true

        override fun newParentGroup() = NioEventLoopGroup(1)

        override fun newChildGroup() = NioEventLoopGroup()

        override fun newServerChannel() = NioServerSocketChannel()
    },

    Epoll {
        override val isAvailable get() = try {
            io.netty.channel.epoll.Epoll.isAvailable()
        } catch (_: Throwable) { false }

        override fun newParentGroup() = EpollEventLoopGroup(1)

        override fun newChildGroup() = EpollEventLoopGroup()

        override fun newServerChannel() = EpollServerSocketChannel()
    },

    KQueue {
        override val isAvailable get() = try {
            io.netty.channel.kqueue.KQueue.isAvailable()
        } catch (_: Throwable) { false }

        override fun newParentGroup() = KQueueEventLoopGroup(1)

        override fun newChildGroup() = KQueueEventLoopGroup()

        override fun newServerChannel() = KQueueServerSocketChannel()
    };

    abstract val isAvailable: Boolean

    abstract fun newParentGroup(): EventLoopGroup

    abstract fun newChildGroup(): EventLoopGroup

    abstract fun newServerChannel(): ServerChannel

    companion object {
        private val available by lazy {
            arrayOf(Epoll, KQueue, Nio).first { it.isAvailable }
        }

        /**
         * Set the channel factory and event loop groups for the given server bootstrap.
         *
         * @param bootstrap The server bootstrap to configure.
         * @param useNativeTransport Whether to use native transport (Epoll or KQueue).
         *
         * @return Parent and child group.
         */
        fun apply(bootstrap: ServerBootstrap, useNativeTransport: Boolean): Pair<EventLoopGroup, EventLoopGroup> {
            val type = if (useNativeTransport) available else Nio

            val parentGroup = type.newParentGroup()
            val childGroup = type.newChildGroup()
            bootstrap.group(parentGroup, childGroup)
                .channelFactory(ChannelFactory(type::newServerChannel))
            return parentGroup to childGroup
        }
    }
}
