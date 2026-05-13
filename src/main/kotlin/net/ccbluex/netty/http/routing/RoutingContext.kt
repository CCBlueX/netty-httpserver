package net.ccbluex.netty.http.routing

import com.google.gson.Gson
import com.google.gson.JsonElement
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import net.ccbluex.netty.http.application.ApplicationCall
import net.ccbluex.netty.http.rest.Node
import java.io.File
import java.io.InputStream
import java.lang.reflect.Type

typealias RoutingHandler = suspend RoutingContext.() -> Unit

class RoutingContext(
    val call: ApplicationCall,
    val route: Node,
) {
    val method: HttpMethod get() = call.method
    val uri: String get() = call.uri
    val path: String get() = call.path
    val remainingPath: String get() = call.remainingPath
    val parameters: Map<String, String> get() = call.parameters
    val queryParameters: Map<String, String> get() = call.queryParameters
    val headers: HttpHeaders get() = call.headers
    val body: String get() = call.body

    inline fun <reified T> receive(gson: Gson = net.ccbluex.netty.http.util.DEFAULT_GSON): T = call.receive(gson)

    fun respond(response: io.netty.handler.codec.http.FullHttpResponse) = call.respond(response)

    @JvmOverloads
    fun respond(status: HttpResponseStatus, body: JsonElement, gson: Gson = net.ccbluex.netty.http.util.DEFAULT_GSON) =
        call.respond(status, body, gson)

    @JvmOverloads
    fun respond(status: HttpResponseStatus, body: Any, gson: Gson = net.ccbluex.netty.http.util.DEFAULT_GSON) =
        call.respond(status, body, gson)

    @JvmOverloads
    fun respond(body: JsonElement, gson: Gson = net.ccbluex.netty.http.util.DEFAULT_GSON) = call.respond(body, gson)

    @JvmOverloads
    fun respond(body: Any, gson: Gson = net.ccbluex.netty.http.util.DEFAULT_GSON) = call.respond(body, gson)

    fun respondNoContent() = call.respondNoContent()

    fun respondFile(file: File) = call.respondFile(file)

    @JvmOverloads
    fun respondFileStream(stream: InputStream, contentType: String? = null, contentLength: Int = 256) =
        call.respondFileStream(stream, contentType, contentLength)

    fun badRequest(reason: String): Nothing = call.badRequest(reason)
    fun forbidden(reason: String): Nothing = call.forbidden(reason)
    fun unauthorized(reason: String): Nothing = call.unauthorized(reason)
    fun notFound(path: String, reason: String): Nothing = call.notFound(path, reason)
    fun internalServerError(reason: String): Nothing = call.internalServerError(reason)
}
