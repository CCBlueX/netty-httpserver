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
import net.ccbluex.netty.http.application.ApplicationCall
import net.ccbluex.netty.http.util.DEFAULT_GSON

/**
 * Compatibility wrapper for the pre-2.6 request API.
 *
 * New code should use [ApplicationCall] inside a routing context instead.
 */
@Deprecated("Use ApplicationCall within RoutingContext handlers")
class RequestObject(
    uri: String,
    path: String,
    remainingPath: String,
    method: HttpMethod,
    body: String,
    params: Map<String, String>,
    queryParams: Map<String, String>,
    headers: HttpHeaders
) : ApplicationCall(
    uri = uri,
    path = path,
    remainingPath = remainingPath,
    method = method,
    body = body,
    parameters = params,
    queryParameters = queryParams,
    headers = headers
) {

    val params: Map<String, String> get() = parameters
    val queryParams: Map<String, String> get() = queryParameters

    /**
     * Converts the body of the request to a JSON object of the specified type.
     *
     * @return The JSON object of the specified type.
     */
    inline fun <reified T> asJson(): T {
        return receive(GSON_INSTANCE)
    }

    companion object {
        @JvmField
        val GSON_INSTANCE = DEFAULT_GSON
    }

}
