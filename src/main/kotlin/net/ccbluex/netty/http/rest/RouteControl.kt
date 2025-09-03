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

import io.netty.handler.codec.http.HttpMethod

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
