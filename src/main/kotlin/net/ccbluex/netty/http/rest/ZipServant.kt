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

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.EmptyHttpHeaders
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.ReadOnlyHttpHeaders
import net.ccbluex.netty.http.util.httpNotFound
import net.ccbluex.netty.http.model.RequestObject
import org.apache.tika.Tika
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

private val tika = Tika()

/**
 * Represents a zip servant in the routing tree that serves files from a zip archive kept in memory.
 *
 * @property part The part of the path this node represents.
 * @param zipInputStream The input stream of the zip file to load into memory.
 */
class ZipServant(part: String, zipInputStream: InputStream) : Node(part) {

    override val isExecutable = true

    /**
     * Data class representing a file entry in the zip archive.
     *
     * @property name The name/path of the file in the zip.
     * @property data The file content as byte array.
     * @property isDirectory Whether this entry is a directory.
     */
    private data class ZipFileEntry(
        val name: String,
        val data: ByteBuf,
        val isDirectory: Boolean,
    ) {
        private val headers =
            ReadOnlyHttpHeaders(
                false,
                HttpHeaderNames.CONTENT_TYPE, tika.detect(name),
                HttpHeaderNames.CONTENT_LENGTH, data.readableBytes().toString(),
            )

        fun toResponse(): FullHttpResponse {
            return DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                data.duplicate(),
                headers,
                EmptyHttpHeaders.INSTANCE,
            )
        }
    }

    private val zipFiles: Map<String, ZipFileEntry>

    init {
        zipFiles = loadZipData(zipInputStream)
    }

    /**
     * Loads the zip file data into memory.
     *
     * @param zipInputStream The input stream of the zip file.
     * @return A map of file paths to their data.
     */
    private fun loadZipData(zipInputStream: InputStream): Map<String, ZipFileEntry> {
        val files = mutableMapOf<String, ZipFileEntry>()

        ZipInputStream(zipInputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry

            while (entry != null) {
                val name = entry.name.removePrefix("/").removePrefix("./")
                val isDirectory = entry.isDirectory

                if (isDirectory) {
                    files[name] = ZipFileEntry(name, Unpooled.EMPTY_BUFFER, true)
                } else {
                    val data = zis.readBytes()
                    files[name] = ZipFileEntry(name, Unpooled.wrappedBuffer(data), false)
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        return files
    }

    override fun handle(request: RequestObject): FullHttpResponse {
        val path = request.remainingPath.removePrefix("/")
        val cleanPath = path.substringBefore("?")
        val sanitizedPath = cleanPath.replace("..", "")

        fun findFile(targetPath: String) =
            zipFiles[targetPath] ?: zipFiles["./$targetPath"] ?: zipFiles["/$targetPath"]

        fun isImplicitDirectory(targetPath: String): Boolean {
            val pathPrefix = if (targetPath.isEmpty()) "" else "$targetPath/"
            return zipFiles.keys.any { key ->
                key.startsWith(pathPrefix) && key != targetPath
            }
        }

        fun findIndexInDirectory(dirPath: String): ZipFileEntry? {
            val indexPath = if (dirPath.isEmpty()) "index.html" else "$dirPath/index.html"
            return findFile(indexPath)
        }

        // Extract directory path from fragments (e.g., "test/#/" -> "test")
        val fragmentIndex = sanitizedPath.indexOf("#")
        val directoryPath = if (fragmentIndex != -1) {
            sanitizedPath.take(fragmentIndex).removeSuffix("/")
        } else {
            sanitizedPath.removeSuffix("/")
        }

        // Try to find exact file match first (non-directory)
        val exactMatch = findFile(sanitizedPath)
        if (exactMatch != null && !exactMatch.isDirectory) {
            return exactMatch.toResponse()
        }

        // Handle directory requests or SPA routes
        when {
            // Case 1: Empty path (root) - serve root index.html
            sanitizedPath.isEmpty() -> {
                val indexEntry = findIndexInDirectory("")
                if (indexEntry != null && !indexEntry.isDirectory) {
                    return indexEntry.toResponse()
                }
            }

            // Case 2: Path ends with "/" - explicit directory request
            sanitizedPath.endsWith("/") -> {
                val indexEntry = findIndexInDirectory(directoryPath)
                if (indexEntry != null && !indexEntry.isDirectory) {
                    return indexEntry.toResponse()
                }
            }

            // Case 3: Path contains "#" - SPA route with fragment
            fragmentIndex != -1 -> {
                val indexEntry = findIndexInDirectory(directoryPath)
                if (indexEntry != null && !indexEntry.isDirectory) {
                    return indexEntry.toResponse()
                }
            }

            // Case 4: Check if path is an implicit directory and has index.html
            isImplicitDirectory(sanitizedPath) -> {
                val indexEntry = findIndexInDirectory(sanitizedPath)
                if (indexEntry != null && !indexEntry.isDirectory) {
                    return indexEntry.toResponse()
                }
            }
        }

        return httpNotFound(sanitizedPath, "File not found in zip archive")
    }
}
