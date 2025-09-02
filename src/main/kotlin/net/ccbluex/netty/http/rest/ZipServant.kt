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
import net.ccbluex.netty.http.util.httpFileStream
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpNotFound
import net.ccbluex.netty.http.model.RequestObject
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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
        val data: ByteArray,
        val isDirectory: Boolean
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ZipFileEntry

            if (name != other.name) return false
            if (!data.contentEquals(other.data)) return false
            if (isDirectory != other.isDirectory) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + data.contentHashCode()
            result = 31 * result + isDirectory.hashCode()
            return result
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
                    files[name] = ZipFileEntry(name, ByteArray(0), true)
                } else {
                    val data = zis.readBytes()
                    files[name] = ZipFileEntry(name, data, false)
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        return files
    }

    override fun handleRequest(requestObject: RequestObject): FullHttpResponse {
        val path = requestObject.remainingPath.removePrefix("/")
        val cleanPath = path.substringBefore("#").substringBefore("?")
        val sanitizedPath = cleanPath.replace("..", "")

        fun findFile(targetPath: String): ZipFileEntry? {
            return zipFiles[targetPath]
                ?: zipFiles["./$targetPath"]
                ?: zipFiles["/$targetPath"]
        }

        fun isImplicitDirectory(targetPath: String): Boolean {
            val pathPrefix = if (targetPath.isEmpty()) "" else "$targetPath/"
            return zipFiles.keys.any { key ->
                key.startsWith(pathPrefix) && key != targetPath && !key.removePrefix(pathPrefix).contains("/")
            }
        }

        if (sanitizedPath.isEmpty() || path.contains("#")) {
            val indexEntry = findFile("index.html")
            if (indexEntry != null && !indexEntry.isDirectory) {
                return httpFileStream(
                    stream = ByteArrayInputStream(indexEntry.data),
                    contentLength = indexEntry.data.size
                )
            }
        }

        val exactMatch = findFile(sanitizedPath)
        if (exactMatch != null) {
            return when {
                exactMatch.isDirectory -> {
                    // This is an explicit directory entry, try to serve index.html from it
                    val indexPath = if (sanitizedPath.isEmpty()) "index.html" else "$sanitizedPath/index.html"
                    val indexEntry = findFile(indexPath)

                    if (indexEntry != null && !indexEntry.isDirectory) {
                        httpFileStream(
                            stream = ByteArrayInputStream(indexEntry.data),
                            contentLength = indexEntry.data.size
                        )
                    } else {
                        httpForbidden("Directory listing not allowed")
                    }
                }
                else -> {
                    // Regular file
                    httpFileStream(
                        stream = ByteArrayInputStream(exactMatch.data),
                        contentLength = exactMatch.data.size
                    )
                }
            }
        }

        // Check if this could be an implicit directory (has files under it)
        if (isImplicitDirectory(sanitizedPath)) {
            val indexPath = if (sanitizedPath.isEmpty()) "index.html" else "$sanitizedPath/index.html"
            val indexEntry = findFile(indexPath)

            return if (indexEntry != null && !indexEntry.isDirectory) {
                httpFileStream(
                    stream = ByteArrayInputStream(indexEntry.data),
                    contentLength = indexEntry.data.size
                )
            } else {
                httpForbidden("Directory listing not allowed")
            }
        }

        return httpNotFound(sanitizedPath, "File not found in zip archive")
    }
}
