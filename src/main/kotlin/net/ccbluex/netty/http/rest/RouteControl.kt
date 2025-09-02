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
import net.ccbluex.netty.http.util.httpFile
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpNotFound
import net.ccbluex.netty.http.model.RequestObject
import java.io.File

/**
 * Controller for handling routing of HTTP requests.
 */
class RouteController : Node("") {

    /**
     * Data class representing a destination node in the routing tree.
     *
     * @property length The length of the path to the destination.
     * @property destination The destination node.
     * @property params The parameters extracted from the path.
     * @property remainingPath The remaining part of the path after reaching the destination.
     */
    internal data class Destination(
        val destination: Node,
        val params: Map<String, String>,
        val remainingPath: String
    )

    /**
     * Finds the destination node for a given path and HTTP method.
     *
     * @param path The path to find the destination for.
     * @param method The HTTP method of the request.
     * @return The destination node and associated information, or null if no destination is found.
     */
    internal fun processPath(path: String, method: HttpMethod): Destination? {
        val pathArray = path.asPathArray()
        require(pathArray.isNotEmpty()) { "Path cannot be empty" }

        return travelNode(this, pathArray, method, 0, mutableMapOf())
    }

    /**
     * Recursively find the destination node by traversing through deeper nodes first.
     *
     * @param currentNode The current node in the traversal.
     * @param pathArray The array of path segments.
     * @param method The HTTP method of the request.
     * @param index The current index in the path array.
     * @param params The map of parameters extracted from the path.
     * @return The destination node and associated information, or null if no destination is found.
     */
    private fun travelNode(
        currentNode: Node,
        pathArray: Array<String>,
        method: HttpMethod,
        index: Int,
        params: MutableMap<String, String>
    ): Destination? {
        if (index < pathArray.size) {
            val part = pathArray[index]
            val matchingNodes = currentNode.nodes.filter { it.matches(index, part) }

            for (node in matchingNodes) {
                val newParams = params.toMutableMap()

                if (node.isParam) {
                    newParams[node.part.substring(1)] = part
                }

                val result = travelNode(node, pathArray, method, index + 1, newParams)
                if (result != null) {
                    return result
                }
            }
        }

        return if (currentNode.matchesMethod(method)) {
            // remainingPath should only include the unmatched trailing segments,
            // not the already matched route path
            val remaining = if (index >= pathArray.size) "" else pathArray.copyOfRange(index, pathArray.size).joinToString("/")
            Destination(currentNode, params, remaining)
        } else {
            null
        }
    }

}

/**
 * Represents a node in the routing tree.
 *
 * @property part The part of the path this node represents.
 */
@Suppress("TooManyFunctions")
open class Node(val part: String) {

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
    fun route(path: String, method: HttpMethod, handler: (RequestObject) -> FullHttpResponse) =
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

    fun get(path: String, handler: (RequestObject) -> FullHttpResponse)
            = route(path, HttpMethod.GET, handler)

    fun post(path: String, handler: (RequestObject) -> FullHttpResponse)
            = route(path, HttpMethod.POST, handler)

    fun put(path: String, handler: (RequestObject) -> FullHttpResponse)
            = route(path, HttpMethod.PUT, handler)

    fun delete(path: String, handler: (RequestObject) -> FullHttpResponse)
            = route(path, HttpMethod.DELETE, handler)

    fun patch(path: String, handler: (RequestObject) -> FullHttpResponse)
            = route(path, HttpMethod.PATCH, handler)

    fun head(path: String, handler: (RequestObject) -> FullHttpResponse)
            = route(path, HttpMethod.HEAD, handler)

    fun options(path: String, handler: (RequestObject) -> FullHttpResponse)
            = route(path, HttpMethod.OPTIONS, handler)

    fun trace(path: String, handler: (RequestObject) -> FullHttpResponse)
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
    open fun handleRequest(requestObject: RequestObject): FullHttpResponse {
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
 * Represents a route in the routing tree.
 *
 * @property part The part of the path this node represents.
 * @property method The HTTP method of the route.
 * @property handler The handler function for the route.
 */
open class Route(name: String, private val method: HttpMethod, val handler: (RequestObject) -> FullHttpResponse)
    : Node(name) {
    override val isExecutable = true
    override fun handleRequest(requestObject: RequestObject) = handler(requestObject)
    override fun matchesMethod(method: HttpMethod) =
        this.method == method && super.matchesMethod(method)

}

/**
 * Represents a file servant in the routing tree.
 *
 * @property part The part of the path this node represents.
 * @property baseFolder The base folder for serving files.
 */
class FileServant(part: String, private val baseFolder: File) : Node(part) {

    override val isExecutable = true

    override fun handleRequest(requestObject: RequestObject): FullHttpResponse {
        val path = requestObject.remainingPath
        val sanitizedPath = path.replace("..", "")
        val file = baseFolder.resolve(sanitizedPath)

        return when {
            !file.exists() -> httpNotFound(path, "File not found")
            !file.isFile -> {
                val indexFile = file.resolve("index.html")

                when {
                    indexFile.exists() && indexFile.isFile -> httpFile(indexFile)
                    else -> httpForbidden("File is not a file")
                }
            }
            file.isHidden -> httpForbidden("File is hidden")
            else -> httpFile(file)
        }
    }

    override fun matches(index: Int, part: String) = super.matches(index, part) || index == 0 && isRoot

    override fun matchesMethod(method: HttpMethod) =
        method == HttpMethod.GET && super.matchesMethod(method)

}

/**
 * Convert a string to an array of path parts and drop the first empty part.
 *
 * @return An array of path parts.
 */
private fun String.asPathArray(): Array<String> {
    val parts = split("/")
    return if (parts.size <= 1) emptyArray()
    else parts.subList(1, parts.size).toTypedArray()
}