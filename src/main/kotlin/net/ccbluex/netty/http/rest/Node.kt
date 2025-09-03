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
import net.ccbluex.netty.http.model.RequestHandler
import net.ccbluex.netty.http.model.RequestObject
import java.io.File
import java.io.InputStream

/**
 * Represents a node in the routing tree.
 *
 * @property part The part of the path this node represents.
 */
@Suppress("TooManyFunctions")
open class Node(val part: String) : RequestHandler {

    open val isRoot = part.isEmpty()
    open val isExecutable = false
    val isParam = part.startsWith(":")

    internal val nodes = mutableListOf<Node>()

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
    fun withPath(path: String, block: Node.() -> Unit) {
        chain({ Node(it).apply(block) }, *path.asPathArray())
    }

    /**
     * Adds a route to the routing tree.
     *
     * @param path The path of the route.
     * @param method The HTTP method of the route.
     * @param handler The handler function for the route.
     * @return The node representing the route.
     */
    fun route(path: String, method: HttpMethod, handler: RequestHandler) =
        chain({ Route(it, method, handler) }, *path.asPathArray())

    /**
     * Adds a file servant to the routing tree.
     *
     * @param path The path of the file servant.
     * @param baseFolder The base folder for serving files.
     * @return The node representing the file servant.
     */
    fun file(path: String, baseFolder: File) =
        chain({ FileServant(it, baseFolder) }, *path.asPathArray())

    /**
     * Adds a zip servant to the routing tree.
     *
     * @param path The path of the zip servant.
     * @param zipInputStream The input stream of the zip file.
     * @return The node representing the zip servant.
     */
    fun zip(path: String, zipInputStream: InputStream) =
        chain({ ZipServant(it, zipInputStream) }, *path.asPathArray())

    fun get(path: String, handler: RequestHandler)
            = route(path, HttpMethod.GET, handler)

    fun post(path: String, handler: RequestHandler)
            = route(path, HttpMethod.POST, handler)

    fun put(path: String, handler: RequestHandler)
            = route(path, HttpMethod.PUT, handler)

    fun delete(path: String, handler: RequestHandler)
            = route(path, HttpMethod.DELETE, handler)

    fun patch(path: String, handler: RequestHandler)
            = route(path, HttpMethod.PATCH, handler)

    fun head(path: String, handler: RequestHandler)
            = route(path, HttpMethod.HEAD, handler)

    fun options(path: String, handler: RequestHandler)
            = route(path, HttpMethod.OPTIONS, handler)

    fun trace(path: String, handler: RequestHandler)
            = route(path, HttpMethod.TRACE, handler)

    /**
     * Chains nodes together to form a path in the routing tree.
     *
     * @param destination The function to create the destination node.
     * @param parts The parts of the path.
     * @return The final node in the chain.
     */
    private fun chain(destination: (String) -> Node, vararg parts: String): Node {
        return when (parts.size) {
            0 -> throw IllegalArgumentException("Parts cannot be empty")
            1 -> destination(parts[0]).also { nodes += it }
            else -> {
                val node = nodes.find { it.part == parts[0] } ?: Node(parts[0]).also { nodes += it }
                node.chain(destination, *parts.copyOfRange(1, parts.size))
            }
        }
    }

    /**
     * Handles an HTTP request.
     *
     * @param requestObject The request object.
     * @return The HTTP response.
     */
    override suspend fun handleRequest(requestObject: RequestObject): FullHttpResponse {
        error("Node does not implement handleRequest")
    }

    /**
     * Checks if the node matches a part of the path and HTTP method.
     *
     * @param part The part of the path.
     * @param method The HTTP method.
     * @return True if the node matches, false otherwise.
     */
    open fun matches(index: Int, part: String) =
        this.part.equals(part, true) || isParam

    /**
     * Checks if the node matches a part of the path and HTTP method.
     */
    open fun matchesMethod(method: HttpMethod) = isExecutable

}

/**
 * Convert a string to an array of path parts and drop the first empty part.
 *
 * @return An array of path parts.
 */
internal fun String.asPathArray(): Array<String> {
    val parts = split("/")
    return if (parts.size <= 1) emptyArray()
    else parts.subList(1, parts.size).toTypedArray()
}