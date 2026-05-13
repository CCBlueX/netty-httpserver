package net.ccbluex.netty.http.application

import com.google.gson.Gson
import com.google.gson.JsonElement
import io.netty.buffer.PooledByteBufAllocator
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import net.ccbluex.netty.http.util.DEFAULT_GSON
import net.ccbluex.netty.http.util.httpBadRequest
import net.ccbluex.netty.http.util.httpFile
import net.ccbluex.netty.http.util.httpFileStream
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpInternalServerError
import net.ccbluex.netty.http.util.httpNoContent
import net.ccbluex.netty.http.util.httpNotFound
import net.ccbluex.netty.http.util.httpResponse
import net.ccbluex.netty.http.util.httpServiceUnavailable
import net.ccbluex.netty.http.util.httpUnauthorized
import net.ccbluex.netty.http.util.inputStream
import net.ccbluex.netty.http.util.outputStream
import net.ccbluex.netty.http.util.tika
import java.io.File
import java.io.InputStream
import java.io.OutputStream

open class ApplicationCall(
    val uri: String,
    val path: String,
    val remainingPath: String,
    val method: HttpMethod,
    val body: String,
    val parameters: Map<String, String>,
    val queryParameters: Map<String, String>,
    val headers: HttpHeaders
) {

    private var response: FullHttpResponse? = null

    inline fun <reified T> receive(gson: Gson = DEFAULT_GSON): T = gson.fromJson(body, T::class.java)

    fun respond(response: FullHttpResponse) {
        this.response = response
    }

    @JvmOverloads
    fun respond(status: HttpResponseStatus, body: JsonElement, gson: Gson = DEFAULT_GSON) {
        respond(httpResponse(status, body, gson))
    }

    @JvmOverloads
    fun respond(status: HttpResponseStatus, body: Any, gson: Gson = DEFAULT_GSON) {
        respond(httpResponse(status, body, gson))
    }

    @JvmOverloads
    fun respond(body: JsonElement, gson: Gson = DEFAULT_GSON) {
        respond(HttpResponseStatus.OK, body, gson)
    }

    @JvmOverloads
    fun respond(body: Any, gson: Gson = DEFAULT_GSON) {
        respond(HttpResponseStatus.OK, body, gson)
    }

    fun respondNoContent() {
        respond(httpNoContent())
    }

    fun respondFile(file: File) {
        respond(httpFile(file))
    }

    @JvmOverloads
    fun respondFileStream(
        stream: InputStream,
        contentType: String? = null,
        contentLength: Int = 256,
    ) {
        respond(httpFileStream(stream, contentType, contentLength))
    }

    @JvmOverloads
    fun respondOutputStream(
        contentType: String? = null,
        status: HttpResponseStatus = HttpResponseStatus.OK,
        contentLength: Int = 256,
        producer: OutputStream.() -> Unit,
    ) {
        val allocator = PooledByteBufAllocator.DEFAULT
        val buf = allocator.buffer(contentLength)

        buf.outputStream().use {
            producer(it)
        }

        respond(httpResponse(status, contentType ?: tika.detect(buf.duplicate().inputStream()), buf))
    }

    fun takeResponse(): FullHttpResponse {
        return response ?: throw IllegalStateException("Route handler completed without responding for $method $path")
    }

    fun badRequest(reason: String): Nothing = abort(httpBadRequest(reason))

    fun forbidden(reason: String): Nothing = abort(httpForbidden(reason))

    fun unauthorized(reason: String): Nothing = abort(httpUnauthorized(reason))

    fun notFound(path: String, reason: String): Nothing = abort(httpNotFound(path, reason))

    fun serviceUnavailable(reason: String): Nothing = abort(httpServiceUnavailable(reason))

    fun internalServerError(reason: String): Nothing = abort(httpInternalServerError(reason))

    private fun abort(response: FullHttpResponse): Nothing = throw ResponseException(response)
}

class ResponseException(val response: FullHttpResponse) : RuntimeException(null, null, false, false)
