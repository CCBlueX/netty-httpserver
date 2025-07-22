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

import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.Base64
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path

/**
 * Reads the image at the given [path] and returns it as a base64 encoded string.
 */
@Deprecated(
    "Use Path.readAsBase64() instead",
    ReplaceWith("path.readAsBase64()")
)
fun readImageAsBase64(path: Path): String = path.readAsBase64()

/**
 * Reads the file and returns it as a base64 encoded string.
 */
fun Path.readAsBase64(): String {
    return FileChannel.open(this).use { channel ->
        val byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, Files.size(this))
        Base64.encode(Unpooled.wrappedBuffer(byteBuffer), false)
    }.toString(Charsets.UTF_8)
}
