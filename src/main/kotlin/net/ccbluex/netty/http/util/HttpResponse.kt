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

import com.google.gson.JsonElement
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.handler.codec.http.*
import org.apache.tika.Tika
import java.io.File
import java.io.InputStream

/**
 * Creates an HTTP response with the given status, content type, and content.
 *
 * @param status The HTTP response status.
 * @param contentType The content type of the response. Defaults to "text/plain".
 * @param content The content of the response, in [io.netty.buffer.ByteBuf].
 * @return A FullHttpResponse object.
 */
fun httpResponse(
    status: HttpResponseStatus,
    contentType: String = "text/plain",
    content: ByteBuf
): FullHttpResponse {
    val response = DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        status,
        content
    )

    val httpHeaders = response.headers()
    httpHeaders[HttpHeaderNames.CONTENT_TYPE] = contentType
    httpHeaders[HttpHeaderNames.CONTENT_LENGTH] = response.content().readableBytes()

    return response
}

/**
 * Creates an HTTP response with the given status, content type, and content.
 *
 * @param status The HTTP response status.
 * @param contentType The content type of the response. Defaults to "text/plain".
 * @param content The content of the response, in [String].
 * @return A FullHttpResponse object.
 */
fun httpResponse(
    status: HttpResponseStatus,
    contentType: String = "text/plain",
    content: String
): FullHttpResponse {
    val buf = PooledByteBufAllocator.DEFAULT.buffer(content.length * 3)
    buf.writeCharSequence(content, Charsets.UTF_8)
    return httpResponse(status, contentType, buf)
}

/**
 * Creates an HTTP response with the given status and JSON content.
 *
 * @param status The HTTP response status.
 * @param json The JSON content of the response.
 * @return A FullHttpResponse object.
 */
fun httpResponse(status: HttpResponseStatus, json: JsonElement) = httpResponse(
    status,
    "application/json",
    PooledByteBufAllocator.DEFAULT.writeJson(json)
)

/**
 * Creates an HTTP response with the given status and JSON content.
 *
 * @param status The HTTP response status.
 * @param json The JSON content of the response.
 * @return A FullHttpResponse object.
 */
fun <T : Any> httpResponse(status: HttpResponseStatus, json: T) = httpResponse(
    status,
    "application/json",
    PooledByteBufAllocator.DEFAULT.writeJson(json, json.javaClass)
)

/**
 * Creates an HTTP 200 OK response with the given JSON content.
 *
 * @param jsonElement The JSON content of the response.
 * @return A FullHttpResponse object.
 */
fun httpOk(jsonElement: JsonElement) = httpResponse(HttpResponseStatus.OK, jsonElement)

/**
 * Creates an HTTP 404 Not Found response with the given path and reason.
 *
 * @param path The path that was not found.
 * @param reason The reason for the 404 error.
 * @return A FullHttpResponse object.
 */
fun httpNotFound(path: String, reason: String): FullHttpResponse {
    data class ResponseBody(val path: String, val reason: String)
    return httpResponse(HttpResponseStatus.NOT_FOUND, ResponseBody(path, reason))
}

/**
 * Creates an HTTP 500 Internal Server Error response with the given exception message.
 *
 * @param exception The exception message.
 * @return A FullHttpResponse object.
 */
fun httpInternalServerError(exception: String): FullHttpResponse {
    data class ResponseBody(val reason: String)
    return httpResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, ResponseBody(exception))
}

/**
 * Creates an HTTP 403 Forbidden response with the given reason.
 *
 * @param reason The reason for the 403 error.
 * @return A FullHttpResponse object.
 */
fun httpForbidden(reason: String): FullHttpResponse {
    data class ResponseBody(val reason: String)
    return httpResponse(HttpResponseStatus.FORBIDDEN, ResponseBody(reason))
}

/**
 * Creates an HTTP 400 Bad Request response with the given reason.
 *
 * @param reason The reason for the 400 error.
 * @return A FullHttpResponse object.
 */
fun httpBadRequest(reason: String): FullHttpResponse {
    data class ResponseBody(val reason: String)
    return httpResponse(HttpResponseStatus.BAD_REQUEST, ResponseBody(reason))
}

private val tika = Tika()

/**
 * Creates an HTTP response for the given file.
 *
 * @param file The file to be included in the response.
 * @return A FullHttpResponse object.
 */
fun httpFile(file: File): FullHttpResponse {
    val buf = file.inputStream().channel.use { channel ->
        val size = channel.size().toInt()
        val buf = PooledByteBufAllocator.DEFAULT.buffer(size)
        while (buf.writableBytes() > 0) {
            val written = buf.writeBytes(channel, buf.writableBytes())
            if (written <= 0) break
        }
        buf
    }

    return httpResponse(
        status = HttpResponseStatus.OK,
        contentType = tika.detect(file),
        content = buf
    )
}

/**
 * Creates an HTTP response for the given input stream.
 *
 * @param stream The input stream to be included in the response.
 * @return A FullHttpResponse object.
 */
fun httpFileStream(stream: InputStream): FullHttpResponse {
    val allocator = PooledByteBufAllocator.DEFAULT
    val buf = allocator.buffer()

    val tmp = ByteArray(8192)
    while (true) {
        val read = stream.read(tmp)
        if (read == -1) break
        buf.writeBytes(tmp, 0, read)
    }

    return httpResponse(
        status = HttpResponseStatus.OK,
        contentType = tika.detect(buf.duplicate().inputStream()),
        content = buf
    )
}

/**
 * Creates an HTTP 204 No Content response.
 *
 * @return A FullHttpResponse object.
 */
fun httpNoContent(): FullHttpResponse {
    val response = DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        HttpResponseStatus.NO_CONTENT
    )

    val httpHeaders = response.headers()
    httpHeaders[HttpHeaderNames.CONTENT_LENGTH] = 0
    return response
}

/**
 * Creates an HTTP 405 Method Not Allowed response with the given method.
 *
 * @param method The method that is not allowed.
 * @return A FullHttpResponse object.
 */
fun httpMethodNotAllowed(method: String): FullHttpResponse {
    data class ResponseBody(val method: String)
    return httpResponse(HttpResponseStatus.METHOD_NOT_ALLOWED, ResponseBody(method))
}

/**
 * Creates an HTTP 401 Unauthorized response with the given reason.
 *
 * @param reason The reason for the 401 error.
 * @return A FullHttpResponse object.
 */
fun httpUnauthorized(reason: String): FullHttpResponse {
    data class ResponseBody(val reason: String)
    return httpResponse(HttpResponseStatus.UNAUTHORIZED, ResponseBody(reason))
}

/**
 * Creates an HTTP 429 Too Many Requests response with the given reason.
 *
 * @param reason The reason for the 429 error.
 * @return A FullHttpResponse object.
 */
fun httpTooManyRequests(reason: String): FullHttpResponse {
    data class ResponseBody(val reason: String)
    return httpResponse(HttpResponseStatus.TOO_MANY_REQUESTS, ResponseBody(reason))
}

/**
 * Creates an HTTP 503 Service Unavailable response with the given reason.
 *
 * @param reason The reason for the 503 error.
 * @return A FullHttpResponse object.
 */
fun httpServiceUnavailable(reason: String): FullHttpResponse {
    data class ResponseBody(val reason: String)
    return httpResponse(HttpResponseStatus.SERVICE_UNAVAILABLE, ResponseBody(reason))
}
