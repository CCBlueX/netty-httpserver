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

import com.google.gson.Gson
import com.google.gson.JsonElement
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import java.lang.reflect.Type

internal val DEFAULT_GSON = Gson()

/**
 * Serialize [json] into [ByteBuf] with given [ByteBufAllocator].
 */
@JvmOverloads
fun ByteBufAllocator.writeJson(
    json: JsonElement,
    gson: Gson = DEFAULT_GSON,
): ByteBuf {
    val buf = buffer(256, Int.MAX_VALUE)
    buf.outputStream().writer(Charsets.UTF_8).use {
        gson.toJson(json, it)
    }
    return buf
}

/**
 * Serialize [obj] as [type] into [ByteBuf] with given [ByteBufAllocator].
 */
@JvmOverloads
fun <T : Any> ByteBufAllocator.writeJson(
    obj: T,
    type: Type = obj.javaClass,
    gson: Gson = DEFAULT_GSON,
): ByteBuf {
    val buf = buffer(256, Int.MAX_VALUE)
    buf.outputStream().writer(Charsets.UTF_8).use {
        gson.toJson(obj, type, it)
    }
    return buf
}
