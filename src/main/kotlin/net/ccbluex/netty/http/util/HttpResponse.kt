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
import com.google.gson.JsonObject
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import org.apache.tika.Tika
import java.io.File
import java.io.InputStream

/**
 * Creates an HTTP response with the given status, content type, and content.
 *
 * @param status The HTTP response status.
 * @param contentType The content type of the response. Defaults to "text/plain".
 * @param content The content of the response.
 * @return A FullHttpResponse object.
 */
fun httpResponse(status: HttpResponseStatus, contentType: String = "text/plain",
                         content: String): FullHttpResponse {
    val response = DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        status,
        Unpooled.wrappedBuffer(content.toByteArray())
    )

    val httpHeaders = response.headers()
    httpHeaders[HttpHeaderNames.CONTENT_TYPE] = contentType
    httpHeaders[HttpHeaderNames.CONTENT_LENGTH] = response.content().readableBytes()
    httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN] = "*"
    httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS] = "GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS"
    httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS] = "Content-Type, Content-Length, Authorization, Accept, X-Requested-With"
    return response
}

/**
 * Creates an HTTP response with the given status and JSON content.
 *
 * @param status The HTTP response status.
 * @param json The JSON content of the response.
 * @return A FullHttpResponse object.
 */
fun httpResponse(status: HttpResponseStatus, json: JsonElement)
        = httpResponse(status, "application/json", Gson().toJson(json))

/**
 * Creates an HTTP 200 OK response with the given JSON content.
 *
 * @param jsonElement The JSON content of the response.
 * @return A FullHttpResponse object.
 */
fun httpOk(jsonElement: JsonElement)
        = httpResponse(HttpResponseStatus.OK, jsonElement)

/**
 * Creates an HTTP 404 Not Found response with the given path and reason.
 *
 * @param path The path that was not found.
 * @param reason The reason for the 404 error.
 * @return A FullHttpResponse object.
 */
fun httpNotFound(path: String, reason: String): FullHttpResponse {
    val jsonObject = JsonObject()
    jsonObject.addProperty("path", path)
    jsonObject.addProperty("reason", reason)
    return httpResponse(HttpResponseStatus.NOT_FOUND, jsonObject)
}

/**
 * Creates an HTTP 500 Internal Server Error response with the given exception message.
 *
 * @param exception The exception message.
 * @return A FullHttpResponse object.
 */
fun httpInternalServerError(exception: String): FullHttpResponse {
    val jsonObject = JsonObject()
    jsonObject.addProperty("reason", exception)
    return httpResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonObject)
}

/**
 * Creates an HTTP 403 Forbidden response with the given reason.
 *
 * @param reason The reason for the 403 error.
 * @return A FullHttpResponse object.
 */
fun httpForbidden(reason: String): FullHttpResponse {
    val jsonObject = JsonObject()
    jsonObject.addProperty("reason", reason)
    return httpResponse(HttpResponseStatus.FORBIDDEN, jsonObject)
}

/**
 * Creates an HTTP 400 Bad Request response with the given reason.
 *
 * @param reason The reason for the 400 error.
 * @return A FullHttpResponse object.
 */
fun httpBadRequest(reason: String): FullHttpResponse {
    val jsonObject = JsonObject()
    jsonObject.addProperty("reason", reason)
    return httpResponse(HttpResponseStatus.BAD_REQUEST, jsonObject)
}

private val tika = Tika()

/**
 * Creates an HTTP response for the given file.
 *
 * @param file The file to be included in the response.
 * @return A FullHttpResponse object.
 */
fun httpFile(file: File): FullHttpResponse {
    val response = DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        HttpResponseStatus.OK,
        Unpooled.wrappedBuffer(file.readBytes())
    )

    val httpHeaders = response.headers()
    httpHeaders[HttpHeaderNames.CONTENT_TYPE] = tika.detect(file)
    httpHeaders[HttpHeaderNames.CONTENT_LENGTH] = response.content().readableBytes()
    httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN] = "*"
    return response
}

/**
 * Creates an HTTP response for the given input stream.
 *
 * @param stream The input stream to be included in the response.
 * @return A FullHttpResponse object.
 */
fun httpFileStream(stream: InputStream): FullHttpResponse {
    val bytes = stream.readBytes()

    val response = DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        HttpResponseStatus.OK,
        Unpooled.wrappedBuffer(bytes)
    )

    val httpHeaders = response.headers()
    httpHeaders[HttpHeaderNames.CONTENT_TYPE] = tika.detect(bytes)
    httpHeaders[HttpHeaderNames.CONTENT_LENGTH] = response.content().readableBytes()
    httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN] = "*"

    return response
}