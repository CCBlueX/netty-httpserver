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
@file:Suppress("NOTHING_TO_INLINE")
package net.ccbluex.netty.http.util

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream

inline fun ByteBuf.inputStream() = ByteBufInputStream(this)

inline fun ByteBuf.outputStream() = ByteBufOutputStream(this)

internal fun ByteBufAllocator.copyOf(byteBuf: ByteBuf): ByteBuf {
    val copy = this.buffer(byteBuf.readableBytes())
    copy.writeBytes(byteBuf)
    return copy
}
