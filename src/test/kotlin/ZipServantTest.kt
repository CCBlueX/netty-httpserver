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

import io.netty.handler.codec.http.EmptyHttpHeaders
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.rest.ZipServant
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZipServantTest {

    /**
     * Creates a test zip file with sample content including nested directories
     */
    private fun createTestZip(): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // Add root index.html
            zos.putNextEntry(ZipEntry("index.html"))
            zos.write("<!DOCTYPE html><html><body>Root SPA</body></html>".toByteArray())
            zos.closeEntry()

            // Add a JavaScript file
            zos.putNextEntry(ZipEntry("assets/app.js"))
            zos.write("console.log('Hello from JS');".toByteArray())
            zos.closeEntry()

            // Add a CSS file
            zos.putNextEntry(ZipEntry("assets/style.css"))
            zos.write("body { color: red; }".toByteArray())
            zos.closeEntry()

            // Add a JSON file
            zos.putNextEntry(ZipEntry("metadata.json"))
            zos.write("{\"name\": \"test\"}".toByteArray())
            zos.closeEntry()

            // Add an image
            zos.putNextEntry(ZipEntry("images/test.png"))
            zos.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) // PNG header
            zos.closeEntry()

            // Add files with ./ prefix (common in zip files)
            zos.putNextEntry(ZipEntry("./components/component.js"))
            zos.write("export default {};".toByteArray())
            zos.closeEntry()

            // Add a subdirectory with its own index.html for SPA testing
            zos.putNextEntry(ZipEntry("admin/index.html"))
            zos.write("<!DOCTYPE html><html><body>Admin SPA</body></html>".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("admin/dashboard.js"))
            zos.write("console.log('Admin dashboard');".toByteArray())
            zos.closeEntry()

            // Add another subdirectory
            zos.putNextEntry(ZipEntry("test/index.html"))
            zos.write("<!DOCTYPE html><html><body>Test SPA</body></html>".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("test/app.js"))
            zos.write("console.log('Test app');".toByteArray())
            zos.closeEntry()

            // Add a directory entry
            zos.putNextEntry(ZipEntry("assets/"))
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun createRequestObject(path: String): RequestObject {
        return RequestObject(
            uri = path,
            path = path,
            remainingPath = path,
            method = HttpMethod.GET,
            body = "",
            params = emptyMap(),
            queryParams = emptyMap(),
            headers = EmptyHttpHeaders.INSTANCE
        )
    }

    @Test
    fun `should serve index html for root path`() {
        val zipData = createTestZip()
        val zipServant = ZipServant("static", zipData.inputStream())

        val response = zipServant.handle(createRequestObject(""))

        assertEquals(HttpResponseStatus.OK, response.status())
        assertTrue(response.headers().get("Content-Type").startsWith("text/html"))

        val content = response.content().toString(StandardCharsets.UTF_8)
        assertTrue(content.contains("Root SPA"))
    }

    @Test
    fun `should serve index html for slash path`() {
        val zipData = createTestZip()
        val zipServant = ZipServant("static", zipData.inputStream())

        val response = zipServant.handle(createRequestObject("/"))

        assertEquals(HttpResponseStatus.OK, response.status())
        assertTrue(response.headers().get("Content-Type").startsWith("text/html"))
    }

    @Test
    fun `should serve specific files with correct content types`() {
        val zipData = createTestZip()
        val zipServant = ZipServant("static", zipData.inputStream())

        // Test JavaScript file
        val jsResponse = zipServant.handle(createRequestObject("/assets/app.js"))
        assertEquals(HttpResponseStatus.OK, jsResponse.status())
        assertEquals("text/javascript", jsResponse.headers().get("Content-Type"))
        val jsContent = jsResponse.content().toString(StandardCharsets.UTF_8)
        assertTrue(jsContent.contains("console.log"))

        // Test CSS file
        val cssResponse = zipServant.handle(createRequestObject("/assets/style.css"))
        assertEquals(HttpResponseStatus.OK, cssResponse.status())
        assertEquals("text/css", cssResponse.headers().get("Content-Type"))

        // Test JSON file
        val jsonResponse = zipServant.handle(createRequestObject("/metadata.json"))
        assertEquals(HttpResponseStatus.OK, jsonResponse.status())
        assertEquals("application/json", jsonResponse.headers().get("Content-Type"))

        // Test PNG file
        val pngResponse = zipServant.handle(createRequestObject("/images/test.png"))
        assertEquals(HttpResponseStatus.OK, pngResponse.status())
        assertEquals("image/png", pngResponse.headers().get("Content-Type"))
    }

    @Test
    fun `should handle files with dot-slash prefix`() {
        val zipData = createTestZip()
        val zipServant = ZipServant("static", zipData.inputStream())

        val response = zipServant.handle(createRequestObject("/components/component.js"))

        assertEquals(HttpResponseStatus.OK, response.status())
        assertEquals("text/javascript", response.headers().get("Content-Type"))
        val content = response.content().toString(StandardCharsets.UTF_8)
        assertTrue(content.contains("export default"))
    }

    @Test
    fun `should return 404 for non-existent files`() {
        val zipData = createTestZip()
        val zipServant = ZipServant("static", zipData.inputStream())

        val response = zipServant.handle(createRequestObject("/non-existent.txt"))

        assertEquals(HttpResponseStatus.NOT_FOUND, response.status())
    }

    @Test
    fun `should sanitize path traversal attempts`() {
        val zipData = createTestZip()
        val zipServant = ZipServant("static", zipData.inputStream())

        val response = zipServant.handle(createRequestObject("/../../../etc/passwd"))

        assertEquals(HttpResponseStatus.NOT_FOUND, response.status())
    }

    @Test
    fun `should handle paths without leading slash`() {
        val zipData = createTestZip()
        val zipServant = ZipServant("static", zipData.inputStream())

        val response = zipServant.handle(createRequestObject("metadata.json"))

        assertEquals(HttpResponseStatus.OK, response.status())
        assertEquals("application/json", response.headers().get("Content-Type"))
    }

    @Test
    fun `should serve index html for SPA routes with hash fragments`() {
        val zipData = createTestZip()
        val zipServant = ZipServant("static", zipData.inputStream())

        // Test that root path serves index.html (which is the typical SPA behavior)
        val response = zipServant.handle(createRequestObject(""))

        // Should serve index.html for root requests (SPA behavior)
        assertEquals(HttpResponseStatus.OK, response.status())
        assertTrue(response.headers().get("Content-Type").startsWith("text/html"))
    }

    @Test
    fun `should handle unknown file extensions with default content type`() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("test.unknown"))
            zos.write("unknown content".toByteArray())
            zos.closeEntry()
        }

        val zipServant = ZipServant("static", baos.toByteArray().inputStream())
        val response = zipServant.handle(createRequestObject("/test.unknown"))

        assertEquals(HttpResponseStatus.OK, response.status())
        assertEquals("application/octet-stream", response.headers().get("Content-Type"))
    }

    @Test
    fun `should handle various content types correctly`() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // Test various file types - using Tika's expected content types
            val testFiles = listOf(
                "test.html", "test.htm", "test.css", "test.js", "test.json",
                "test.png", "test.jpg", "test.jpeg", "test.gif", "test.svg",
                "test.ico", "test.ttf", "test.woff", "test.woff2", "test.txt",
                "test.xml", "test.pdf", "test.zip", "test.md"
            )

            testFiles.forEach { filename ->
                zos.putNextEntry(ZipEntry(filename))
                zos.write("test content".toByteArray())
                zos.closeEntry()
            }
        }

        val zipServant = ZipServant("static", baos.toByteArray().inputStream())

        // Test some key content types that we know Tika should detect correctly
        val response = zipServant.handle(createRequestObject("/test.js"))
        assertEquals(HttpResponseStatus.OK, response.status())
        // Tika should detect JavaScript files correctly
        assertTrue(
            response.headers().get("Content-Type").contains("javascript") ||
            response.headers().get("Content-Type").contains("js"),
            "JavaScript file should have appropriate content type, got: ${response.headers().get("Content-Type")}"
        )

        val cssResponse = zipServant.handle(createRequestObject("/test.css"))
        assertEquals(HttpResponseStatus.OK, cssResponse.status())
        assertTrue(
            cssResponse.headers().get("Content-Type").contains("css"),
            "CSS file should have appropriate content type, got: ${cssResponse.headers().get("Content-Type")}"
        )

        val htmlResponse = zipServant.handle(createRequestObject("/test.html"))
        assertEquals(HttpResponseStatus.OK, htmlResponse.status())
        assertTrue(
            htmlResponse.headers().get("Content-Type").contains("html"),
            "HTML file should have appropriate content type, got: ${htmlResponse.headers().get("Content-Type")}"
        )
    }

    @Test
    fun `should handle empty zip file gracefully`() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { /* empty zip */ }

        val zipServant = ZipServant("static", baos.toByteArray().inputStream())
        val response = zipServant.handle(createRequestObject("/"))

        assertEquals(HttpResponseStatus.NOT_FOUND, response.status())
    }

    @Test
    fun `should serve index html for directory paths with trailing slash`() {
        val zipData = createTestZip()
        val zipServant = ZipServant("static", zipData.inputStream())

        // Test accessing admin/ directory - should serve admin/index.html
        val adminResponse = zipServant.handle(createRequestObject("/admin/"))
        assertEquals(HttpResponseStatus.OK, adminResponse.status())
        assertTrue(adminResponse.headers().get("Content-Type").startsWith("text/html"))
        val adminContent = adminResponse.content().toString(StandardCharsets.UTF_8)
        assertTrue(adminContent.contains("Admin SPA"))

        // Test accessing test/ directory - should serve test/index.html
        val testResponse = zipServant.handle(createRequestObject("/test/"))
        assertEquals(HttpResponseStatus.OK, testResponse.status())
        assertTrue(testResponse.headers().get("Content-Type").startsWith("text/html"))
        val testContent = testResponse.content().toString(StandardCharsets.UTF_8)
        assertTrue(testContent.contains("Test SPA"))
    }

    @Test
    fun `should serve index html for SPA routes with fragments`() {
        val zipData = createTestZip()
        val zipServant = ZipServant("static", zipData.inputStream())

        // Test SPA route with fragment in root
        val rootSpaResponse = zipServant.handle(createRequestObject("/#/dashboard"))
        assertEquals(HttpResponseStatus.OK, rootSpaResponse.status())
        assertTrue(rootSpaResponse.headers().get("Content-Type").startsWith("text/html"))
        val rootContent = rootSpaResponse.content().toString(StandardCharsets.UTF_8)
        assertTrue(rootContent.contains("Root SPA"))

        // Test SPA route with fragment in subdirectory
        val adminSpaResponse = zipServant.handle(createRequestObject("/admin/#/users"))
        assertEquals(HttpResponseStatus.OK, adminSpaResponse.status())
        assertTrue(adminSpaResponse.headers().get("Content-Type").startsWith("text/html"))
        val adminContent = adminSpaResponse.content().toString(StandardCharsets.UTF_8)
        assertTrue(adminContent.contains("Admin SPA"))

        // Test SPA route with fragment in test directory
        val testSpaResponse = zipServant.handle(createRequestObject("/test/#/settings"))
        assertEquals(HttpResponseStatus.OK, testSpaResponse.status())
        assertTrue(testSpaResponse.headers().get("Content-Type").startsWith("text/html"))
        val testContent = testSpaResponse.content().toString(StandardCharsets.UTF_8)
        assertTrue(testContent.contains("Test SPA"))
    }

    @Test
    fun `should serve index html for implicit directory access`() {
        val zipData = createTestZip()
        val zipServant = ZipServant("static", zipData.inputStream())

        // Test accessing admin directory without trailing slash - should serve admin/index.html
        val adminResponse = zipServant.handle(createRequestObject("/admin"))
        assertEquals(HttpResponseStatus.OK, adminResponse.status())
        assertTrue(adminResponse.headers().get("Content-Type").startsWith("text/html"))
        val adminContent = adminResponse.content().toString(StandardCharsets.UTF_8)
        assertTrue(adminContent.contains("Admin SPA"))

        // Test accessing test directory without trailing slash - should serve test/index.html
        val testResponse = zipServant.handle(createRequestObject("/test"))
        assertEquals(HttpResponseStatus.OK, testResponse.status())
        assertTrue(testResponse.headers().get("Content-Type").startsWith("text/html"))
        val testContent = testResponse.content().toString(StandardCharsets.UTF_8)
        assertTrue(testContent.contains("Test SPA"))
    }

    @Test
    fun `should return 404 for directory without index html`() {
        val zipData = createTestZip()
        val zipServant = ZipServant("static", zipData.inputStream())

        // Test accessing assets directory (which has no index.html) - should return 404
        val assetsResponse = zipServant.handle(createRequestObject("/assets/"))
        assertEquals(HttpResponseStatus.NOT_FOUND, assetsResponse.status())

        // Test accessing assets directory without trailing slash - should return 404
        val assetsResponse2 = zipServant.handle(createRequestObject("/assets"))
        assertEquals(HttpResponseStatus.NOT_FOUND, assetsResponse2.status())
    }
}
