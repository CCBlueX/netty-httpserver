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
 * Represents a file servant in the routing tree.
 *
 * @property part The part of the path this node represents.
 * @property baseFolder The base folder for serving files.
 */
class FileServant(part: String, private val baseFolder: File) : Node(part) {

    override val isExecutable = true

    override suspend fun handle(request: RequestObject): FullHttpResponse {
        val path = request.remainingPath
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