import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import kotlinx.coroutines.runBlocking
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.middleware.Middleware
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpBadRequest
import net.ccbluex.netty.http.util.httpOk
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.Buffer
import org.junit.jupiter.api.*
import java.net.ProtocolException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for the HttpServer, focusing on verifying the routing capabilities
 * and correctness of responses from different endpoints.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpMiddlewareServerTest {

    private lateinit var server: HttpServer
    private val client = OkHttpClient()

    /**
     * This method sets up the necessary environment before any tests are run.
     * It creates a temporary directory with dummy files and starts the HTTP server
     * in a separate thread.
     */
    @BeforeAll
    fun initialize() {
        // Start the HTTP server in a separate thread
        server = startHttpServer()

        // Allow the server some time to start
        Thread.sleep(1000)
    }

    /**
     * This method cleans up resources after all tests have been executed.
     * It stops the server and deletes the temporary directory.
     */
    @AfterAll
    fun cleanup() = runBlocking {
        server.stop()
    }

    /**
     * This function starts the HTTP server with routing configured for
     * different difficulty levels.
     */
    private fun startHttpServer(): HttpServer = runBlocking {
        val server = HttpServer()

        server.routing {
            get("/", ::static)
        }

        server.middleware(Middleware.OnResponse { requestContext, fullHttpResponse ->
            // Add custom headers to the response
            fullHttpResponse.headers().add("X-Custom-Header", "Custom Value")

            // Add a custom header if there is a query parameter
            if (requestContext.params.isNotEmpty()) {
                fullHttpResponse.headers().add("X-Query-Param",
                    requestContext.params.entries.joinToString(","))
            }

            fullHttpResponse
        }).middleware(Middleware.OnWebSocketUpgrade { context, _ ->
            httpBadRequest("WebSocket unsupported")
        })

        server.start(8080)  // Start the server on port 8080
        server
    }

    @Suppress("UNUSED_PARAMETER")
    fun static(requestObject: RequestObject): FullHttpResponse {
        return httpOk(JsonObject().apply {
            addProperty("message", "Hello, World!")
        })
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
     * Test the root endpoint ("/") with a query parameter and verify that the
     * custom header is added to the response.
     */
    @Test
    fun testRootEndpointWithQueryParam() {
        val response = makeRequest("/?param1=value1&param2=value2")
        assertEquals(200, response.code, "Expected status code 200")

        val responseBody = response.body?.string()
        assertNotNull(responseBody, "Response body should not be null")

        assertTrue(responseBody.contains("Hello, World!"), "Response should contain 'Hello, World!'")
        assertTrue(response.headers("X-Custom-Header").contains("Custom Value"), "Custom header should be present")
        assertTrue(response.headers("X-Query-Param").contains("param1=value1,param2=value2"),
            "Query parameter should be present in the response")
    }

    @Test
    fun testWebSocketShouldBeBadRequest() {
        val future = CompletableFuture<Boolean>()

        client.newWebSocket(
            Request.Builder()
                .url("http://localhost:8080")
                .build(),
            object : WebSocketListener() {
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    assertIs<ProtocolException>(t)
                    assertNotNull(response)
                    assertEquals(HttpResponseStatus.BAD_REQUEST.code(), response.code)
                    val exceptedResponseBody = httpBadRequest("WebSocket unsupported")
                    val buffer = Buffer()
                    buffer.write(exceptedResponseBody.content().nioBuffer())
                    assertEquals(
                        buffer.readUtf8(),
                        response.body!!.string()
                    )
                    future.complete(true)
                }
            }
        )

        assertTrue(future.get(10, TimeUnit.SECONDS))
    }

}
