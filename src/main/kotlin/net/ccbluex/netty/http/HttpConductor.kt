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

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import net.ccbluex.netty.http.HttpServer.Companion.logger
import net.ccbluex.netty.http.model.RequestContext
import net.ccbluex.netty.http.util.httpBadRequest
import net.ccbluex.netty.http.util.httpInternalServerError
import net.ccbluex.netty.http.util.httpNotFound
import net.ccbluex.netty.http.model.RequestObject

internal class HttpConductor(private val server: HttpServer) {

    /**
     * Processes the incoming request context and returns the response.
     *
     * @param context The request context to process.
     * @return The response to the request.
     */
    fun processRequestContext(context: RequestContext) = runCatching {
        val content = context.contentBuffer.toString()
        val method = context.httpMethod

        logger.debug("Request {}", context)

        if (!context.headers["content-length"].isNullOrEmpty() &&
            context.headers["content-length"]?.toInt() != content.toByteArray(Charsets.UTF_8).size) {
            logger.warn("Received incomplete request: $context")
            return@runCatching httpBadRequest("Incomplete request")
        }

        if (method == HttpMethod.OPTIONS) {
            val response = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(ByteArray(0))
            )

            val httpHeaders = response.headers()
            httpHeaders[HttpHeaderNames.CONTENT_TYPE] = "text/plain"
            httpHeaders[HttpHeaderNames.CONTENT_LENGTH] = response.content().readableBytes()
            httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN] = "*"
            httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS] = "GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS"
            httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS] = "Content-Type, Content-Length, Authorization, Accept, X-Requested-With"
            return@runCatching response
        }

        val (node, params, remaining) = server.routeController.processPath(context.path, method) ?:
            return@runCatching httpNotFound(context.path, "Route not found")

        logger.debug("Found destination {}", node)
        val requestObject = RequestObject(
            uri = context.uri,
            path = context.path,
            remainingPath = remaining,
            method = method,
            body = content,
            params = params,
            queryParams = context.params,
            headers = context.headers
        )

        return@runCatching node.handleRequest(requestObject)
    }.getOrElse {
        logger.error("Error while processing request object: $context", it)
        httpInternalServerError(it.message ?: "Unknown error")
    }


}
