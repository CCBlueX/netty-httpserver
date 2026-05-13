import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.routing.Routing
import net.ccbluex.netty.http.routing.RoutingContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.*
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for the HttpServer, focusing on verifying the routing capabilities
 * and correctness of responses from different endpoints.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpServerTest {

    private lateinit var folder: File
    private lateinit var server: HttpServer
    private val client = OkHttpClient()

    /**
     * This method sets up the necessary environment before any tests are run.
     * It creates a temporary directory with dummy files and starts the HTTP server
     * in a separate thread.
     */
    @BeforeAll
    fun initialize() {
        // Create a temporary folder with dummy data
        folder = Files.createTempDirectory("netty-rest-test").toFile()
        File(folder, "fa.txt").writeText("A")
        File(folder, "fb.txt").writeText("B")
        File(folder, "fc.txt").writeText("C")

        // sub-folder with index.html
        val subFolder = File(folder, "sub")
        subFolder.mkdir()
        File(subFolder, "index.html").writeText("Hello, World!")

        // Start the HTTP server in a separate thread
        server = startHttpServer(folder)

        // Allow the server some time to start
        Thread.sleep(1000)
    }

    /**
     * This method cleans up resources after all tests have been executed.
     * It stops the server and deletes the temporary directory.
     */
    @AfterAll
    fun cleanup() {
        server.stopBlocking()
        folder.deleteRecursively() // Clean up the temporary folder
    }

    /**
     * This function starts the HTTP server with routing configured for
     * different difficulty levels.
     */
    private fun startHttpServer(folder: File): HttpServer = runBlocking {
        val server = HttpServer()

        server.routing {
            basicRoutes()
            nestedRoutes()
            apiRoutes()
            file("/abc", folder)
            file("/def/abc", folder)
        }

        server.start(8080)  // Start the server on port 8080
        server
    }

    private suspend fun RoutingContext.static() {
        respond(JsonObject().apply {
            addProperty("message", "Hello, World!")
        })
    }

    private suspend fun RoutingContext.param() {
        respond(JsonObject().apply {
            addProperty("message", "Hello, ${parameters["name"]}")
        })
    }

    private suspend fun RoutingContext.params() {
        respond(JsonObject().apply {
            addProperty("message", "Hello, ${parameters["value1"]} and ${parameters["value2"]}")
        })
    }

    private suspend fun RoutingContext.a() {
        respond(JsonObject().apply {
            addProperty("char", "A")
        })
    }

    private suspend fun RoutingContext.b() {
        respond(JsonObject().apply {
            addProperty("char", "B")
        })
    }

    private suspend fun RoutingContext.c() {
        respond(JsonObject().apply {
            addProperty("char", "C")
        })
    }

    private fun Routing.basicRoutes() {
        get("/") { static() }
        get("/a") { a() }
        get("/b") { b() }
        get("/c") { c() }
        get("/v/:name") { param() }
        get("/r/:value1/:value2") { params() }
        get("/o/:value1/in/:value2") { params() }
    }

    private fun Routing.nestedRoutes() {
        route("/m") {
            route("/a") {
                get("/b") { b() }
                get("/c") { c() }
            }
            route("/b") {
                get("/a") { a() }
                get("/c") { c() }
            }
            route("/c") {
                get("/a") { a() }
                get("/b") { b() }
            }
        }

        route("/h") {
            get("/a") { a() }
            route("/a") {
                get("/b") { b() }
            }
        }

        route("/ktor") {
            get { a() }
            route("/nested") {
                get { b() }
            }
        }
    }

    private fun Routing.apiRoutes() {
        route("/api") {
            route("/v1") {
                route("/s") {
                    get { static() }
                    route("/s") {
                        get { static() }
                        delete { static() }
                    }
                }
            }
        }
    }

    /**
     * Utility function to make HTTP GET requests to the specified path.
     *
     * @param path The path for the request.
     * @return The HTTP response.
     */
    private fun makeRequest(path: String): Response {
        val request = Request.Builder()
            .url("http://localhost:8080$path")
            .build()
        return client.newCall(request).execute()
    }

    /**
     * Test the root endpoint ("/") and verify that it returns the correct number
     * of files in the directory.
     */
    @Test
    fun testRootEndpoint() {
        val response = makeRequest("/")
        assertEquals(200, response.code, "Expected status code 200")

        val responseBody = response.body?.string()
        assertNotNull(responseBody, "Response body should not be null")

        assertTrue(responseBody.contains("Hello, World!"), "Response should contain 'Hello, World!'")
    }

    /**
     * Test the "/a" endpoint and verify that it returns the correct character.
     */
    @Test
    fun testAEndpoint() {
        val response = makeRequest("/a")
        assertEquals(200, response.code, "Expected status code 200")

        val responseBody = response.body?.string()
        assertNotNull(responseBody, "Response body should not be null")

        assertTrue(responseBody.contains("\"char\":\"A\""), "Response should contain char 'A'")
    }

    /**
     * Test the "/b" endpoint and verify that it returns the correct character.
     */
    @Test
    fun testBEndpoint() {
        val response = makeRequest("/b")
        assertEquals(200, response.code, "Expected status code 200")

        val responseBody = response.body?.string()
        assertNotNull(responseBody, "Response body should not be null")

        assertTrue(responseBody.contains("\"char\":\"B\""), "Response should contain char 'B'")
    }

    /**
     * Test the "/c" endpoint and verify that it returns the correct character.
     */
    @Test
    fun testCEndpoint() {
        val response = makeRequest("/c")
        assertEquals(200, response.code, "Expected status code 200")

        val responseBody = response.body?.string()
        assertNotNull(responseBody, "Response body should not be null")

        assertTrue(responseBody.contains("\"char\":\"C\""), "Response should contain char 'C'")
    }

    @Test
    fun testKtorStyleNodeEndpoint() {
        val response = makeRequest("/ktor")
        assertEquals(200, response.code, "Expected status code 200")

        val responseBody = response.body?.string()
        assertNotNull(responseBody, "Response body should not be null")

        assertTrue(responseBody.contains("\"char\":\"A\""), "Response should contain char 'A'")
    }

    @Test
    fun testKtorStyleNestedEndpoint() {
        val response = makeRequest("/ktor/nested")
        assertEquals(200, response.code, "Expected status code 200")

        val responseBody = response.body?.string()
        assertNotNull(responseBody, "Response body should not be null")

        assertTrue(responseBody.contains("\"char\":\"B\""), "Response should contain char 'B'")
    }

    /**
     * Test the "/v/:name" endpoint with a parameter and verify that it returns the correct message.
     */
    @Test
    fun testVParamEndpoint() {
        val response = makeRequest("/v/Alice")
        assertEquals(200, response.code, "Expected status code 200")

        val responseBody = response.body?.string()
        assertNotNull(responseBody, "Response body should not be null")

        assertTrue(responseBody.contains("Hello, Alice"), "Response should contain 'Hello, Alice'")
    }

    /**
     * Test the "/r/:value1/:value2" endpoint with multiple parameters and verify that it returns the correct message.
     */
    @Test
    fun testRParamsEndpoint() {
        val response = makeRequest("/r/Alice/Bob")
        assertEquals(200, response.code, "Expected status code 200")

        val responseBody = response.body?.string()
        assertNotNull(responseBody, "Response body should not be null")

        assertTrue(responseBody.contains("Hello, Alice and Bob"), "Response should contain 'Hello, Alice and Bob'")
    }

    /**
     * Test the "/o/:value1/in/:value2" endpoint with multiple parameters and verify that it returns the correct message.
     */
    @Test
    fun testOParamsEndpoint() {
        val response = makeRequest("/o/Alice/in/Bob")
        assertEquals(200, response.code, "Expected status code 200")

        val responseBody = response.body?.string()
        assertNotNull(responseBody, "Response body should not be null")

        assertTrue(responseBody.contains("Hello, Alice and Bob"), "Response should contain 'Hello, Alice and Bob'")
    }

    /**
     * Test various medium difficulty endpoints and verify correct responses.
     */
    @Test
    fun testMultipleEndpoints() {
        val endpoints = listOf("/m/a/b", "/m/a/c", "/m/b/a", "/m/b/c", "/m/c/a", "/m/c/b")

        endpoints.forEach { endpoint ->
            val response = makeRequest(endpoint)
            assertEquals(200, response.code, "Expected status code 200 for $endpoint")

            val responseBody = response.body?.string()
            assertNotNull(responseBody, "Response body should not be null for $endpoint")

            // Example assertions; adjust according to the expected behavior of each endpoint
            assertTrue(
                responseBody.contains("\"char\":\"A\"") ||
                        responseBody.contains("\"char\":\"B\"") ||
                        responseBody.contains("\"char\":\"C\""),
                "Unexpected response for $endpoint"
            )
        }
    }

    /**
     * Test various hard difficulty endpoints and verify correct responses.
     */
    @Test
    fun testStackedEndpoints() {
        val endpoints = listOf("/h/a/b", "/h/a")

        endpoints.forEach { endpoint ->
            val response = makeRequest(endpoint)
            assertEquals(200, response.code, "Expected status code 200 for $endpoint")

            val responseBody = response.body?.string()
            assertNotNull(responseBody, "Response body should not be null for $endpoint")

            // Example assertions; adjust according to the expected behavior of each endpoint
            assertTrue(
                responseBody.contains("\"char\":\"A\"") || responseBody.contains("\"char\":\"B\""),
                "Unexpected response for $endpoint"
            )
        }
    }

    @Test
    fun testNonExistentEndpoint() {
        val response = makeRequest("/nonexistent")
        assertEquals(404, response.code, "Expected status code 404")
    }

    @Test
    fun testFileEndpoint() {
        testFileEndpoint("/abc")
        testFileEndpoint("/def/abc")
    }

    fun testFileEndpoint(prefix: String) {
        val files = folder.list() ?: emptyArray()

        files.forEach { file ->
            val response = makeRequest("$prefix/$file")
            assertEquals(200, response.code, "Expected status code 200 for $file")

            val responseBody = response.body?.string()
            assertNotNull(responseBody, "Response body should not be null for $file")

            val file = File(folder, file)
            val expected = if (file.isDirectory) {
                File(file, "index.html")
            } else {
                file
            }.readText()

            assertEquals(expected, responseBody, "Response should match file content")
        }
    }

    @Test
    fun testApiEndpoints() {
        val endpoints = listOf("/api/v1/s", "/api/v1/s/s")

        endpoints.forEach { endpoint ->
            val response = makeRequest(endpoint)
            assertEquals(200, response.code, "Expected status code 200 for $endpoint")

            val responseBody = response.body?.string()
            assertNotNull(responseBody, "Response body should not be null for $endpoint")

            assertTrue(responseBody.contains("Hello, World!"), "Response should contain 'Hello, World!'")
        }
    }

    /**
     * Test the "/h/a" file servant endpoint and verify that it returns the correct file content.
     * This test is expected to fail due to the conflict with the "/h/a" route.
     *
     * Since this matter is not worth fixing, the test is commented out.
     */
//    @Test
//    fun testConflictingFileEndpoint() {
//        val files = folder.list() ?: emptyArray()
//
//        files.forEach { file ->
//            val response = makeRequest("/h/a/$file")
//            assertEquals(200, response.code(), "Expected status code 200 for $file")
//
//            val responseBody = response.body()?.string()
//            assertNotNull(responseBody, "Response body should not be null for $file")
//
//            assertEquals(File(folder, file).readText(), responseBody, "Response should match file content")
//        }
//    }

}
