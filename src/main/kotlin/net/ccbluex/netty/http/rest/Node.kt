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
package net.ccbluex.netty.http.rest

import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpMethod
import net.ccbluex.netty.http.application.ApplicationCall
import net.ccbluex.netty.http.model.RequestHandler
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.routing.RoutingContext
import net.ccbluex.netty.http.routing.RoutingHandler
import java.io.File
import java.io.InputStream

/**
 * Represents a node in the routing tree.
 *
 * @property part The part of the path this node represents.
 */
@Suppress("TooManyFunctions")
open class Node(val part: String) {

    open val isRoot = part.isEmpty()
    open val isExecutable get() = handlers.isNotEmpty()
    val isParam = part.startsWith(":")

    internal val nodes = mutableListOf<Node>()
    private val handlers = linkedMapOf<HttpMethod, RoutingHandler>()

    init {
        if (part.contains("/")) {
            error("Part cannot contain slashes")
        }
    }

    /**
     * Adds a path to the routing tree and executes a block to configure the path.
     *
     * @param path The path of the route.
     * @param block The block to execute for the path.
     */
    fun withPath(path: String, block: Node.() -> Unit): Node =
        chain(::Node, path.toPathArray()).apply(block)

    /**
     * Adds a path node to the routing tree and executes a block to configure its children.
     *
     * This mirrors Ktor's `route("/path") { ... }` grouping style while keeping the
     * existing path-based route registration API available.
     */
    fun route(path: String, block: Node.() -> Unit): Node = withPath(path, block)

    /**
     * Adds a route to the routing tree.
     *
     * @param path The path of the route.
     * @param method The HTTP method of the route.
     * @param handler The handler function for the route.
     * @return The node representing the route.
     */
    fun route(path: String, method: HttpMethod, handler: RoutingHandler): Node =
        chain(::Node, path.toPathArray()).apply {
            registerMethodHandler(method, handler)
        }

    @Deprecated("Use RoutingContext handlers")
    fun route(path: String, method: HttpMethod, handler: RequestHandler): Node =
        route(path, method, handler.asRoutingHandler())

    /**
     * Adds a file servant to the routing tree.
     *
     * @param path The path of the file servant.
     * @param baseFolder The base folder for serving files.
     * @return The node representing the file servant.
     */
    fun file(path: String, baseFolder: File) =
        chain({ FileServant(it, baseFolder) }, path.toPathArray())

    /**
     * Adds a zip servant to the routing tree.
     *
     * @param path The path of the zip servant.
     * @param zipInputStream The input stream of the zip file.
     * @return The node representing the zip servant.
     */
    fun zip(path: String, zipInputStream: InputStream) =
        chain({ ZipServant(it, zipInputStream) }, path.toPathArray())

    fun get(handler: RoutingHandler) = registerMethodHandler(HttpMethod.GET, handler)

    fun get(path: String, handler: RoutingHandler)
            = route(path, HttpMethod.GET, handler)

    @Deprecated("Use RoutingContext handlers")
    fun get(handler: RequestHandler) = registerMethodHandler(HttpMethod.GET, handler.asRoutingHandler())

    @Deprecated("Use RoutingContext handlers")
    fun get(path: String, handler: RequestHandler) = route(path, HttpMethod.GET, handler)

    fun post(handler: RoutingHandler) = registerMethodHandler(HttpMethod.POST, handler)

    fun post(path: String, handler: RoutingHandler)
            = route(path, HttpMethod.POST, handler)

    @Deprecated("Use RoutingContext handlers")
    fun post(handler: RequestHandler) = registerMethodHandler(HttpMethod.POST, handler.asRoutingHandler())

    @Deprecated("Use RoutingContext handlers")
    fun post(path: String, handler: RequestHandler) = route(path, HttpMethod.POST, handler)

    fun put(handler: RoutingHandler) = registerMethodHandler(HttpMethod.PUT, handler)

    fun put(path: String, handler: RoutingHandler)
            = route(path, HttpMethod.PUT, handler)

    @Deprecated("Use RoutingContext handlers")
    fun put(handler: RequestHandler) = registerMethodHandler(HttpMethod.PUT, handler.asRoutingHandler())

    @Deprecated("Use RoutingContext handlers")
    fun put(path: String, handler: RequestHandler) = route(path, HttpMethod.PUT, handler)

    fun delete(handler: RoutingHandler) = registerMethodHandler(HttpMethod.DELETE, handler)

    fun delete(path: String, handler: RoutingHandler)
            = route(path, HttpMethod.DELETE, handler)

    @Deprecated("Use RoutingContext handlers")
    fun delete(handler: RequestHandler) = registerMethodHandler(HttpMethod.DELETE, handler.asRoutingHandler())

