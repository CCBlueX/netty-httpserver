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

import com.google.gson.Gson
import io.netty.handler.codec.http.HttpMethod

/**
 * Represents an HTTP request object.
 *
 * @property uri The full URI of the request.
 * @property path The path of the request.
 * @property remainingPath The ending part of the path which was not matched by the route.
 * @property method The HTTP method of the request.
 * @property body The body of the request.
 * @property params The inline URI parameters of the request.
 * @property queryParams The query parameters of the request.
 * @property headers The headers of the request.
 */
data class RequestObject(
    val uri: String,
    val path: String,
    val remainingPath: String,
    val method: HttpMethod,
    val body: String,
    val params: Map<String, String>,
    val queryParams: Map<String, String>,
    val headers: Map<String, String>
) {

    /**
     * Converts the body of the request to a JSON object of the specified type.
     *
     * @return The JSON object of the specified type.
     */
    inline fun <reified T> asJson(): T {
        return Gson().fromJson(body, T::class.java)
    }

}