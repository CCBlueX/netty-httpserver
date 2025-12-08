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
package net.ccbluex.netty.http.model

import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpMethod
import java.io.ByteArrayOutputStream

data class RequestContext(var httpMethod: HttpMethod, var uri: String, var headers: HttpHeaders) {
    val contentBuffer = ByteArrayOutputStream()
    val path = uri.substringBefore('?', uri)
    val params = getUriParams(uri)
}

/**
 * The received uri should be like: '...?param1=value&param2=value'
 */
private fun getUriParams(uri: String): Map<String, String> {
    val queryString = uri.substringAfter('?', "")

    if (queryString.isEmpty()) {
        return emptyMap()
    }

    // in case of duplicated params, will be used the last value
    return queryString.split('&')
        .mapNotNull { param ->
            val index = param.indexOf('=')
            if (index == -1) null
            else {
                val key = param.take(index)
                val value = param.substring(index + 1)
                if (key.isNotEmpty()) key to value else null
            }
        }.toMap()
}
