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
import net.ccbluex.netty.http.model.RequestHandler
import net.ccbluex.netty.http.model.RequestObject

/**
 * Represents a route in the routing tree.
 *
 * @property part The part of the path this node represents.
 * @property method The HTTP method of the route.
 * @property handler The handler function for the route.
 */
open class Route(name: String, private val method: HttpMethod, val handler: RequestHandler)
    : Node(name) {
    override val isExecutable = true
    override suspend fun handle(request: RequestObject) = handler.handle(request)
    override fun matchesMethod(method: HttpMethod) =
        this.method == method && super.matchesMethod(method)

}