    @Deprecated("Use RoutingContext handlers")
    fun delete(path: String, handler: RequestHandler) = route(path, HttpMethod.DELETE, handler)

    fun patch(handler: RoutingHandler) = registerMethodHandler(HttpMethod.PATCH, handler)

    fun patch(path: String, handler: RoutingHandler)
            = route(path, HttpMethod.PATCH, handler)

    @Deprecated("Use RoutingContext handlers")
    fun patch(handler: RequestHandler) = registerMethodHandler(HttpMethod.PATCH, handler.asRoutingHandler())

    @Deprecated("Use RoutingContext handlers")
    fun patch(path: String, handler: RequestHandler) = route(path, HttpMethod.PATCH, handler)

    fun head(handler: RoutingHandler) = registerMethodHandler(HttpMethod.HEAD, handler)

    fun head(path: String, handler: RoutingHandler)
            = route(path, HttpMethod.HEAD, handler)

    @Deprecated("Use RoutingContext handlers")
    fun head(handler: RequestHandler) = registerMethodHandler(HttpMethod.HEAD, handler.asRoutingHandler())

    @Deprecated("Use RoutingContext handlers")
    fun head(path: String, handler: RequestHandler) = route(path, HttpMethod.HEAD, handler)

    fun options(handler: RoutingHandler) = registerMethodHandler(HttpMethod.OPTIONS, handler)

    fun options(path: String, handler: RoutingHandler)
            = route(path, HttpMethod.OPTIONS, handler)

    @Deprecated("Use RoutingContext handlers")
    fun options(handler: RequestHandler) = registerMethodHandler(HttpMethod.OPTIONS, handler.asRoutingHandler())

    @Deprecated("Use RoutingContext handlers")
    fun options(path: String, handler: RequestHandler) = route(path, HttpMethod.OPTIONS, handler)

    fun trace(handler: RoutingHandler) = registerMethodHandler(HttpMethod.TRACE, handler)

    fun trace(path: String, handler: RoutingHandler)
            = route(path, HttpMethod.TRACE, handler)

    @Deprecated("Use RoutingContext handlers")
    fun trace(handler: RequestHandler) = registerMethodHandler(HttpMethod.TRACE, handler.asRoutingHandler())

    @Deprecated("Use RoutingContext handlers")
    fun trace(path: String, handler: RequestHandler) = route(path, HttpMethod.TRACE, handler)

    /**
     * Chains nodes together to form a path in the routing tree.
     *
     * @param destination The function to create the destination node.
     * @param parts The parts of the path.
     * @return The final node in the chain.
     */
    private fun chain(destination: (String) -> Node, parts: List<String>): Node {
        return when (parts.size) {
            0 -> throw IllegalArgumentException("Parts cannot be empty")
            1 -> nodes.find { it.part == parts[0] } ?: destination(parts[0]).also { nodes += it }
            else -> {
                val node = nodes.find { it.part == parts[0] } ?: Node(parts[0]).also { nodes += it }
                node.chain(destination, parts.subList(1, parts.size))
            }
        }
    }

    /**
     * Handles an HTTP request.
     *
     * @param call The HTTP call object.
     * @return The HTTP response.
     */
    open suspend fun handle(call: ApplicationCall) = handlers[call.method]?.let { handler ->
        handler(RoutingContext(call, this))
        call.takeResponse()
    } ?: throw NotImplementedError()

    /**
     * Checks if the node matches a part of the path and HTTP method.
     *
     * @param part The part of the path.
     * @return True if the node matches, false otherwise.
     */
    open fun matches(index: Int, part: String) =
        this.part.equals(part, true) || isParam

    /**
     * Checks if the node matches a part of the path and HTTP method.
     */
    open fun matchesMethod(method: HttpMethod) = handlers.containsKey(method)

    internal fun registerMethodHandler(method: HttpMethod, handler: RoutingHandler): Node = apply {
        handlers[method] = handler
    }

}

private fun RequestHandler.asRoutingHandler(): RoutingHandler = {
    respond(handle(call.toLegacyRequestObject()))
}

private fun ApplicationCall.toLegacyRequestObject() = RequestObject(
    uri = uri,
    path = path,
    remainingPath = remainingPath,
    method = method,
    body = body,
    params = parameters,
    queryParams = queryParameters,
    headers = headers
)

/**
 * Convert a string to an array of path parts and drop the first empty part.
 *
 * @return An array of path parts.
 */
internal fun String.toPathArray(): List<String> {
    val parts = split('/')
    return if (parts.size <= 1) emptyList()
    else parts.subList(1, parts.size)
}
