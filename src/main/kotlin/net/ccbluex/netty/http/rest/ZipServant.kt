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
        val sanitizedPath = path.replace("..", "")

        return when {
            zipFiles.containsKey(sanitizedPath) -> {
                val entry = zipFiles[sanitizedPath]!!

                when {
                    entry.isDirectory -> {
                        // Try to serve index.html from the directory
                        val indexPath = if (sanitizedPath.isEmpty()) "index.html" else "$sanitizedPath/index.html"
                        val indexEntry = zipFiles[indexPath]

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
                        httpFileStream(
                            stream = ByteArrayInputStream(entry.data),
                            contentLength = entry.data.size
                        )
                    }
                }
            }
            // Try exact match first, then try as directory
            zipFiles.keys.any { it.startsWith("$sanitizedPath/") } -> {
                // This is a directory path without trailing slash
                val indexPath = if (sanitizedPath.isEmpty()) "index.html" else "$sanitizedPath/index.html"
                val indexEntry = zipFiles[indexPath]

                if (indexEntry != null && !indexEntry.isDirectory) {
                    httpFileStream(
                        stream = ByteArrayInputStream(indexEntry.data),
                        contentLength = indexEntry.data.size
                    )
                } else {
                    httpForbidden("Directory listing not allowed")
                }
            }
            else -> httpNotFound(sanitizedPath, "File not found in zip archive")
        }
    }
}
